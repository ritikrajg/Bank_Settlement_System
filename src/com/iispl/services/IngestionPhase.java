package com.iispl.services;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.iispl.adapter.AdapterRegistry;
import com.iispl.adapter.TransactionAdapter;

import com.iispl.entity.IncomingTransaction;
import com.iispl.enums.SourceType;


public class IngestionPhase {

	private final AdapterRegistry adapterRegistry;
    private final ExecutorService executor;
    private final java.util.concurrent.BlockingQueue<IncomingTransaction> queue;
    

    public IngestionPhase(AdapterRegistry adapterRegistry,
            ExecutorService executor,
            java.util.concurrent.BlockingQueue<IncomingTransaction> pipelineQueue
            ) {
        this.adapterRegistry = adapterRegistry;
        this.executor = executor;
        this.queue = pipelineQueue;
        
    }
    public void runIngestion(Map<SourceType, List<String>> payloads) throws InterruptedException {
        List<Future<?>> futures = new ArrayList<>();

        for (Map.Entry<SourceType, List<String>> entry : payloads.entrySet()) {
            SourceType type = entry.getKey();
            if (!adapterRegistry.hasAdapter(type)) {
                System.err.println("  Skipped source with no adapter: " + type);
                continue;
            }

            try {
                TransactionAdapter adapter = adapterRegistry.getAdapter(type);
                
            } catch (Exception e) {
                System.err.println("  Could not start ingestion for " + type + ": " + e.getMessage());
            }
        }

        for (Future<?> future : futures) {
            try {
                future.get(60, TimeUnit.SECONDS);
            } catch (TimeoutException e) {
                future.cancel(true);
                System.err.println("  Ingestion timed out for one worker.");
            } catch (ExecutionException e) {
                System.err.println("  Ingestion worker failed: " + e.getCause());
            }
        }

        System.out.printf("  Ingestion queue depth: %d%n", queue.size());
    }
}