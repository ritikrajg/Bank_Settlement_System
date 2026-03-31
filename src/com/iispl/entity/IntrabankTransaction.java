package com.iispl.entity;

import java.math.BigDecimal;

public class IntrabankTransaction extends Transaction {

    public IntrabankTransaction(long txnId, BigDecimal amount, String currency,
                                String sourceBank, String destinationBank) {
        super(txnId, amount, currency, sourceBank, destinationBank);
    }

    @Override public String getType() { return "INTRABANK"; }

    @Override public String toString() {
        return String.format("Intrabank{id=%d, %s, %.2f %s}", txnId, sourceBank, amount, currency);
    }
}
