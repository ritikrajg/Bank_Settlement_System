package com.iispl.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * IS-A Transaction — inter-bank / correspondent settlement transaction. Carries
 * the correspondent bank identifier and SWIFT BIC for routing.
 */
public class InterBankTransaction extends Transaction {

	private Long correspondentBankId;
	private String correspondentBic; // SWIFT BIC of the counterparty bank

	public InterBankTransaction() {
	}

	public InterBankTransaction(Long debitAccountId, Long creditAccountId, BigDecimal amount, String currency,
			LocalDateTime txnDate, LocalDate valueDate, String referenceNumber, Long correspondentBankId,
			String correspondentBic) {
		super(debitAccountId, creditAccountId, amount, currency, txnDate, valueDate, referenceNumber);
		this.correspondentBankId = correspondentBankId;
		this.correspondentBic = correspondentBic;
	}

	@Override
	public void process() {
		super.process();
		System.out.println("[InterBankTransaction] Routing to correspondent bank " + correspondentBic + " (id="
				+ correspondentBankId + ")");
	}

	// ------------------------------------------------------------------ //
	// Getters / Setters //
	// ------------------------------------------------------------------ //

	public Long getCorrespondentBankId() {
		return correspondentBankId;
	}

	public void setCorrespondentBankId(Long correspondentBankId) {
		this.correspondentBankId = correspondentBankId;
	}

	public String getCorrespondentBic() {
		return correspondentBic;
	}

	public void setCorrespondentBic(String correspondentBic) {
		this.correspondentBic = correspondentBic;
	}

	@Override
	public String toString() {
		return "InterBankTransaction{id=" + getId() + ", ref=" + getReferenceNumber() + ", amount=" + getAmount() + " "
				+ getCurrency() + ", bic=" + correspondentBic + ", status=" + getStatus() + "}";
	}
}
