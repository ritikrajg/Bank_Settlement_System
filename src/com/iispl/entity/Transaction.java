package com.iispl.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.iispl.enums.TransactionStatus;

/**
 * Abstract base for every transaction subtype. Concrete subclasses add
 * channel-specific fields.
 */
public abstract class Transaction extends BaseEntity implements Validatable, Processable {

	private Long debitAccountId;
	private Long creditAccountId;
	private BigDecimal amount;
	private String currency;
	private LocalDateTime txnDate;
	private LocalDate valueDate;
	private TransactionStatus status;
	private String referenceNumber;

	protected Transaction() {
	}

	protected Transaction(Long debitAccountId, Long creditAccountId, BigDecimal amount, String currency,
			LocalDateTime txnDate, LocalDate valueDate, String referenceNumber) {
		this.debitAccountId = debitAccountId;
		this.creditAccountId = creditAccountId;
		this.amount = amount;
		this.currency = currency;
		this.txnDate = txnDate;
		this.valueDate = valueDate;
		this.referenceNumber = referenceNumber;
		this.status = TransactionStatus.INITIATED;
	}

	// ------------------------------------------------------------------ //
	// Processable — subclasses may override process() for custom logic //
	// ------------------------------------------------------------------ //

	@Override
	public boolean canProcess() {
		return isValid() && (status == TransactionStatus.INITIATED || status == TransactionStatus.VALIDATED);
	}

	@Override
	public void process() {
		if (!canProcess()) {
			throw new IllegalStateException(
					"Transaction " + referenceNumber + " cannot be processed in status: " + status);
		}
		status = TransactionStatus.PENDING_SETTLEMENT;
	}

	// ------------------------------------------------------------------ //
	// Validatable //
	// ------------------------------------------------------------------ //

	@Override
	public boolean isValid() {
		return debitAccountId != null && creditAccountId != null && amount != null
				&& amount.compareTo(BigDecimal.ZERO) > 0 && currency != null && currency.length() == 3
				&& txnDate != null && valueDate != null && referenceNumber != null && !referenceNumber.isBlank();
	}

	@Override
	public String validationErrors() {
		StringBuilder sb = new StringBuilder();
		if (debitAccountId == null)
			sb.append("debitAccountId required; ");
		if (creditAccountId == null)
			sb.append("creditAccountId required; ");
		if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0)
			sb.append("amount must be positive; ");
		if (currency == null || currency.length() != 3)
			sb.append("currency must be ISO-3; ");
		if (txnDate == null)
			sb.append("txnDate required; ");
		if (valueDate == null)
			sb.append("valueDate required; ");
		if (referenceNumber == null || referenceNumber.isBlank())
			sb.append("referenceNumber required; ");
		return sb.toString();
	}

	// ------------------------------------------------------------------ //
	// Getters / Setters //
	// ------------------------------------------------------------------ //

	public Long getDebitAccountId() {
		return debitAccountId;
	}

	public void setDebitAccountId(Long id) {
		this.debitAccountId = id;
	}

	public Long getCreditAccountId() {
		return creditAccountId;
	}

	public void setCreditAccountId(Long id) {
		this.creditAccountId = id;
	}

	public BigDecimal getAmount() {
		return amount;
	}

	public void setAmount(BigDecimal amount) {
		this.amount = amount;
	}

	public String getCurrency() {
		return currency;
	}

	public void setCurrency(String currency) {
		this.currency = currency;
	}

	public LocalDateTime getTxnDate() {
		return txnDate;
	}

	public void setTxnDate(LocalDateTime txnDate) {
		this.txnDate = txnDate;
	}

	public LocalDate getValueDate() {
		return valueDate;
	}

	public void setValueDate(LocalDate valueDate) {
		this.valueDate = valueDate;
	}

	public TransactionStatus getStatus() {
		return status;
	}

	public void setStatus(TransactionStatus status) {
		this.status = status;
	}

	public String getReferenceNumber() {
		return referenceNumber;
	}

	public void setReferenceNumber(String ref) {
		this.referenceNumber = ref;
	}
}
