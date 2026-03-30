package com.iispl.entity;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.iispl.enums.ReconStatus;

/**
 * Compares expected vs actual settlement amounts for an account on a given
 * date. Variance = actualAmount − expectedAmount (positive = over-settled,
 * negative = under-settled).
 */
public class ReconciliationEntry extends BaseEntity {

	private LocalDate reconciliationDate;
	private Long accountId; // FK → Account
	private BigDecimal expectedAmount;
	private BigDecimal actualAmount;
	private BigDecimal variance;
	private ReconStatus reconStatus;
	private String remarks;

	public ReconciliationEntry() {
	}

	public ReconciliationEntry(LocalDate reconciliationDate, Long accountId, BigDecimal expectedAmount,
			BigDecimal actualAmount) {
		this.reconciliationDate = reconciliationDate;
		this.accountId = accountId;
		this.expectedAmount = expectedAmount;
		this.actualAmount = actualAmount;
		this.variance = actualAmount.subtract(expectedAmount);
		this.reconStatus = variance.compareTo(BigDecimal.ZERO) == 0 ? ReconStatus.MATCHED : ReconStatus.UNMATCHED;
	}

	/** Recalculates variance and status from current amounts. */
	public void recalculate() {
		if (expectedAmount != null && actualAmount != null) {
			variance = actualAmount.subtract(expectedAmount);
			reconStatus = variance.compareTo(BigDecimal.ZERO) == 0 ? ReconStatus.MATCHED : ReconStatus.UNMATCHED;
		}
	}

	// Getters / Setters
	public LocalDate getReconciliationDate() {
		return reconciliationDate;
	}

	public void setReconciliationDate(LocalDate reconciliationDate) {
		this.reconciliationDate = reconciliationDate;
	}

	public Long getAccountId() {
		return accountId;
	}

	public void setAccountId(Long accountId) {
		this.accountId = accountId;
	}

	public BigDecimal getExpectedAmount() {
		return expectedAmount;
	}

	public void setExpectedAmount(BigDecimal expectedAmount) {
		this.expectedAmount = expectedAmount;
	}

	public BigDecimal getActualAmount() {
		return actualAmount;
	}

	public void setActualAmount(BigDecimal actualAmount) {
		this.actualAmount = actualAmount;
	}

	public BigDecimal getVariance() {
		return variance;
	}

	public void setVariance(BigDecimal variance) {
		this.variance = variance;
	}

	public ReconStatus getReconStatus() {
		return reconStatus;
	}

	public void setReconStatus(ReconStatus reconStatus) {
		this.reconStatus = reconStatus;
	}

	public String getRemarks() {
		return remarks;
	}

	public void setRemarks(String remarks) {
		this.remarks = remarks;
	}

	@Override
	public String toString() {
		return "ReconciliationEntry{id=" + getId() + ", date=" + reconciliationDate + ", account=" + accountId
				+ ", variance=" + variance + ", status=" + reconStatus + "}";
	}
}
