package com.iispl.entity;

import java.math.BigDecimal;

import com.iispl.enums.TransactionStatus;

/**
 * Abstract base for all transaction subtypes used during settlement processing.
 */
public abstract class Transaction {

    protected long txnId;
    protected BigDecimal amount;
    protected String currency;
    protected TransactionStatus status;
    protected String sourceBank;
    protected String destinationBank;

    protected Transaction() {
    }

    protected Transaction(long txnId, BigDecimal amount, String currency,
            String sourceBank, String destinationBank) {
        this.txnId = txnId;
        this.amount = amount;
        this.currency = currency;
        this.sourceBank = sourceBank;
        this.destinationBank = destinationBank;
        this.status = TransactionStatus.QUEUED;
    }

    /** Factory — wraps an IncomingTransaction into the correct concrete subtype. */
    

    public abstract String getType();

    // Getters / Setters
    public long getTxnId() {
        return txnId;
    }

    public void setTxnId(long txnId) {
        this.txnId = txnId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public TransactionStatus getStatus() {
        return status;
    }

    public void setStatus(TransactionStatus status) {
        this.status = status;
    }

    public String getSourceBank() {
        return sourceBank;
    }

    public void setSourceBank(String sourceBank) {
        this.sourceBank = sourceBank;
    }

    public String getDestinationBank() {
        return destinationBank;
    }

    public void setDestinationBank(String destinationBank) {
        this.destinationBank = destinationBank;
    }
}
