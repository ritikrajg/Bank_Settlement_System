package com.iispl.threading;

import java.util.List;
import java.util.concurrent.BlockingQueue;

import com.iispl.adapter.AdapterException;
import com.iispl.adapter.TransactionAdapter;
import com.iispl.config.DBConnection;
import com.iispl.dao.TransactionDao;
import com.iispl.entity.IncomingTransaction;
import com.iispl.enums.TransactionStatus;

/**
 * Producer worker that parses raw payloads for a single channel
 * (CBS, RTGS, NEFT, UPI, or SWIFT) and pushes the results onto
 * the shared queue for the settlement phase.
 *
 * What happens for each payload line/message:
 * 1. Parse the raw data using the adapter
 * 2. Save the transaction to the database (status = QUEUED)
 * 3. Put the transaction on the queue for settlement
 *
 * If parsing fails, the transaction is counted as "rejected" and
 * processing continues with the next one.
 */
public class IngestionWorker implements Runnable {

    private final TransactionAdapter adapter;
    private final List<String> rawPayloads;
    private final BlockingQueue<IncomingTransaction> queue;
    private final TransactionDao txnDAO;

    // Counters — updated during run()
    private int acceptedCount;
    private int rejectedCount;

    public int getAcceptedCount() {
        return acceptedCount;
    }

    public int getRejectedCount() {
        return rejectedCount;
    }

    public IngestionWorker(TransactionAdapter adapter,
            List<String> rawPayloads,
            BlockingQueue<IncomingTransaction> queue,
            TransactionDao txnDAO) {
        this.adapter = adapter;
        this.rawPayloads = rawPayloads;
        this.queue = queue;
        this.txnDAO = txnDAO;
    }

    @Override
    public void run() {
        try {
            for (String rawPayload : rawPayloads) {
                try {
                    IncomingTransaction txn = adapter.adapt(rawPayload);

                    txn.setStatus(TransactionStatus.QUEUED);
                    boolean inserted = txnDAO.insert(txn);

                    if (!inserted) {
                        DBConnection.rollback();
                        rejectedCount++;
                        System.err.println("[WARN] Duplicate transaction skipped: txn_id="
                                + txn.getTxnId() + ", source=" + adapter.getSourceType());
                        continue;
                    }

                    txnDAO.updateStatus(txn.getTxnId(), TransactionStatus.QUEUED);
                    DBConnection.commit();

                    queue.put(txn);
                    acceptedCount++;

                } catch (InterruptedException e) {
                    // Thread was interrupted — stop cleanly
                    Thread.currentThread().interrupt();
                    DBConnection.rollback();
                    System.err.println("[WARN] " + adapter.getSourceType()
                            + " ingestion interrupted; stopping worker.");
                    break;

                } catch (AdapterException e) {
                    // Bad data — skip this payload and continue
                    rejectedCount++;
                    System.err.println("[ERROR] Invalid payload: " + rawPayload);

                } catch (Exception e) {
                    // Unexpected error — rollback and continue
                    DBConnection.rollback();
                    rejectedCount++;
                    e.printStackTrace();
                }
            }
        } finally {
            DBConnection.close();
        }

        System.out.printf("  [%-5s] accepted=%-4d  rejected=%d%n",
                adapter.getSourceType(), acceptedCount, rejectedCount);
    }
}
