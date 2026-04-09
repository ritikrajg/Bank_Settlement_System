package com.iispl.services;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.iispl.adapter.AdapterRegistry;
import com.iispl.adapter.TransactionAdapter;
import com.iispl.dao.TransactionDao;
import com.iispl.entity.IncomingTransaction;
import com.iispl.enums.SourceType;
import com.iispl.threading.IngestionWorker;

/**
 * Phase 1 of the settlement pipeline: Ingestion.
 *
 * What this class does:
 *  - Takes the raw payload data from all 5 sources
 *  - Creates one IngestionWorker per source type
 *  - Each worker runs in parallel on the thread pool
 *  - Workers parse the raw data and put transactions onto a shared queue
 */
public class IngestionPhase {

    /** Maximum time (seconds) to wait for one source's ingestion to finish */
    private static final int INGESTION_TIMEOUT_SECS = 60;

    private final AdapterRegistry adapterRegistry;
    private final ExecutorService executor;
    private final BlockingQueue<IncomingTransaction> queue;
    private final TransactionDao txnDAO;

    public IngestionPhase(AdapterRegistry adapterRegistry,
            ExecutorService executor,
            BlockingQueue<IncomingTransaction> pipelineQueue,
            TransactionDao txnDAO) {
        this.adapterRegistry = adapterRegistry;
        this.executor        = executor;
        this.queue           = pipelineQueue;
        this.txnDAO          = txnDAO;
    }

    /**
     * Starts one ingestion worker per source type.
     * Waits for all workers to finish before returning.
     */
    public void runIngestion(Map<SourceType, List<String>> payloads)
            throws InterruptedException {

        System.out.println();
        System.out.println("Phase 1 : Ingestion");
        System.out.println("-------------------");

        List<Future<?>> futures = new ArrayList<>();

        // Submit one worker per source type
        for (Map.Entry<SourceType, List<String>> entry : payloads.entrySet()) {
            SourceType type = entry.getKey();

            // Check if we have an adapter for this source
            if (!adapterRegistry.hasAdapter(type)) {
                System.err.println("  [SKIP] No adapter registered for source: " + type);
                continue;
            }

            try {
                TransactionAdapter adapter = adapterRegistry.getAdapter(type);
                IngestionWorker worker = new IngestionWorker(adapter, entry.getValue(), queue, txnDAO);
                futures.add(executor.submit(worker));
            } catch (Exception e) {
                System.err.println("  [ERROR] Could not start ingestion for " + type + ": " + e.getMessage());
            }
        }

        // Wait for every worker to finish
        for (Future<?> future : futures) {
            try {
                future.get(INGESTION_TIMEOUT_SECS, TimeUnit.SECONDS);
            } catch (TimeoutException e) {
                future.cancel(true);
                System.err.println("  [TIMEOUT] Ingestion timed out for one source; worker cancelled.");
            } catch (ExecutionException e) {
                Throwable cause = e.getCause();
                String errorMsg = (cause != null) ? cause.getMessage() : e.getMessage();
                System.err.println("  [ERROR] Ingestion failed: " + errorMsg);
            }
        }

        System.out.println("Ingestion complete ");
    }
}