package com.iispl.dao;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Optional;

import com.iispl.config.DBConnection;
import com.iispl.entity.Customer;
import com.iispl.enums.KycStatus;

public class CustomerDao {
    private static final String FIND_BY_ID    = "SELECT * FROM customer WHERE id=?";
    public Optional<Customer> findById(Long id) throws SQLException {
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(FIND_BY_ID)) {
            ps.setLong(1, id);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? Optional.of(map(rs)) : Optional.empty();
        }
    }

    private Customer map(ResultSet rs) throws SQLException {
        Customer c = new Customer();
        c.setId(rs.getString("id"));
        c.setFirstName(rs.getString("first_name"));
        c.setLastName(rs.getString("last_name"));
        c.setEmail(rs.getString("email"));
        c.setKycStatus(KycStatus.valueOf(rs.getString("kyc_status")));
        c.setCustomerTier(rs.getString("customer_tier"));
        Date od = rs.getDate("onboarding_date");
        if (od != null) c.setOnboardingDate(od.toLocalDate());
        Timestamp ca = rs.getTimestamp("created_at");
        if (ca != null) c.setCreatedAt(ca.toLocalDateTime());
        Timestamp ua = rs.getTimestamp("updated_at");
        if (ua != null) c.setUpdatedAt(ua.toLocalDateTime());
        return c;
    }
}
