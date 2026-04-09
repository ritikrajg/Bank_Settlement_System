package com.iispl.threading;

import java.sql.SQLException;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import com.iispl.config.DBConnection;
import com.iispl.dao.AccountDao;
import com.iispl.dao.CustomerDao;
import com.iispl.dao.SettlementBatchDAO;
import com.iispl.dao.SettlementRecordDAO;
import com.iispl.dao.TransactionDao;
import com.iispl.entity.Account;
import com.iispl.entity.Customer;
import com.iispl.entity.IncomingTransaction;
import com.iispl.entity.SettlementBatch;
import com.iispl.entity.SettlementRecord;
import com.iispl.entity.SettlementResult;
import com.iispl.enums.AccountStatus;
import com.iispl.enums.KycStatus;
import com.iispl.enums.SettlementStatus;
import com.iispl.enums.TransactionStatus;

/**
 * Consumer worker that settles transactions from the shared queue.
 *
 * Settlement steps for each transaction:
 *  1. Look up sender and receiver accounts
 *  2. Check both accounts are ACTIVE
 *  3. Check both customers have VERIFIED KYC
 *  4. Debit sender, credit receiver
 *  5. Save settlement record and update transaction status
 *
 * If any step fails, the transaction is marked FAILED and we
 * continue with the next one.
 */
public class SettlementProcessor {

    /** How long to wait when queue is empty (seconds) */
    private static final int POLL_TIMEOUT_SECS = 1;

    private final Map<String, SettlementBatch> batchesByType;
    private final SettlementBatchDAO           batchDAO;
    private final SettlementRecordDAO          recordDAO;
    private final TransactionDao               txnDAO;
    private final AccountDao                   accountDAO;
    private final CustomerDao                  customerDao;

    public SettlementProcessor(Map<String, SettlementBatch> batchesByType,
            SettlementBatchDAO batchDAO,
            SettlementRecordDAO recordDAO,
            TransactionDao txnDAO,
            AccountDao accountDAO,
            CustomerDao customerDao) {
        this.batchesByType = batchesByType;
        this.batchDAO      = batchDAO;
        this.recordDAO     = recordDAO;
        this.txnDAO        = txnDAO;
        this.accountDAO    = accountDAO;
        this.customerDao   = customerDao;
    }

    /**
     * Keeps taking transactions from the queue until:
     *  - Ingestion is done AND
     *  - The queue is empty
     *
     * Returns the result with counts of settled and failed transactions.
     */
    public SettlementResult consumeQueue(BlockingQueue<IncomingTransaction> queue,
            AtomicBoolean ingestionCompleted) throws InterruptedException {

        SettlementResult result = new SettlementResult();

        try {
            while (!ingestionCompleted.get() || !queue.isEmpty()) {

                // Wait up to 1 second for a transaction from the queue
                IncomingTransaction txn = queue.poll(POLL_TIMEOUT_SECS, TimeUnit.SECONDS);

                if (txn == null) {
                    // Queue was temporarily empty — try again
                    continue;
                }

                try {
                    settle(txn, result);
                    DBConnection.commit();
                } catch (Exception e) {
                    DBConnection.rollback();
                    markFailed(txn, getBatchFor(txn), e.getMessage(), result);
                }
            }

            result.finalise();

        } finally {
            DBConnection.close();
        }

        return result;
    }

    // ─── Private Helper Methods ───────────────────────────────────────

    /**
     * Validates and settles a single transaction.
     */
    private void settle(IncomingTransaction txn, SettlementResult result)
            throws SQLException {

        SettlementBatch batch = getBatchFor(txn);

        // Step 1: Look up sender and receiver accounts
        Optional<Account> fromOpt = accountDAO.findByAccountNumber(txn.getFromAccount());
        Optional<Account> toOpt   = accountDAO.findByAccountNumber(txn.getToAccount());

        if (fromOpt.isEmpty() || toOpt.isEmpty()) {
            failTransaction(txn, batch, result,
                    "Account not found for transaction " + txn.getTxnId());
            return;
        }

        Account fromAccount = fromOpt.get();
        Account toAccount   = toOpt.get();

        // Step 2: Check both accounts are ACTIVE
        if (fromAccount.getStatus() != AccountStatus.ACTIVE) {
            failTransaction(txn, batch, result,
                    "Sender account is not active for transaction " + txn.getTxnId());
            return;
        }
        if (toAccount.getStatus() != AccountStatus.ACTIVE) {
            failTransaction(txn, batch, result,
                    "Receiver account is not active for transaction " + txn.getTxnId());
            return;
        }

        // Step 3: Check KYC status for both customers
        Optional<Customer> sender   = customerDao.findById(fromAccount.getCustomerId());
        Optional<Customer> receiver = customerDao.findById(toAccount.getCustomerId());

        if (sender.isEmpty() || receiver.isEmpty()) {
            failTransaction(txn, batch, result,
                    "Customer not found for transaction " + txn.getTxnId());
            return;
        }
        if (sender.get().getKycStatus() != KycStatus.VERIFIED) {
            failTransaction(txn, batch, result,
                    "Sender KYC not verified for transaction " + txn.getTxnId());
            return;
        }
        if (receiver.get().getKycStatus() != KycStatus.VERIFIED) {
            failTransaction(txn, batch, result,
                    "Receiver KYC not verified for transaction " + txn.getTxnId());
            return;
        }

        // Step 4: Move funds — debit sender, credit receiver
        fromAccount.debit(txn.getAmount());
        toAccount.credit(txn.getAmount());
        //accountDAO.updateBalance(fromAccount);
        //accountDAO.updateBalance(toAccount);

        // Step 5: Save settlement record and update status
        SettlementRecord record = new SettlementRecord(
                batch.getBatchId(),
                txn.getTxnId(),
                txn.getAmount(),
                txn.getSourceBank(),
                txn.getDestinationBank(),
                SettlementStatus.SETTLED);
        recordDAO.insert(record);
        txnDAO.updateStatus(txn.getTxnId(), TransactionStatus.SETTLED);

        // Update counts
        batch.recordSettlement(txn.getAmount());
        result.incrementSettled(txn.getAmount());
    }

    /**
     * Records a failed transaction in the database.
     */
    private void failTransaction(IncomingTransaction txn,
            SettlementBatch batch,
            SettlementResult result,
            String reason) throws SQLException {

        SettlementRecord record = new SettlementRecord(
                null,
                txn.getTxnId(),
                txn.getAmount(),
                txn.getSourceBank(),
                txn.getDestinationBank(),
                SettlementStatus.FAILED);
        record.setFailureReason(reason);
        recordDAO.insert(record);
        txnDAO.updateStatus(txn.getTxnId(), TransactionStatus.FAILED);

        batch.recordFailure();
        result.incrementFailed();
        result.addFailureReason(reason);
    }

    /**
     * Tries to record a failure after the main transaction already rolled back.
     */
    private void markFailed(IncomingTransaction txn,
            SettlementBatch batch,
            String reason,
            SettlementResult result) {
        try {
            failTransaction(txn, batch, result, reason);
            DBConnection.commit();
        } catch (SQLException ex) {
            DBConnection.rollback();
            System.err.println("[ERROR] Could not persist failure record for txn "
                    + txn.getTxnId() + ": " + ex.getMessage());
        }
    }

    /**
     * Finds the batch that matches this transaction's source system.
     */
    private SettlementBatch getBatchFor(IncomingTransaction txn) {
        SettlementBatch batch = batchesByType.get(txn.getSourceSystem().name());
        if (batch == null) {
            throw new IllegalStateException(
                    "No batch found for source: " + txn.getSourceSystem());
        }
        return batch;
    }
}