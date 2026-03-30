package com.iispl.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.iispl.enums.SettlementStatus;

/**
 * One settled transaction within a SettlementBatch. FK references batchId and
 * incomingTxnId (no object graph to keep JDBC simple).
 */
public class SettlementRecord extends BaseEntity {

	private String batchId; // FK → SettlementBatch
	private Long incomingTxnId; // FK → IncomingTransaction
	private BigDecimal settledAmount;
	private LocalDateTime settledDate;
	private SettlementStatus settledStatus;
	private String failureReason;

	public SettlementRecord() {
	}

	public SettlementRecord(String batchId, Long incomingTxnId, BigDecimal settledAmount,
			SettlementStatus settledStatus) {
		this.batchId = batchId;
		this.incomingTxnId = incomingTxnId;
		this.settledAmount = settledAmount;
		this.settledStatus = settledStatus;
		this.settledDate = LocalDateTime.now();
	}

	// Getters / Setters
	public String getBatchId() {
		return batchId;
	}

	public void setBatchId(String batchId) {
		this.batchId = batchId;
	}

	public Long getIncomingTxnId() {
		return incomingTxnId;
	}

	public void setIncomingTxnId(Long incomingTxnId) {
		this.incomingTxnId = incomingTxnId;
	}

	public BigDecimal getSettledAmount() {
		return settledAmount;
	}

	public void setSettledAmount(BigDecimal a) {
		this.settledAmount = a;
	}

	public LocalDateTime getSettledDate() {
		return settledDate;
	}

	public void setSettledDate(LocalDateTime d) {
		this.settledDate = d;
	}

	public SettlementStatus getSettledStatus() {
		return settledStatus;
	}

	public void setSettledStatus(SettlementStatus s) {
		this.settledStatus = s;
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
