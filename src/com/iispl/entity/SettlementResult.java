package com.iispl.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.iispl.enums.SettlementStatus;

/**
 * Immutable-ish result object returned by SettlementProcessor (Callable).
 * Collected by CompletableFuture callbacks in the concurrency layer.
 */
public class SettlementResult {

	//private final String batchId;
	private SettlementStatus status;
	private int settledCount;
	private int failedCount;
	private BigDecimal totalSettledAmount;
	private final List<String> failureReasons = new ArrayList<>();
	private final LocalDateTime processedAt = LocalDateTime.now();

	public SettlementResult() {
	
		this.status = SettlementStatus.PENDING;
		this.totalSettledAmount = BigDecimal.ZERO;
	}

	public void addFailureReason(String reason) {
		failureReasons.add(reason);
	}

	public void incrementSettled(BigDecimal amount) {
		settledCount++;
		totalSettledAmount = totalSettledAmount.add(amount);
	}

	public void incrementFailed() {
		failedCount++;
	}

	/** Derives final status from counts. Call after all records are processed. */
	public void finalise() {
		if (failedCount == 0) {
			status = SettlementStatus.SETTLED;
		} else if (settledCount == 0) {
			status = SettlementStatus.FAILED;
		} else {
			status = SettlementStatus.PARTIALLY_SETTLED;
		}
	}

	// Getters (read-only after construction)
	// public String getBatchId() {
	// 	return batchId;
	// }

	public SettlementStatus getStatus() {
		return status;
	}

	public void setStatus(SettlementStatus s) {
		this.status = s;
	}

	public int getSettledCount() {
		return settledCount;
	}

	public int getFailedCount() {
		return failedCount;
	}

	public BigDecimal getTotalSettledAmount() {
		return totalSettledAmount;
	}

	public List<String> getFailureReasons() {
		return Collections.unmodifiableList(failureReasons);
	}

	public LocalDateTime getProcessedAt() {
		return processedAt;
	}

	@Override
	public String toString() {
		return ", status=" + status + ", settled=" + settledCount + ", failed="
				+ failedCount + ", total=" + totalSettledAmount;
	}
}