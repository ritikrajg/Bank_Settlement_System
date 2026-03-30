package com.iispl.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * IS-A Transaction — represents an outbound debit from a remitter account. Adds
 * the debit account reference for the balance-check hook.
 */
public class DebitTransaction extends Transaction {

	/** Account number string of the remitter (for balance-check / statement). */
	private String debitAccountRef;

	public DebitTransaction() {
	}

	public DebitTransaction(Long debitAccountId, Long creditAccountId, BigDecimal amount, String currency,
			LocalDateTime txnDate, LocalDate valueDate, String referenceNumber, String debitAccountRef) {
		super(debitAccountId, creditAccountId, amount, currency, txnDate, valueDate, referenceNumber);
		this.debitAccountRef = debitAccountRef;
	}

	@Override
	public void process() {
		super.process();
		System.out.println(
				"[DebitTransaction] Initiating debit from account: " + debitAccountRef + " for amount: " + getAmount());
	}

	// ------------------------------------------------------------------ //
	// Getters / Setters //
	// ------------------------------------------------------------------ //

	public String getDebitAccountRef() {
		return debitAccountRef;
	}

	public void setDebitAccountRef(String debitAccountRef) {
		this.debitAccountRef = debitAccountRef;
	}

	@Override
	public String toString() {
		return "DebitTransaction{id=" + getId() + ", ref=" + getReferenceNumber() + ", amount=" + getAmount() + " "
				+ getCurrency() + ", debitAcctRef=" + debitAccountRef + ", status=" + getStatus() + "}";
	}
}
