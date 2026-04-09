package com.iispl.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.iispl.enums.SettlementStatus;

public class SettlementRecord extends BaseEntity {

    private String batchId;
    private String incomingTxnId;
    private String sourceBank;
    private String destinationBank;
    private BigDecimal settledAmount;
    private LocalDateTime settledDate;
    private SettlementStatus settledStatus;
    private String failureReason;

    public SettlementRecord() {
    }

    public SettlementRecord(String batchId, String incomingTxnId, BigDecimal settledAmount,
            String sourceBank, String destinationBank,SettlementStatus settledStatus) {
        this.batchId = batchId;
        this.incomingTxnId = incomingTxnId;
        this.sourceBank = sourceBank;
        this.destinationBank = destinationBank;
        this.settledAmount = settledAmount;
        this.settledStatus = settledStatus;
        this.settledDate = LocalDateTime.now();
    }

    public String getBatchId() {
        return batchId;
    }

    public void setBatchId(String batchId) {
        this.batchId = batchId;
    }

    public String getIncomingTxnId() {
        return incomingTxnId;
    }

    public void setIncomingTxnId(String incomingTxnId) {
        this.incomingTxnId = incomingTxnId;
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

    public BigDecimal getSettledAmount() {
        return settledAmount;
    }

    public void setSettledAmount(BigDecimal settledAmount) {
        this.settledAmount = settledAmount;
    }

    public LocalDateTime getSettledDate() {
        return settledDate;
    }

    public void setSettledDate(LocalDateTime settledDate) {
        this.settledDate = settledDate;
    }

    public SettlementStatus getSettledStatus() {
        return settledStatus;
    }

    public void setSettledStatus(SettlementStatus settledStatus) {
        this.settledStatus = settledStatus;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }

    @Override
    public String toString() {
        return "SettlementRecord{id=" + getId() + ", batch=" + batchId + ", txnId=" + incomingTxnId + ", amount="
                + settledAmount + ", status=" + settledStatus + "}";
    }
}