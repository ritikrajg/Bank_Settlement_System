package com.iispl.services;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

import com.iispl.config.DBConnection;
import com.iispl.dao.AccountDao;
import com.iispl.dao.CustomerDao;
import com.iispl.dao.SettlementBatchDAO;
import com.iispl.dao.SettlementRecordDAO;
import com.iispl.dao.TransactionDao;
import com.iispl.entity.IncomingTransaction;
import com.iispl.entity.SettlementBatch;
import com.iispl.entity.SettlementResult;
import com.iispl.enums.BatchStatus;
import com.iispl.threading.SettlementProcessor;

/**
 * Phase 2 of the settlement pipeline: Settlement.
 *
 * Spawns {@code consumerCount} {@link SettlementProcessor} workers that drain
 * the shared ingestion queue concurrently. Each worker validates accounts and
 * KYC status, debits / credits balances, and writes settlement records.
 *
 * All workers run via the supplied {@link ExecutorService}. Their individual
 * {@link Future} objects can be started first and then waited on later.
 */
public class SettlementPhase {

    private final SettlementBatchDAO  batchDAO;
    private final AccountDao          accountDao;
    private final SettlementRecordDAO recordDAO;
    private final TransactionDao      txnDao;
    private final CustomerDao         customerDao;
    private final ExecutorService     executor;

    public SettlementPhase(SettlementBatchDAO batchDAO,
            AccountDao accountDao,
            SettlementRecordDAO recordDAO,
            TransactionDao txnDao,
            CustomerDao customerDao,
            ExecutorService executor) {
        this.batchDAO   = batchDAO;
        this.accountDao = accountDao;
        this.recordDAO  = recordDAO;
        this.txnDao     = txnDao;
        this.customerDao = customerDao;
        this.executor   = executor;
    }

    /**
     * Starts {@code consumerCount} settlement workers and returns their
     * {@link Future} handles.
     *
     * @param queue              shared queue populated by the ingestion phase
     * @param ingestionCompleted flag set by the orchestrator when ingestion ends
     * @param consumerCount      number of parallel settlement workers to spawn
     * @param batchesByType      map of source-type name → active SettlementBatch
     * @param settlementDate     date being settled (used for any date-stamped records)
     * @param runBy              operator / scheduler identifier
     * @return one {@link Future} per worker
     */
    public List<Future<SettlementResult>> startSettlementPhase(
            BlockingQueue<IncomingTransaction> queue,
            AtomicBoolean ingestionCompleted,
            int consumerCount,
            Map<String, SettlementBatch> batchesByType,
            LocalDate settlementDate,
            String runBy) throws Exception {

        System.out.println();
        System.out.println("Phase 2 : Settlement");
        System.out.println("--------------------");

        List<Future<SettlementResult>> futures = new ArrayList<>();

        // Spawn one SettlementProcessor worker per consumer slot
        for (int i = 0; i < consumerCount; i++) {
            SettlementProcessor processor = new SettlementProcessor(
                    batchesByType, batchDAO, recordDAO, txnDao, accountDao, customerDao);

            Callable<SettlementResult> task = new Callable<SettlementResult>() {
                @Override
                public SettlementResult call() {
                    try {
                        return processor.consumeQueue(queue, ingestionCompleted);
                    } catch (Exception e) {
                        System.err.println("[ERROR] Settlement worker failed: " + e.getMessage());

                        SettlementResult err = new SettlementResult();
                        err.incrementFailed();
                        err.addFailureReason("Processor error: " + e.getMessage());
                        err.finalise();
                        return err;
                    }
                }
            };

            Future<SettlementResult> future = executor.submit(task);

            futures.add(future);
        }

        return futures;
    }

    /**
     * Waits for the supplied settlement workers to finish and then updates
     * batch status and prints the summary.
     */
    public List<SettlementResult> collectSettlementResults(
            List<Future<SettlementResult>> futures,
            Map<String, SettlementBatch> batchesByType) throws Exception {

        List<SettlementResult> results = new ArrayList<>();
        for (Future<SettlementResult> future : futures) {
            results.add(future.get());
        }

        // Update each batch's final status in the database
        for (SettlementBatch batch : batchesByType.values()) {
            try {
                updateBatchOutcome(batch);
            } catch (SQLException e) {
                throw new RuntimeException("Failed to update batch outcome for "
                        + batch.getBatchId(), e);
            }
        }

        printSummary(results);
        return results;
    }

    /**
     * Prints a human-readable summary of the settlement run to stdout.
     */
    private void printSummary(List<SettlementResult> results) {
        int        settled = 0;
        int        failed  = 0;
        BigDecimal amount  = BigDecimal.ZERO;

        for (SettlementResult result : results) {
            settled += result.getSettledCount();
            failed  += result.getFailedCount();
            amount   = amount.add(result.getTotalSettledAmount());
        }

        System.out.println();
        System.out.println("╔══════════════════════════════════════╗");
        System.out.println("║          Settlement Summary          ║");
        System.out.println("╠══════════════════════════════════════╣");
        System.out.printf( "║  Transactions settled : %-13d║%n", settled);
        System.out.printf( "║  Transactions failed  : %-13d║%n", failed);
        System.out.printf( "║  Total amount settled : %-13s║%n", amount.toPlainString());
        System.out.println("╚══════════════════════════════════════╝");
    }

    /**
     * Prints a concise summary for a single completed batch, showing the key
     * fields selected by the operator: ID, source, date, settled count,
     * total amount, and final status.
     */
    private void printBatchSummary(SettlementBatch batch) {
        System.out.println();
        System.out.println("  Batch Summary");
        System.out.println("  ┌─────────────────────────────────────────┐");
        System.out.printf( "  │  Batch ID       : %-22s│%n", batch.getBatchId());
        System.out.printf( "  │  Source type    : %-22s│%n", batch.getBatchType());
        System.out.printf( "  │  Settlement date: %-22s│%n", batch.getBatchDate());
        System.out.printf( "  │  Settled count  : %-22d│%n", batch.getTotalTransactions());
        System.out.printf( "  │  Amount settled : %-22s│%n", batch.getTotalAmount().toPlainString());
        System.out.printf( "  │  Status         : %-22s│%n", batch.getBatchStatus());
        System.out.println("  └─────────────────────────────────────────┘");
    }

    /**
     * Updates the batch status in the database once all its transactions have
     * been processed. Batches with zero transactions are deleted (they were
     * created speculatively before payloads were parsed).
     *
     * @param batch the batch whose outcome is to be recorded
     */
    private void updateBatchOutcome(SettlementBatch batch) throws SQLException {
        if (batch.getTotalTransactions() == 0) {
            // No transactions were processed — remove the empty batch record
            try {
                batchDAO.delete(batch.getBatchId());
                DBConnection.commit();
            } catch (SQLException e) {
                DBConnection.rollback();
                throw e;
            } finally {
                DBConnection.close();
            }
        } else {
            // Mark the batch as fully completed
            batch.setBatchStatus(BatchStatus.COMPLETED);

            // Print a per-batch summary before persisting the final status
            printBatchSummary(batch);

            try {
                batchDAO.update(batch);
                DBConnection.commit();
            } catch (SQLException e) {
                DBConnection.rollback();
                throw e;
            } finally {
                DBConnection.close();
            }
        }
    }
}