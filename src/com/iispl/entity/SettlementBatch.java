package com.iispl.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.iispl.enums.BatchStatus;

/**
 * Groups a set of transactions for atomic batch settlement. HAS-A
 * List<SettlementRecord> — records are composed inside the batch.
 */
public class SettlementBatch extends BaseEntity implements Validatable {

	private String batchId;
	private LocalDate batchDate;
	private BatchStatus batchStatus;
	private int totalTransactions;
	private BigDecimal totalAmount;
	private String runBy;
	private LocalDateTime runAt;

	/** Composed records — owned by this batch. */
	private final List<SettlementRecord> records = new ArrayList<>();

	public SettlementBatch() {
	}

	public SettlementBatch(String batchId, LocalDate batchDate, String runBy) {
		this.batchId = batchId;
		this.batchDate = batchDate;
		this.batchStatus = BatchStatus.SCHEDULED;
		this.runBy = runBy;
		this.totalAmount = BigDecimal.ZERO;
	}

	/** Adds a record and recalculates totals. */
	public void addRecord(SettlementRecord record) {
		records.add(record);
		totalTransactions = records.size();
		totalAmount = records.stream().map(SettlementRecord::getSettledAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
	}

	public List<SettlementRecord> getRecords() {
		return Collections.unmodifiableList(records);
	}

	@Override
	public boolean isValid() {
		return batchId != null && !batchId.isBlank() && batchDate != null && runBy != null && !runBy.isBlank();
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

	public void setBatchId(String batchId) {
		this.batchId = batchId;
	}

	public LocalDate getBatchDate() {
		return batchDate;
	}

	public void setBatchDate(LocalDate batchDate) {
		this.batchDate = batchDate;
	}

	public BatchStatus getBatchStatus() {
		return batchStatus;
	}

	public void setBatchStatus(BatchStatus s) {
		this.batchStatus = s;
	}

	public int getTotalTransactions() {
		return totalTransactions;
	}

	public void setTotalTransactions(int n) {
		this.totalTransactions = n;
	}

	public BigDecimal getTotalAmount() {
		return totalAmount;
	}

	public void setTotalAmount(BigDecimal a) {
		this.totalAmount = a;
	}

	public String getRunBy() {
		return runBy;
	}

	public void setRunBy(String runBy) {
		this.runBy = runBy;
	}

	public LocalDateTime getRunAt() {
		return runAt;
	}

	public void setRunAt(LocalDateTime runAt) {
		this.runAt = runAt;
	}

	@Override
	public String toString() {
		return "SettlementBatch{batchId=" + batchId + ", date=" + batchDate + ", status=" + batchStatus + ", records="
				+ totalTransactions + ", total=" + totalAmount + "}";
	}
}
