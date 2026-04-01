package com.iispl.threading;

import java.util.List;
import java.util.concurrent.BlockingQueue;

import com.iispl.adapter.AdapterException;
import com.iispl.adapter.TransactionAdapter;
import com.iispl.config.DBConnection;
import com.iispl.dao.TransactionDao;
import com.iispl.entity.IncomingTransaction;
import com.iispl.enums.TransactionStatus;

public class IngestionWorker implements Runnable {
    private final TransactionAdapter adapter;
    private final List<String> rawPayloads;
    private final BlockingQueue<IncomingTransaction> queue;
    private final TransactionDao txnDAO;

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
        String threadName = Thread.currentThread().getName();
        System.out.printf("  [%s] Ingestion | %s | %d payloads%n",
                threadName, adapter.getSourceType(), rawPayloads.size());

        try {
            for (String rawPayload : rawPayloads) {
                try {
                    IncomingTransaction txn = adapter.adapt(rawPayload);
                    txnDAO.insert(txn);
                    queue.put(txn);
                    txn.setStatus(TransactionStatus.QUEUED);
                    txnDAO.updateStatus(txn.getTxnId(), TransactionStatus.QUEUED);
                    DBConnection.commit();

                    System.out.printf("    ACCEPTED %s%n", txn.getSourceRef());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    DBConnection.rollback();
                    System.err.printf("    INTERRUPTED %s%n", adapter.getSourceType());
                    break;
                } catch (AdapterException e) {
                    System.err.printf("    REJECTED  %s | %s%n",
                            adapter.getSourceType(), e.getMessage());
                } catch (Exception e) {
                    DBConnection.rollback();
                    System.err.printf("    ERROR     %s | %s%n",
                            adapter.getSourceType(), e.getMessage());
                    e.printStackTrace(System.err);
                    if (isFatalDatabaseError(e)) {
                        break;
                    }
                }
            }
        } finally {
            DBConnection.close();
        }

        System.out.printf("  [%s] Ingestion complete | %s%n",
                threadName, adapter.getSourceType());
    }

    private boolean isFatalDatabaseError(Exception exception) {
        return exception instanceof java.sql.SQLException;
    }
}