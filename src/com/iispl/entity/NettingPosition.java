package com.iispl.entity;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.iispl.enums.NetDirection;

/**
 * Net bilateral position between this bank and a counterparty for one currency.
 * The NettingEngine accumulates gross amounts under a ReentrantLock and then
 * derives netAmount + direction before the settlement instruction is raised.
 */
public class NettingPosition extends BaseEntity {

	private Long counterpartyBankId;
	private String currency;
	private BigDecimal grossDebitAmount;
	private BigDecimal grossCreditAmount;
	private BigDecimal netAmount;
	private NetDirection direction;
	private LocalDate positionDate;

	public NettingPosition() {
		this.grossDebitAmount = BigDecimal.ZERO;
		this.grossCreditAmount = BigDecimal.ZERO;
		this.netAmount = BigDecimal.ZERO;
		this.direction = NetDirection.FLAT;
	}

	public NettingPosition(Long counterpartyBankId, String currency, LocalDate positionDate) {
		this();
		this.counterpartyBankId = counterpartyBankId;
		this.currency = currency;
		this.positionDate = positionDate;
	}

	/**
	 * Recalculates netAmount and direction from current gross amounts. Must be
	 * called after every accumulation.
	 */
	public void recalculate() {
		int cmp = grossCreditAmount.compareTo(grossDebitAmount);
		if (cmp > 0) {
			netAmount = grossCreditAmount.subtract(grossDebitAmount);
			direction = NetDirection.NET_CREDIT;
		} else if (cmp < 0) {
			netAmount = grossDebitAmount.subtract(grossCreditAmount);
			direction = NetDirection.NET_DEBIT;
		} else {
			netAmount = BigDecimal.ZERO;
			direction = NetDirection.FLAT;
		}
	}

	// Getters / Setters
	public Long getCounterpartyBankId() {
		return counterpartyBankId;
	}

	public void setCounterpartyBankId(Long counterpartyBankId) {
		this.counterpartyBankId = counterpartyBankId;
	}

	public String getCurrency() {
		return currency;
	}

	public void setCurrency(String currency) {
		this.currency = currency;
	}

	public BigDecimal getGrossDebitAmount() {
		return grossDebitAmount;
	}

	public void setGrossDebitAmount(BigDecimal a) {
		this.grossDebitAmount = a;
	}

	public BigDecimal getGrossCreditAmount() {
		return grossCreditAmount;
	}

	public void setGrossCreditAmount(BigDecimal a) {
		this.grossCreditAmount = a;
	}

	public BigDecimal getNetAmount() {
		return netAmount;
	}

	public void setNetAmount(BigDecimal netAmount) {
		this.netAmount = netAmount;
	}

	public NetDirection getDirection() {
		return direction;
	}

	public void setDirection(NetDirection direction) {
		this.direction = direction;
	}

	public LocalDate getPositionDate() {
		return positionDate;
	}

	public void setPositionDate(LocalDate positionDate) {
		this.positionDate = positionDate;
	}

	@Override
	public String toString() {
		return "NettingPosition{counterparty=" + counterpartyBankId + ", ccy=" + currency + ", net=" + netAmount + " "
				+ direction + ", date=" + positionDate + "}";
	}
}
