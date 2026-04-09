package com.iispl.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.iispl.enums.SourceType;
import com.iispl.enums.TransactionStatus;

public class IncomingTransaction {

    private String txnId;
    private SourceType sourceSystem;
    private String sourceBank;
    private String destinationBank;
    private String fromAccount;
    private String toAccount;
    private BigDecimal amount;
    private TransactionStatus status;
    private LocalDate valueDate;
    private LocalDateTime ingestedAt;

    public IncomingTransaction(
            String txnId,
            SourceType sourceSystem,
            String sourceBank,
            String destinationBank,
            String fromAccount,
            String toAccount,
            BigDecimal amount,
            LocalDate valueDate
    ) {
        this.txnId = txnId;
        this.sourceSystem = sourceSystem;
        this.sourceBank = sourceBank;
        this.destinationBank = destinationBank;
        this.fromAccount = fromAccount;
        this.toAccount = toAccount;
        this.amount = amount;
        this.valueDate = valueDate;
        this.status = TransactionStatus.RECEIVED;
        this.ingestedAt = LocalDateTime.now();
    }

    public boolean isValid() {
        return sourceBank != null && !sourceBank.isBlank()
                && destinationBank != null && !destinationBank.isBlank()
                && fromAccount != null && !fromAccount.isBlank()
                && toAccount != null && !toAccount.isBlank()
                && amount != null && amount.compareTo(BigDecimal.ZERO) > 0
                && valueDate != null;
    }

    public String getTxnId() { return txnId; }
    public SourceType getSourceSystem() { return sourceSystem; }
    public String getSourceBank() { return sourceBank; }
    public String getDestinationBank() { return destinationBank; }
    public String getFromAccount() { return fromAccount; }
    public String getToAccount() { return toAccount; }
    public BigDecimal getAmount() { return amount; }
    public TransactionStatus getStatus() { return status; }
    public void setStatus(TransactionStatus status) { this.status = status; }
    public LocalDate getValueDate() { return valueDate; }
    public LocalDateTime getIngestedAt() { return ingestedAt; }

    @Override
    public String toString() {
        return String.format(
                "Txn{id='%s', %s/%s -> %s/%s %.2f, status=%s}",
                txnId,
                sourceBank, fromAccount,
                destinationBank, toAccount,
                amount,
                status
        );
    }
}