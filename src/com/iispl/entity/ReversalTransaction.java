package com.iispl.entity;

import java.math.BigDecimal;

/** Reversal of a previously settled transaction — swaps the net direction. */
public class ReversalTransaction extends Transaction {

    public ReversalTransaction(long txnId, BigDecimal amount, String currency,
                               String sourceBank, String destinationBank) {
        super(txnId, amount, currency, sourceBank, destinationBank);
    }

    @Override public String getType() { return "REVERSAL"; }

    @Override public String toString() {
        return String.format("Reversal{id=%d, %s→%s, %.2f %s}", txnId, sourceBank, destinationBank, amount, currency);
    }
}
