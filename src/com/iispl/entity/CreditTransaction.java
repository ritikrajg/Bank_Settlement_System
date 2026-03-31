package com.iispl.entity;

import java.math.BigDecimal;

/** Inbound credit — money arriving at destinationBank from sourceBank. */
public class CreditTransaction extends Transaction {

    public CreditTransaction(long txnId, BigDecimal amount, String currency,
            String sourceBank, String destinationBank) {
        super(txnId, amount, currency, sourceBank, destinationBank);
    }

    @Override
    public String getType() {
        return "CREDIT";
    }

    @Override
    public String toString() {
        return String.format("Credit{id=%d, %s→%s, %.2f %s}", txnId, sourceBank, destinationBank, amount, currency);
    }
}