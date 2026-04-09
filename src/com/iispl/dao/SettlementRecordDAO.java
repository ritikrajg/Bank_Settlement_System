package com.iispl.dao;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.sql.Types;

import com.iispl.config.DBConnection;
import com.iispl.entity.SettlementRecord;
import com.iispl.enums.SettlementStatus;

public class SettlementRecordDAO {

    private static final String INSERT =
        "INSERT INTO settlement_records "
        + "(batch_id, incoming_txn_id, source_bank, destination_bank, settled_amount, status, failure_reason, settled_at) "
        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?) RETURNING record_id";

    private static final String FIND_SETTLED_BY_BATCH =
        "SELECT record_id, batch_id, incoming_txn_id, source_bank, destination_bank, "
        + "settled_amount, status, failure_reason, settled_at "
        + "FROM settlement_records WHERE batch_id=? AND status=?";

    public SettlementRecord insert(SettlementRecord record) throws SQLException {

        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(INSERT)) {

            ps.setString(1, record.getBatchId());
            ps.setString(2, record.getIncomingTxnId());
            ps.setString(3, record.getSourceBank());
            ps.setString(4, record.getDestinationBank());
            ps.setBigDecimal(5, record.getSettledAmount());
            ps.setString(6, record.getSettledStatus().name());

            // ✅ FIX 1: Handle NULL failure reason
            if (record.getFailureReason() != null) {
                ps.setString(7, record.getFailureReason());
            } else {
                ps.setNull(7, Types.VARCHAR);
            }

            // ✅ FIX 2: Handle NULL timestamp
            if (record.getSettledDate() != null) {
                ps.setTimestamp(8, java.sql.Timestamp.valueOf(record.getSettledDate()));
            } else {
                ps.setNull(8, Types.TIMESTAMP);
            }

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    record.setId(rs.getString("record_id"));
                }
            }
        }

        return record;
    }

    public List<SettlementRecord> findSettledByBatchId(String batchId) throws SQLException {
        List<SettlementRecord> records = new ArrayList<>();

        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(FIND_SETTLED_BY_BATCH)) {
            ps.setString(1, batchId);
            ps.setString(2, SettlementStatus.SETTLED.name());

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    records.add(map(rs));
                }
            }
        }

        return records;
    }

    private SettlementRecord map(ResultSet rs) throws SQLException {
        SettlementRecord record = new SettlementRecord();
        record.setId(rs.getString("record_id"));
        record.setBatchId(rs.getString("batch_id"));
        record.setIncomingTxnId(rs.getString("incoming_txn_id"));
        record.setSourceBank(rs.getString("source_bank"));
        record.setDestinationBank(rs.getString("destination_bank"));
        record.setSettledAmount(rs.getBigDecimal("settled_amount"));
        record.setSettledStatus(SettlementStatus.valueOf(rs.getString("status")));
        record.setFailureReason(rs.getString("failure_reason"));

        Timestamp settledAt = rs.getTimestamp("settled_at");
        if (settledAt != null) {
            record.setSettledDate(settledAt.toLocalDateTime());
        }

        return record;
    }
}