package com.iispl.dao;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;

import com.iispl.config.DBConnection;
import com.iispl.entity.SettlementBatch;
import com.iispl.enums.BatchStatus;

public class SettlementBatchDAO {

    private static final String INSERT = "INSERT INTO settlement_batches "
            + "(batch_id, settlement_date, status, total_items, total_amount, run_by, created_at, completed_at) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

    private static final String UPDATE = "UPDATE settlement_batches "
            + "SET status=?, total_items=?, total_amount=?, completed_at=? "
            + "WHERE batch_id=?";

    private static final String DELETE = "delete from settlement_batches where batch_id=?";

    public SettlementBatch insert(SettlementBatch batch) throws SQLException {

        if (!batch.isValid()) {
            throw new IllegalArgumentException("Invalid SettlementBatch: " + batch.validationErrors());
        }

        batch.prePersist(batch.getRunBy());

        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(INSERT)) {

            ps.setString(1, batch.getBatchId()); // ✅ correct
            ps.setDate(2, Date.valueOf(batch.getBatchDate()));
            ps.setString(3, batch.getBatchStatus().name());
            ps.setInt(4, batch.getTotalTransactions());
            ps.setBigDecimal(5, batch.getTotalAmount());
            ps.setString(6, batch.getRunBy());
            ps.setTimestamp(7, Timestamp.valueOf(batch.getCreatedAt()));

            if (batch.getCompletedAt() != null) {
                ps.setTimestamp(8, Timestamp.valueOf(batch.getCompletedAt()));
            } else {
                ps.setNull(8, java.sql.Types.TIMESTAMP);
            }

            ps.executeUpdate();
        }

        return batch;
    }

    public int update(SettlementBatch batch) throws SQLException {

        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(UPDATE)) {

            ps.setString(1, batch.getBatchStatus().name());
            ps.setInt(2, batch.getTotalTransactions());
            ps.setBigDecimal(3, batch.getTotalAmount());

            if (batch.getBatchStatus() == BatchStatus.PROCESSING) {
                ps.setNull(4, java.sql.Types.TIMESTAMP);
            } else {
                LocalDateTime completedAt =
                        batch.getCompletedAt() != null
                        ? batch.getCompletedAt()
                        : LocalDateTime.now();

                ps.setTimestamp(4, Timestamp.valueOf(completedAt));
            }

            ps.setString(5, batch.getBatchId()); // ✅ FIXED

            int rows = ps.executeUpdate();

            if (rows == 0) {
                throw new SQLException("Settlement batch not found id=" + batch.getBatchId());
            }

            return rows;
        }
    }
    public int delete(String batchId) throws SQLException {

        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(DELETE)) {

            ps.setString(1, batchId);

            int rows = ps.executeUpdate();

            if (rows == 0) {
                throw new SQLException("No batch found with id=" + batchId);
            }

            return rows;
        }

    }
}