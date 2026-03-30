package com.iispl.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * IS-A Transaction — reversal of a previously settled transaction. Carries the
 * original transaction reference so the audit trail is intact.
 */
public class ReversalTransaction extends Transaction {

	private String originalRef; // referenceNumber of the transaction being reversed
	private String reversalReason;

	public ReversalTransaction() {
	}

	public ReversalTransaction(Long debitAccountId, Long creditAccountId, BigDecimal amount, String currency,
			LocalDateTime txnDate, LocalDate valueDate, String referenceNumber, String originalRef,
			String reversalReason) {
		super(debitAccountId, creditAccountId, amount, currency, txnDate, valueDate, referenceNumber);
		this.originalRef = originalRef;
		this.reversalReason = reversalReason;
	}

	@Override
	public void process() {
		super.process();
		System.out.println(
				"[ReversalTransaction] Reversing original txn: " + originalRef + " — reason: " + reversalReason);
	}

	// ------------------------------------------------------------------ //
	// Getters / Setters //
	// ------------------------------------------------------------------ //

	public String getOriginalRef() {
		return originalRef;
	}

	public void setOriginalRef(String originalRef) {
		this.originalRef = originalRef;
	}

	public String getReversalReason() {
		return reversalReason;
	}

	public void setReversalReason(String reversalReason) {
		this.reversalReason = reversalReason;
	}

	@Override
	public String toString() {
		return "ReversalTransaction{id=" + getId() + ", ref=" + getReferenceNumber() + ", originalRef=" + originalRef
				+ ", amount=" + getAmount() + " " + getCurrency() + ", status=" + getStatus() + "}";
	}
}
