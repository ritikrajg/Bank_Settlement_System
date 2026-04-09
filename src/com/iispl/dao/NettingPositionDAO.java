package com.iispl.dao;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;

import com.iispl.config.DBConnection;
import com.iispl.enums.NetDirection;

/*
 * NettingPositionDAO
 *
 * Talks to two tables:
 *   1. netting_positions  -> saves each bank's net result
 *   2. npci_accounts      -> updates the balance for net-debit banks
 */
public class NettingPositionDAO {

    // SQL to insert one row into netting_positions
    private static final String INSERT_NETTING =
        "INSERT INTO netting_positions " +
        "  (batch_id, bank_code, gross_debit_amount, gross_credit_amount, " +
        "   net_amount, direction, currency, position_date) " +
        "VALUES (?, ?, ?, ?, ?, ?, 'INR', ?)";

    // SQL to add money to a bank's NPCI account balance
    // (used only for NET_DEBIT banks — they must pre-fund NPCI)
    private static final String UPDATE_NPCI_BALANCE =
        "UPDATE npci_accounts " +
        "SET balance = balance + ?, updated_at = NOW() " +
        "WHERE bank_code = ?";

    /*
     * Saves one bank's netting result to the netting_positions table.
     */
    public void savePosition(String batchId,
                             String bankCode,
                             BigDecimal totalSent,
                             BigDecimal totalReceived,
                             BigDecimal netAmount,
                             NetDirection direction,
                             LocalDate positionDate) throws SQLException {

        PreparedStatement ps =
                DBConnection.getConnection().prepareStatement(INSERT_NETTING);

        ps.setString(1, batchId);
        ps.setString(2, bankCode);
        ps.setBigDecimal(3, totalSent);
        ps.setBigDecimal(4, totalReceived);
        ps.setBigDecimal(5, netAmount);
        ps.setString(6, direction.name());
        ps.setDate(7, Date.valueOf(positionDate));

        ps.executeUpdate();
        ps.close();
    }

    /*
     * Updates the balance in npci_accounts table for a net-debit bank.
     *
     * Only called when direction == NET_DEBIT.
     * The bank owes money to NPCI, so we add that amount to its NPCI account.
     */
    public void updateNpciBalance(String bankCode,
                                   BigDecimal amount) throws SQLException {

        PreparedStatement ps =
                DBConnection.getConnection().prepareStatement(UPDATE_NPCI_BALANCE);

        ps.setBigDecimal(1, amount);
        ps.setString(2, bankCode);

        int rowsUpdated = ps.executeUpdate();
        ps.close();

        // If 0 rows updated, the bank_code doesn't exist in npci_accounts table
        if (rowsUpdated == 0) {
            System.out.println("  [WARN] No npci_accounts row found for bank: " + bankCode);
        }
    }
}