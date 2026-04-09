package com.iispl.services;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

import com.iispl.config.DBConnection;
import com.iispl.dao.ReconciliationEntryDAO;
import com.iispl.entity.ReconciliationEntry;
import com.iispl.enums.ReconStatus;

public class ReconciliationPhase {

    private static final String LOAD_NETTING =
            "SELECT bank_code, net_amount FROM netting_positions WHERE position_date = ?";

    private static final String LOAD_ACTUAL =
        "SELECT bank_code, SUM(net_amount) AS amount "
      + "FROM netting_positions "
      + "WHERE position_date = ? "
      + "GROUP BY bank_code";

    private final ReconciliationEntryDAO reconciliationEntryDAO;

    public ReconciliationPhase() {
        this.reconciliationEntryDAO = new ReconciliationEntryDAO();
    }

    public void runReconciliationPhase(LocalDate reconciliationDate) throws SQLException {
        System.out.println();
        System.out.println("===========================================");
        System.out.println("      Phase 4 : RECONCILIATION             ");
        System.out.println("===========================================");

        Map<String, BigDecimal> expectedByBank = loadExpectedAmounts(reconciliationDate);
        Map<String, BigDecimal> actualByBank = loadActualAmounts(reconciliationDate);

        int matched = 0;
        int unmatched = 0;

        for (String bankCode : expectedByBank.keySet()) {
            BigDecimal expected = expectedByBank.getOrDefault(bankCode, BigDecimal.ZERO);
            BigDecimal actual = actualByBank.getOrDefault(bankCode, BigDecimal.ZERO);
            BigDecimal variance = actual.subtract(expected);

            ReconciliationEntry entry = new ReconciliationEntry();
            entry.setReconciliationDate(reconciliationDate);
            entry.setAccountId(null);
            entry.setExpectedAmount(expected);
            entry.setActualAmount(actual);
            entry.setVariance(variance);

            if (variance.compareTo(BigDecimal.ZERO) == 0) {
                entry.setReconStatus(ReconStatus.MATCHED);
                entry.setRemarks("Matched for bank " + bankCode);
                matched++;
            } else {
                entry.setReconStatus(ReconStatus.UNMATCHED);
                entry.setRemarks("Mismatch for bank " + bankCode + ", variance=" + variance);
                unmatched++;
            }

            reconciliationEntryDAO.insert(entry);

            System.out.printf("  %-8s expected=%12.2f  actual=%12.2f  variance=%12.2f  status=%s%n",
                    bankCode, expected, actual, variance, entry.getReconStatus());
        }

        DBConnection.commit();

        System.out.println();
        System.out.println("  Reconciliation Date : " + reconciliationDate);
        System.out.println("  Matched banks       : " + matched);
        System.out.println("  Unmatched banks     : " + unmatched);
        System.out.println("  [OK] Reconciliation entries saved to DB.");
    }

    private Map<String, BigDecimal> loadExpectedAmounts(LocalDate reconciliationDate) throws SQLException {
        Map<String, BigDecimal> expectedByBank = new LinkedHashMap<>();

        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(LOAD_NETTING)) {
            ps.setDate(1, Date.valueOf(reconciliationDate));

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    expectedByBank.put(rs.getString("bank_code"), rs.getBigDecimal("net_amount"));
                }
            }
        }

        return expectedByBank;
    }

    private Map<String, BigDecimal> loadActualAmounts(LocalDate reconciliationDate) throws SQLException {
        Map<String, BigDecimal> actualByBank = new LinkedHashMap<>();

        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(LOAD_ACTUAL)) {
            ps.setDate(1, Date.valueOf(reconciliationDate));

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    actualByBank.put(rs.getString("bank_code"), rs.getBigDecimal("amount"));
                }
            }
        }

        return actualByBank;
    }
}
