package com.iispl.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * IS-A Transaction — represents an inbound credit to a beneficiary account.
 * Adds the beneficiary account reference for downstream notification.
 */
public class CreditTransaction extends Transaction {

	/** The account number of the beneficiary (for display / notification). */
	private String creditAccountRef;

	public CreditTransaction() {
	}

	public CreditTransaction(Long debitAccountId, Long creditAccountId, BigDecimal amount, String currency,
			LocalDateTime txnDate, LocalDate valueDate, String referenceNumber, String creditAccountRef) {
		super(debitAccountId, creditAccountId, amount, currency, txnDate, valueDate, referenceNumber);
		this.creditAccountRef = creditAccountRef;
	}

	@Override
	public void process() {
		super.process();
		// Credit-specific logic: mark beneficiary for notification
		System.out.println("[CreditTransaction] Queued credit notification for account: " + creditAccountRef);
	}

	// ------------------------------------------------------------------ //
	// Getters / Setters //
	// ------------------------------------------------------------------ //

	public String getCreditAccountRef() {
		return creditAccountRef;
	}

	public void setCreditAccountRef(String creditAccountRef) {
		this.creditAccountRef = creditAccountRef;
	}

	@Override
	public String toString() {
		return "CreditTransaction{id=" + getId() + ", ref=" + getReferenceNumber() + ", amount=" + getAmount() + " "
				+ getCurrency() + ", creditAcctRef=" + creditAccountRef + ", status=" + getStatus() + "}";
	}
}
