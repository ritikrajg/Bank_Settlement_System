package com.iispl.dao;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;

import com.iispl.config.DBConnection;
import com.iispl.entity.IncomingTransaction;
import com.iispl.enums.TransactionStatus;

public class TransactionDao {

    private static final String INSERT =
    "INSERT INTO incoming_transactions "
    + "(txn_id, source_system, source_bank, destination_bank, "
    + "from_account, to_account, amount, status, value_date, ingested_at) "
    + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?) "
    + "ON CONFLICT (txn_id) DO NOTHING";

    private static final String UPDATE_STATUS =
        "UPDATE incoming_transactions SET status=? WHERE txn_id=?";

    public boolean insert(IncomingTransaction txn) throws SQLException {
    if (!txn.isValid()) {
        throw new IllegalArgumentException("Invalid IncomingTransaction");
    }

    try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(INSERT)) {
        ps.setString(1, txn.getTxnId());
        ps.setString(2, txn.getSourceSystem().name());
        ps.setString(3, txn.getSourceBank());
        ps.setString(4, txn.getDestinationBank());
        ps.setString(5, txn.getFromAccount());
        ps.setString(6, txn.getToAccount());
        ps.setBigDecimal(7, txn.getAmount());
        ps.setString(8, txn.getStatus().name());
        ps.setDate(9, Date.valueOf(txn.getValueDate()));
        ps.setTimestamp(10, Timestamp.valueOf(txn.getIngestedAt()));

        return ps.executeUpdate() > 0;
    }
}


    public int updateStatus(String txnId, TransactionStatus status) throws SQLException {

        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(UPDATE_STATUS)) {

            ps.setString(1, status.name());
            ps.setString(2, txnId);

            int rows = ps.executeUpdate();

            //System.out.println("Rows updated: " + rows); // DEBUG

            return rows;
        }
    }
}