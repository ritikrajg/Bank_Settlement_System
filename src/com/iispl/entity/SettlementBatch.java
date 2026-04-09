package com.iispl.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.iispl.enums.BatchStatus;

public class SettlementBatch extends BaseEntity implements Validatable {

    private String batchId;
    private String batchType;
    private LocalDate batchDate;
    private BatchStatus batchStatus;
    private int totalTransactions;
    private int failedTransactions;
    private BigDecimal totalAmount;
    private String runBy;

    // ✅ NEW (aligned with DB)
    private LocalDateTime completedAt;

    private final List<SettlementRecord> records = new ArrayList<>();

    public SettlementBatch() {
    }

    public SettlementBatch(String batchId, String batchType,LocalDate batchDate, String runBy) {
        this.batchId = batchId;
        this.batchType=batchType;
        this.batchDate = batchDate;
        this.batchStatus = BatchStatus.INITIATED; // ✅ FIXED
        this.runBy = runBy;
        this.totalTransactions=0;
        this.totalAmount = BigDecimal.ZERO;
    }

    /** Adds a record and recalculates totals */
    public void addRecord(SettlementRecord record) {
        records.add(record);
        totalTransactions = records.size();
        totalAmount = records.stream()
                .map(SettlementRecord::getSettledAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public List<SettlementRecord> getRecords() {
        return Collections.unmodifiableList(records);
    }

    @Override
    public boolean isValid() {
        return batchId != null && !batchId.isBlank()
                && batchDate != null
                && runBy != null && !runBy.isBlank();
    }

    @Override
    public String validationErrors() {
        StringBuilder sb = new StringBuilder();
        if (batchId == null || batchId.isBlank())
            sb.append("batchId required; ");
        if (batchDate == null)
            sb.append("batchDate required; ");
        if (runBy == null || runBy.isBlank())
            sb.append("runBy required; ");
        return sb.toString();
    }

    // Getters / Setters

    public String getBatchId() {
        return batchId;
    }

    public String getBatchType(){
        return batchType;
    }

    public LocalDate getBatchDate() {
        return batchDate;
    }

    public BatchStatus getBatchStatus() {
        return batchStatus;
    }

    public void setBatchStatus(BatchStatus status) {
        this.batchStatus = status;
    }

    public int getTotalTransactions() {
        return totalTransactions;
    }

    public void setTotalTransactions(int totalTransactions) {
        this.totalTransactions += totalTransactions;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = this.totalAmount.add(totalAmount);
    }

    public synchronized void recordSettlement(BigDecimal amount) {
        totalTransactions++;
        totalAmount = totalAmount.add(amount);
    }

    public synchronized void recordFailure() {
        failedTransactions++;
    }

    public int getFailedTransactions() {
        return failedTransactions;
    }

    public int getSettledTransactions() {
        return totalTransactions - failedTransactions;
    }

    public String getRunBy() {
        return runBy;
    }

    // ✅ NEW
    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    @Override
    public String toString() {
        return "SettlementBatch{batchId=" + batchId
                + ", date=" + batchDate
                + ", status=" + batchStatus
                + ", records=" + totalTransactions
                + ", total=" + totalAmount + "}";
    }
}