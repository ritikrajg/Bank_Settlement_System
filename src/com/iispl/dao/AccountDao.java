package com.iispl.dao;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Optional;

import com.iispl.config.DBConnection;
import com.iispl.entity.Account;
import com.iispl.enums.AccountStatus;
import com.iispl.enums.AccountType;

/**
 * Data Access Object for the Account table.
 * Handles finding accounts by number and updating balances.
 */
public class AccountDao {

    private static final String FIND_BY_NUMBER = "SELECT * FROM account WHERE account_number=?";
    private static final String UPDATE_BALANCE =
        "UPDATE account SET balance=?, updated_at=NOW() WHERE account_number=?";

    /**
     * Finds an account by its account number.
     * Returns Optional.empty() if not found.
     */
    public Optional<Account> findByAccountNumber(String number) throws SQLException {
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(FIND_BY_NUMBER)) {
            ps.setString(1, number);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return Optional.of(mapResultSetToAccount(rs));
            } else {
                return Optional.empty();
            }
        }
    }

    /**
     * Updates only the balance of an account.
     * Used after credit/debit operations.
     */
    public int updateBalance(Account account) throws SQLException {
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(UPDATE_BALANCE)) {
            ps.setBigDecimal(1, account.getBalanceSnapshot());
            ps.setString(2, account.getAccountNumber());
            return ps.executeUpdate();
        }
    }

    /**
     * Converts a database ResultSet row into an Account object.
     * Each column is read separately for clarity.
     */
    private Account mapResultSetToAccount(ResultSet rs) throws SQLException {

        Account account = new Account();

        // Basic fields
        account.setId(rs.getString("id"));
        account.setAccountNumber(rs.getString("account_number"));
        account.setAccountType(AccountType.valueOf(rs.getString("account_type")));
        account.setBalance(rs.getBigDecimal("balance"));
        account.setCurrency(rs.getString("currency"));
        account.setStatus(AccountStatus.valueOf(rs.getString("status")));

        // Customer ID (can be null)
        long customerId = rs.getLong("customer_id");
        if (!rs.wasNull()) {
            account.setCustomerId(customerId);
        }

        // Bank ID (can be null)
        String bankId = rs.getString("bank_id");
        if (bankId != null) {
            account.setBankId(bankId);
        }

        // Timestamps (can be null)
        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            account.setCreatedAt(createdAt.toLocalDateTime());
        }

        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) {
            account.setUpdatedAt(updatedAt.toLocalDateTime());
        }

        return account;
    }
}
