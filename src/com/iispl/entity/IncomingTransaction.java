package com.iispl.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.iispl.enums.ProcessingStatus;
import com.iispl.enums.TransactionType;

/**
 * Canonical representation of every inbound transaction regardless of origin.
 * Adapters normalise raw source payloads into this form before queuing.
 */
public class IncomingTransaction extends BaseEntity implements Validatable {

	private SourceSystem sourceSystem; // HAS-A
	private String sourceRef; // reference from originating system
	private String rawPayload; // original wire payload (XML / JSON / flat)
	private String normalizedPayload; // adapter-produced canonical JSON
	private TransactionType txnType;
	private BigDecimal amount;
	private String currency; // ISO-4217
	private LocalDate valueDate;
	private ProcessingStatus processingStatus;
	private LocalDateTime ingestTimestamp;

	public IncomingTransaction() {
	}

	// ------------------------------------------------------------------ //
	// Validatable //
	// ------------------------------------------------------------------ //

	@Override
	public boolean isValid() {
		return sourceSystem != null && sourceRef != null && !sourceRef.isBlank() && txnType != null && amount != null
				&& amount.compareTo(BigDecimal.ZERO) > 0 && currency != null && currency.length() == 3
				&& valueDate != null;
	}

	@Override
	public String validationErrors() {
		StringBuilder sb = new StringBuilder();
		if (sourceSystem == null)
			sb.append("sourceSystem required; ");
		if (sourceRef == null || sourceRef.isBlank())
			sb.append("sourceRef required; ");
		if (txnType == null)
			sb.append("txnType required; ");
		if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0)
			sb.append("amount must be positive; ");
		if (currency == null || currency.length() != 3)
			sb.append("currency must be 3-char ISO code; ");
		if (valueDate == null)
			sb.append("valueDate required; ");
		return sb.toString();
	}

	// ------------------------------------------------------------------ //
	// Getters / Setters //
	// ------------------------------------------------------------------ //

	public SourceSystem getSourceSystem() {
		return sourceSystem;
	}

	public void setSourceSystem(SourceSystem s) {
		this.sourceSystem = s;
	}

	public String getSourceRef() {
		return sourceRef;
	}

	public void setSourceRef(String sourceRef) {
		this.sourceRef = sourceRef;
	}

	public String getRawPayload() {
		return rawPayload;
	}

	public void setRawPayload(String rawPayload) {
		this.rawPayload = rawPayload;
	}

	public String getNormalizedPayload() {
		return normalizedPayload;
	}

	public void setNormalizedPayload(String np) {
		this.normalizedPayload = np;
	}

	public TransactionType getTxnType() {
		return txnType;
	}

	public void setTxnType(TransactionType txnType) {
		this.txnType = txnType;
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

	public LocalDate getValueDate() {
		return valueDate;
	}

	public void setValueDate(LocalDate valueDate) {
		this.valueDate = valueDate;
	}

	public ProcessingStatus getProcessingStatus() {
		return processingStatus;
	}

	public void setProcessingStatus(ProcessingStatus ps) {
		this.processingStatus = ps;
	}

	public LocalDateTime getIngestTimestamp() {
		return ingestTimestamp;
	}

	public void setIngestTimestamp(LocalDateTime ts) {
		this.ingestTimestamp = ts;
	}

	@Override
	public String toString() {
		return "IncomingTransaction{id=" + getId() + ", ref=" + sourceRef + ", type=" + txnType + ", amount=" + amount
				+ " " + currency + ", status=" + processingStatus + "}";
	}
}
