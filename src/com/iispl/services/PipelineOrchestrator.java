package com.iispl.services;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import com.iispl.adapter.AdapterRegistry;
import com.iispl.config.DBConnection;
import com.iispl.dao.AccountDao;
import com.iispl.dao.CustomerDao;
import com.iispl.dao.SettlementBatchDAO;
import com.iispl.dao.SettlementRecordDAO;
import com.iispl.dao.TransactionDao;
import com.iispl.entity.IncomingTransaction;
import com.iispl.entity.NpciAccount;
import com.iispl.entity.SettlementBatch;
import com.iispl.entity.SettlementResult;
import com.iispl.enums.BatchStatus;
import com.iispl.enums.SourceType;

/**
 * Coordinates the two-phase bank settlement pipeline:
 *
 * Phase 1 – Ingestion : parallel producer threads parse raw payloads via
 * channel adapters and push IncomingTransactions onto
 * a bounded blocking queue.
 * Phase 2 – Settlement : parallel consumer threads drain the queue, validate
 * accounts / KYC, and persist settlement records.
 *
 * Both phases run concurrently; the settlement phase starts listening before
 * ingestion begins, so there is no latency gap between the two.
 */
public class PipelineOrchestrator {

        // Number of producer threads (one per channel source is ideal; capped at 5)
        private static final int PRODUCER_POOL_SIZE = 5;

        // Number of consumer/settlement threads – at least 2, scales with CPU cores
        private static final int CONSUMER_POOL_SIZE = Math.max(2, Runtime.getRuntime().availableProcessors());

        // Seconds to wait for graceful thread-pool shutdown before forcing it
        private static final int KEEP_ALIVE_SECS = 60;

        // Shared bounded queue between ingestion producers and settlement consumers
        private static final int QUEUE_CAPACITY = 100;

        // --- Dependencies injected at construction time ---
        private final AdapterRegistry adapterRegistry;
        private final TransactionDao txnDAO;
        private final SettlementBatchDAO batchDAO;
        private final AccountDao accountDao;
        private final SettlementRecordDAO recordDAO;
        private final CustomerDao customerDao;

        // Thread pools created once and shut down after the pipeline finishes
        private final ExecutorService producerExecutor = Executors.newFixedThreadPool(PRODUCER_POOL_SIZE);
        private final ExecutorService consumerExecutor = Executors.newFixedThreadPool(CONSUMER_POOL_SIZE);

        // Shared queue connecting producers (ingestion) to consumers (settlement)
        private final BlockingQueue<IncomingTransaction> queue = new LinkedBlockingQueue<>(QUEUE_CAPACITY);

        public PipelineOrchestrator(AdapterRegistry adapterRegistry,
                        TransactionDao txnDAO,
                        SettlementBatchDAO batchDAO,
                        AccountDao accountDao,
                        SettlementRecordDAO recordDAO,
                        CustomerDao customerDao) {
                this.adapterRegistry = adapterRegistry;
                this.txnDAO = txnDAO;
                this.batchDAO = batchDAO;
                this.accountDao = accountDao;
                this.recordDAO = recordDAO;
                this.customerDao = customerDao;
        }

        /**
         * Executes the full pipeline:
         * 1. Creates a {@link SettlementBatch} record for every source type.
         * 2. Starts the settlement consumer phase asynchronously.
         * 3. Runs the ingestion producer phase (blocks until all payloads are parsed).
         * 4. Signals ingestion completion and waits for settlement to finish.
         * 5. Shuts down both thread pools cleanly.
         *
         * @param payloads map of source type → raw payload lines/messages
         * @return list of {@link SettlementResult} objects, one per consumer thread
         */
        public List<SettlementResult> runPipeline(Map<SourceType, List<String>> payloads)
                        throws Exception {

                System.out.println("Starting pipeline...");

                // Create DB batch records for each channel before processing begins
                Map<String, SettlementBatch> batchesByType = initializeBatches(payloads.keySet(), LocalDate.now(),
                                "Demo");

                // Flag shared between producers and consumers to signal ingestion is done
                AtomicBoolean ingestionCompleted = new AtomicBoolean(false);

                List<SettlementResult> results;
                try {
                        // Start settlement consumers FIRST so they are ready to drain the queue
                        SettlementPhase settlementPhase = new SettlementPhase(
                                        batchDAO, accountDao, recordDAO, txnDAO, customerDao, consumerExecutor);
                        List<Future<SettlementResult>> settlementFutures = settlementPhase.startSettlementPhase(
                                        queue, ingestionCompleted, CONSUMER_POOL_SIZE,
                                        batchesByType, LocalDate.now(), "Demo");

                        // Run ingestion producers (blocks until all sources are fully parsed)
                        IngestionPhase ingestionPhase = new IngestionPhase(adapterRegistry, producerExecutor, queue,
                                        txnDAO);
                        int ingestionRejected = ingestionPhase.runIngestion(payloads);

                        // Signal to consumers that no more transactions will be enqueued
                        ingestionCompleted.set(true);

                        // Wait for all consumer futures to complete
                        results = settlementPhase.collectSettlementResults(
                                        settlementFutures, batchesByType, ingestionRejected);

                        NettingPhase nettingPhase = new NettingPhase(recordDAO);
                        Map<String, NpciAccount> npciAccounts = nettingPhase.runNettingPhase(batchesByType,
                                        LocalDate.now());

                        ReconciliationPhase reconciliationPhase = new ReconciliationPhase();
                        reconciliationPhase.runReconciliationPhase(LocalDate.now());

                } finally {
                        // Ensure ingestion flag is set even if an exception was thrown above
                        ingestionCompleted.set(true);
                        shutdownExecutor(producerExecutor);
                        shutdownExecutor(consumerExecutor);
                }

                return results;
        }

        /**
         * Creates and persists one {@link SettlementBatch} per source type.
         * Each batch tracks aggregate counts and amounts for its channel.
         *
         * @param sourceTypes    the set of channels that have payloads
         * @param settlementDate date for which the batch is being run
         * @param runBy          identifier of the operator or scheduler
         * @return map of source-type name → initialized SettlementBatch
         */
        private Map<String, SettlementBatch> initializeBatches(
                        Set<SourceType> sourceTypes,
                        LocalDate settlementDate,
                        String runBy) throws SQLException {

                Map<String, SettlementBatch> batchesByType = new LinkedHashMap<>();
                int sequence = 1;
                String runToken = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HHmmss"));

                for (SourceType sourceType : sourceTypes) {
                        String batchId = sourceType.name() + "-BATCH-" + settlementDate + "-" + runToken + "-"
                                        + sequence++;

                        SettlementBatch batch = new SettlementBatch(
                                        batchId, sourceType.name(), settlementDate, runBy);
                        batch.setBatchStatus(BatchStatus.PROCESSING);

                        try {
                                batchDAO.insert(batch);
                                DBConnection.commit();
                                batchesByType.put(sourceType.name(), batch);
                        } catch (SQLException e) {
                                DBConnection.rollback();
                                throw e;
                        } finally {
                                DBConnection.close();
                        }
                }

                return batchesByType;
        }

        /**
         * Gracefully shuts down an executor, waiting up to {@code KEEP_ALIVE_SECS}
         * seconds before forcing an immediate shutdown.
         */
        private void shutdownExecutor(ExecutorService executor) throws InterruptedException {
                executor.shutdown();
                if (!executor.awaitTermination(KEEP_ALIVE_SECS, TimeUnit.SECONDS)) {
                        executor.shutdownNow();
                }
        }
}
