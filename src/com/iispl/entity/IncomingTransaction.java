package com.iispl.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.iispl.enums.SourceType;
import com.iispl.enums.TransactionStatus;
import com.iispl.enums.TransactionType;

public class IncomingTransaction {
    private long txnId;
    private String sourceRef;
    private SourceType sourceSystem;
    private String sourceBank;
    private String destinationBank;
    private String fromAccount;
    private String toAccount;
    private BigDecimal amount;
    private String currency;
    private TransactionType txnType;
    private TransactionStatus status;
    private LocalDate valueDate;
    private LocalDateTime ingestedAt;
    private String rawPayload;

    public IncomingTransaction() {
        this.status = TransactionStatus.RECEIVED;
        this.ingestedAt = LocalDateTime.now();
    }

    public boolean isValid() {
        return sourceRef != null && !sourceRef.isBlank()
                && sourceBank != null && !sourceBank.isBlank()
                && destinationBank != null && !destinationBank.isBlank()
                && fromAccount != null && !fromAccount.isBlank()
                && toAccount != null && !toAccount.isBlank()
                && amount != null && amount.compareTo(BigDecimal.ZERO) > 0
                && currency != null && currency.length() == 3
                && txnType != null
                && valueDate != null;
    }

    public long getTxnId() { return txnId; }
    public void setTxnId(long txnId) { this.txnId = txnId; }
    public String getSourceRef() { return sourceRef; }
    public void setSourceRef(String sourceRef) { this.sourceRef = sourceRef; }
    public SourceType getSourceSystem() { return sourceSystem; }
    public void setSourceSystem(SourceType sourceSystem) { this.sourceSystem = sourceSystem; }
    public String getSourceBank() { return sourceBank; }
    public void setSourceBank(String sourceBank) { this.sourceBank = sourceBank; }
    public String getDestinationBank() { return destinationBank; }
    public void setDestinationBank(String destinationBank) { this.destinationBank = destinationBank; }
    public String getFromAccount() { return fromAccount; }
    public void setFromAccount(String fromAccount) { this.fromAccount = fromAccount; }
    public String getToAccount() { return toAccount; }
    public void setToAccount(String toAccount) { this.toAccount = toAccount; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public TransactionType getTxnType() { return txnType; }
    public void setTxnType(TransactionType txnType) { this.txnType = txnType; }
    public TransactionStatus getStatus() { return status; }
    public void setStatus(TransactionStatus status) { this.status = status; }
    public LocalDate getValueDate() { return valueDate; }
    public void setValueDate(LocalDate valueDate) { this.valueDate = valueDate; }
    public LocalDateTime getIngestedAt() { return ingestedAt; }
    public void setIngestedAt(LocalDateTime ingestedAt) { this.ingestedAt = ingestedAt; }
    public String getRawPayload() { return rawPayload; }
    public void setRawPayload(String rawPayload) { this.rawPayload = rawPayload; }

    @Override
    public String toString() {
        return String.format(
                "IncomingTransaction{ref='%s', %s/%s -> %s/%s, %s %.2f %s, %s}",
                sourceRef, sourceBank, fromAccount, destinationBank, toAccount,
                txnType, amount, currency, status);
    }
}
