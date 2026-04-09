package com.iispl.services;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.iispl.config.DBConnection;
import com.iispl.dao.NettingPositionDAO;
import com.iispl.dao.SettlementRecordDAO;
import com.iispl.entity.NpciAccount;
import com.iispl.entity.SettlementBatch;
import com.iispl.entity.SettlementRecord;
import com.iispl.enums.NetDirection;

/*
 * Phase 3 : Netting
 *
 * Simple explanation:
 *   - Every bank sent money to others and also received money.
 *   - We total up each bank's sent and received amounts.
 *   - Net = totalReceived - totalSent
 *       Net > 0  → bank will GET money from NPCI  (NET CREDIT)
 *       Net < 0  → bank must PAY money to NPCI    (NET DEBIT)
 *       Net = 0  → nothing to do                  (FLAT)
 *   - We save this to DB (netting_positions table)
 *   - For NET DEBIT banks, we also update npci_accounts balance in DB
 *   - We print a clean table on the console
 */
public class NettingPhase {

    private SettlementRecordDAO recordDAO;
    private NettingPositionDAO nettingDAO;

    public NettingPhase(SettlementRecordDAO recordDAO) {
        this.recordDAO = recordDAO;
        this.nettingDAO = new NettingPositionDAO();
    }

    // ---------------------------------------------------------------
    // MAIN METHOD
    // ---------------------------------------------------------------
    public Map<String, NpciAccount> runNettingPhase(
            Map<String, SettlementBatch> batchesByType,
            LocalDate nettingDate) throws SQLException {

        System.out.println();
        System.out.println("===========================================");
        System.out.println("         Phase 3 : NETTING                ");
        System.out.println("===========================================");

        // Step 1: Load all settled transactions from DB
        List<SettlementRecord> allRecords = loadAllSettledRecords(batchesByType);

        // Step 2: Calculate total sent and received per bank
        Map<String, BigDecimal> totalSent = new HashMap<>();
        Map<String, BigDecimal> totalReceived = new HashMap<>();
        calculateTotals(allRecords, totalSent, totalReceived);

        // Step 3: Get batchId (needed to save netting rows)
        String batchId = null;

        for (SettlementBatch batch : batchesByType.values()) {
            if (batch.getTotalTransactions() > 0) {
                batchId = batch.getBatchId();
                break;
            }
        }

        if (batchId == null) {
            System.out.println("  [WARN] No non-empty batch found for netting.");
            return Collections.emptyMap();
        }
        // Step 4: Calculate net per bank, save to DB, update NPCI balance
        List<NettingResult> results = saveResults(
                batchId, totalSent, totalReceived, nettingDate);

        // Step 5: Print table on console
        printNettingTable(results, nettingDate);

        // Step 6: Build NpciAccount objects for next phase
        Map<String, NpciAccount> npciAccounts = createNpciAccounts(results);

        // Commit all DB changes
        DBConnection.commit();
        System.out.println("  [OK] Netting positions saved to DB.");
        System.out.println("  [OK] NPCI account balances updated in DB.");

        return npciAccounts;
    }

    // ---------------------------------------------------------------
    // Step 1: Load all settled records across all batches
    // ---------------------------------------------------------------
    private List<SettlementRecord> loadAllSettledRecords(
            Map<String, SettlementBatch> batchesByType) throws SQLException {

        List<SettlementRecord> allRecords = new ArrayList<>();

        for (SettlementBatch batch : batchesByType.values()) {
            List<SettlementRecord> records = recordDAO.findSettledByBatchId(batch.getBatchId());
            allRecords.addAll(records);
        }

        System.out.println("\n  Total settled transactions: " + allRecords.size());
        return allRecords;
    }

    // ---------------------------------------------------------------
    // Step 2: Add up sent and received amounts per bank
    // ---------------------------------------------------------------
    private void calculateTotals(List<SettlementRecord> allRecords,
            Map<String, BigDecimal> totalSent,
            Map<String, BigDecimal> totalReceived) {

        for (SettlementRecord record : allRecords) {

            String sender = record.getSourceBank();
            String receiver = record.getDestinationBank();
            BigDecimal amount = record.getSettledAmount();

            // Sender's total sent increases
            if (totalSent.containsKey(sender)) {
                totalSent.put(sender, totalSent.get(sender).add(amount));
            } else {
                totalSent.put(sender, amount);
            }

            // Receiver's total received increases
            if (totalReceived.containsKey(receiver)) {
                totalReceived.put(receiver, totalReceived.get(receiver).add(amount));
            } else {
                totalReceived.put(receiver, amount);
            }
        }

        // Make sure every bank appears in both maps
        for (String bank : new ArrayList<>(totalSent.keySet())) {
            if (!totalReceived.containsKey(bank)) {
                totalReceived.put(bank, BigDecimal.ZERO);
            }
        }
        for (String bank : new ArrayList<>(totalReceived.keySet())) {
            if (!totalSent.containsKey(bank)) {
                totalSent.put(bank, BigDecimal.ZERO);
            }
        }
    }

    // ---------------------------------------------------------------
    // Step 4: For each bank, calculate net, save to DB, update NPCI balance
    // ---------------------------------------------------------------
    private List<NettingResult> saveResults(
            String batchId,
            Map<String, BigDecimal> totalSent,
            Map<String, BigDecimal> totalReceived,
            LocalDate nettingDate) throws SQLException {

        List<NettingResult> results = new ArrayList<>();

        // Sort bank codes alphabetically for consistent output
        List<String> banks = new ArrayList<>(totalSent.keySet());
        Collections.sort(banks);

        for (String bankCode : banks) {

            BigDecimal sent = totalSent.get(bankCode);
            BigDecimal received = totalReceived.get(bankCode);

            // Net = received - sent
            BigDecimal net = received.subtract(sent);

            NetDirection direction;
            BigDecimal netAbs;

            if (net.compareTo(BigDecimal.ZERO) > 0) {
                direction = NetDirection.NET_CREDIT; // bank will receive money
                netAbs = net;
            } else if (net.compareTo(BigDecimal.ZERO) < 0) {
                direction = NetDirection.NET_DEBIT; // bank must pay money
                netAbs = net.abs();
            } else {
                direction = NetDirection.FLAT; // balanced, nothing to do
                netAbs = BigDecimal.ZERO;
            }

            // Save netting row to netting_positions table
            nettingDAO.savePosition(
                    batchId, bankCode, sent, received, netAbs, direction, nettingDate);

            // ✅ If this bank owes money, update its balance in npci_accounts table
            if (direction == NetDirection.NET_DEBIT) {
                nettingDAO.updateNpciBalance(bankCode, netAbs);
            }

            results.add(new NettingResult(bankCode, sent, received, netAbs, direction));
        }

        return results;
    }

    // ---------------------------------------------------------------
    // Step 5: Print a clean console table
    // ---------------------------------------------------------------
    private void printNettingTable(List<NettingResult> results, LocalDate nettingDate) {

        String line = "  +-----------+------------------+------------------+------------------+--------------+";

        System.out.println();
        System.out.println("  Netting Date : " + nettingDate);
        System.out.println();
        System.out.println(line);
        System.out.printf("  | %-9s | %-16s | %-16s | %-16s | %-12s |%n",
                "Bank", "Total Sent", "Total Received", "Net Amount", "Status");
        System.out.println(line);

        for (NettingResult r : results) {

            String status;
            if (r.direction == NetDirection.NET_CREDIT) {
                status = "WILL RECEIVE";
            } else if (r.direction == NetDirection.NET_DEBIT) {
                status = "MUST PAY";
            } else {
                status = "FLAT";
            }

            System.out.printf("  | %-9s | %16.2f | %16.2f | %16.2f | %-12s |%n",
                    r.bankCode,
                    r.totalSent,
                    r.totalReceived,
                    r.netAmount,
                    status);
        }

        System.out.println(line);
        System.out.println();
        System.out.println("  WILL RECEIVE = NPCI will send money to this bank");
        System.out.println("  MUST PAY     = This bank's NPCI balance has been charged");
        System.out.println("  FLAT         = No money movement needed");
    }

    // ---------------------------------------------------------------
    // Step 6: Build in-memory NpciAccount objects for the next phase
    // ---------------------------------------------------------------
    private Map<String, NpciAccount> createNpciAccounts(List<NettingResult> results) {

        Map<String, NpciAccount> accounts = new HashMap<>();

        for (NettingResult r : results) {
            NpciAccount account = new NpciAccount(r.bankCode);

            if (r.direction == NetDirection.NET_DEBIT
                    && r.netAmount.compareTo(BigDecimal.ZERO) > 0) {
                account.credit(r.netAmount); // mirrors what we already saved to DB
            }

            accounts.put(r.bankCode, account);
        }

        return accounts;
    }

    // ---------------------------------------------------------------
    // Inner class: holds one bank's netting result (used only here)
    // ---------------------------------------------------------------
    private static class NettingResult {

        String bankCode;
        BigDecimal totalSent;
        BigDecimal totalReceived;
        BigDecimal netAmount;
        NetDirection direction;

        NettingResult(String bankCode, BigDecimal totalSent,
                BigDecimal totalReceived, BigDecimal netAmount,
                NetDirection direction) {
            this.bankCode = bankCode;
            this.totalSent = totalSent;
            this.totalReceived = totalReceived;
            this.netAmount = netAmount;
            this.direction = direction;
        }
    }
}