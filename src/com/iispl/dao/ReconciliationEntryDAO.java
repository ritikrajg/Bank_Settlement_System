package com.iispl.dao;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;

import com.iispl.config.DBConnection;
import com.iispl.entity.ReconciliationEntry;

public class ReconciliationEntryDAO {

    private static final String INSERT =
            "INSERT INTO reconciliation_entries "
          + "(reconciliation_date, account_id, expected_amount, actual_amount, variance, recon_status, remarks) "
          + "VALUES (?, ?, ?, ?, ?, ?, ?)";

    public void insert(ReconciliationEntry entry) throws SQLException {
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(INSERT)) {
            ps.setDate(1, Date.valueOf(entry.getReconciliationDate()));

            if (entry.getAccountId() != null) {
                ps.setLong(2, entry.getAccountId());
            } else {
                ps.setNull(2, Types.BIGINT);
            }

            ps.setBigDecimal(3, entry.getExpectedAmount());
            ps.setBigDecimal(4, entry.getActualAmount());
            ps.setBigDecimal(5, entry.getVariance());
            ps.setString(6, entry.getReconStatus().name());

            if (entry.getRemarks() != null) {
                ps.setString(7, entry.getRemarks());
            } else {
                ps.setNull(7, Types.VARCHAR);
            }

            ps.executeUpdate();
        }
    }
}
