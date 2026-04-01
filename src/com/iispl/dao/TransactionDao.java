package com.iispl.dao;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;

import com.iispl.config.DBConnection;
import com.iispl.entity.IncomingTransaction;
import com.iispl.enums.TransactionStatus;

public class TransactionDao {

    private static final String INSERT =
            "INSERT INTO incoming_transactions "
            + "(source_ref, source_system, source_bank, destination_bank, "
            + "amount, currency, txn_type, status, value_date, ingested_at, raw_payload) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING txn_id";

    private static final String UPDATE_STATUS =
            "UPDATE incoming_transactions SET status=? WHERE txn_id=?";

    public IncomingTransaction insert(IncomingTransaction txn) throws SQLException {

        if (!txn.isValid()) {
            throw new IllegalArgumentException("Invalid IncomingTransaction");
        }

        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(INSERT)) {
            ps.setString(1, txn.getSourceRef());
            if (txn.getSourceSystem() != null) {
                ps.setString(2, txn.getSourceSystem().name());
            } else {
                ps.setNull(2, Types.VARCHAR);
            }
            ps.setString(3, txn.getSourceBank());
            ps.setString(4, txn.getDestinationBank());
            ps.setBigDecimal(5, txn.getAmount());
            ps.setString(6, txn.getCurrency());
            ps.setString(7, txn.getTxnType().name());
            ps.setString(8, txn.getStatus().name());
            ps.setDate(9, Date.valueOf(txn.getValueDate()));
            ps.setTimestamp(10, Timestamp.valueOf(txn.getIngestedAt()));
            ps.setString(11, txn.getRawPayload());

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    txn.setTxnId(rs.getLong(1));
                } else {
                    throw new SQLException("Insert succeeded but no txn_id was returned");
                }
            }
        }

        return txn;
    }

    public int updateStatus(Long id, TransactionStatus status) throws SQLException {
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(UPDATE_STATUS)) {
            ps.setString(1, status.name());
            ps.setLong(2, id);
            return ps.executeUpdate();
        }
    }
}
