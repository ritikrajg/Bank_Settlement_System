package com.iispl.entity;

import java.math.BigDecimal;

/** Outbound debit — money leaving sourceBank to destinationBank. */
public class DebitTransaction extends Transaction {

    public DebitTransaction(long txnId, BigDecimal amount, String currency,
            String sourceBank, String destinationBank) {
        super(txnId, amount, currency, sourceBank, destinationBank);
    }

    @Override
    public String getType() {
        return "DEBIT";
    }

    @Override
    public String toString() {
        return String.format("Debit{id=%d, %s→%s, %.2f %s}", txnId, sourceBank, destinationBank, amount, currency);
    }
}
