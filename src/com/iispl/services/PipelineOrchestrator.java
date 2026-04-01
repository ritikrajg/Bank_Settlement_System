package com.iispl.services;

import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import com.iispl.adapter.AdapterRegistry;
import com.iispl.dao.TransactionDao;
import com.iispl.entity.IncomingTransaction;
import com.iispl.enums.SourceType;

public class PipelineOrchestrator {

	private static final int CORE_POOL_SIZE = 5;
    private static final int MAX_POOL_SIZE = 20;
    private static final int KEEP_ALIVE_SECS = 60;
    private static final AtomicLong THREAD_COUNTER = new AtomicLong();

    private final AdapterRegistry adapterRegistry;
 
    private final TransactionDao txnDAO;

    public PipelineOrchestrator(AdapterRegistry adapterRegistry,
            TransactionDao txnDAO) {
        this.adapterRegistry = adapterRegistry;
        this.txnDAO = txnDAO;
 
    }
    private final ThreadPoolExecutor executor = new ThreadPoolExecutor(
            CORE_POOL_SIZE,
            MAX_POOL_SIZE,
            KEEP_ALIVE_SECS,
            TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(50),
            task -> new Thread(task, "settle-" + THREAD_COUNTER.incrementAndGet()));

    private final BlockingQueue<IncomingTransaction> queue = new LinkedBlockingQueue<>(1000);
    public void runPipeline(Map<SourceType, List<String>> payloads) throws Exception{
        System.out.println("\n========== PIPELINE START ==========" );
        //System.out.println("Date=" + settlementDate + " | operator=" + runBy);

        // ---- Phase 1: Ingestion ----------------------------------------
        try {
            IngestionPhase ingestionPhase=new IngestionPhase(adapterRegistry, executor, queue, txnDAO);
            ingestionPhase.runIngestion(payloads);
        } finally {
            executor.shutdown();
            if (!executor.awaitTermination(KEEP_ALIVE_SECS, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        }
    }
}