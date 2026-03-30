package com.iispl.entity;

import java.math.BigDecimal;
import java.util.concurrent.locks.ReentrantLock;

import com.iispl.enums.AccountStatus;
import com.iispl.enums.AccountType;

/**
 * Bank account. Balance mutations are fully thread-safe via an internal
 * ReentrantLock so that concurrent SettlementProcessor threads cannot corrupt
 * the running balance.
 */
public class Account extends BaseEntity implements Validatable {

	private String accountNumber;
	private AccountType accountType;
	private BigDecimal balance;
	private String currency;
	private Long customerId; // FK → Customer
	private Long bankId;
	private AccountStatus status;

	/** Guards all balance read-modify-write operations. */
	private final transient ReentrantLock balanceLock = new ReentrantLock();

	public Account() {
	}

	public Account(String accountNumber, AccountType accountType, BigDecimal balance, String currency, Long customerId,
			Long bankId, AccountStatus status) {
		this.accountNumber = accountNumber;
		this.accountType = accountType;
		this.balance = balance;
		this.currency = currency;
		this.customerId = customerId;
		this.bankId = bankId;
		this.status = status;
	}

	// ------------------------------------------------------------------ //
	// Thread-safe balance operations //
	// ------------------------------------------------------------------ //

	/**
	 * Credits the account (increases balance). Thread-safe: acquires balanceLock
	 * before modifying.
	 *
	 * @throws IllegalArgumentException if amount is not positive
	 * @throws IllegalStateException    if account is not ACTIVE
	 */
	public void credit(BigDecimal amount) {
		validateAmount(amount);
		validateActive();
		balanceLock.lock();
		try {
			balance = balance.add(amount);
		} finally {
			balanceLock.unlock();
		}
	}

	/**
	 * Debits the account (decreases balance). Thread-safe: acquires balanceLock
	 * before modifying.
	 *
	 * @throws IllegalArgumentException if amount is not positive
	 * @throws IllegalStateException    if account is not ACTIVE or has insufficient
	 *                                  funds
	 */
	public void debit(BigDecimal amount) {
		validateAmount(amount);
		validateActive();
		balanceLock.lock();
		try {
			if (balance.compareTo(amount) < 0) {
				throw new IllegalStateException("Insufficient funds in account " + accountNumber + ": available="
						+ balance + ", requested=" + amount);
			}
			balance = balance.subtract(amount);
		} finally {
			balanceLock.unlock();
		}
	}

	/**
	 * Returns a snapshot of the current balance. Thread-safe: acquires balanceLock
	 * before reading.
	 */
	public BigDecimal getBalanceSnapshot() {
		balanceLock.lock();
		try {
			return balance;
		} finally {
			balanceLock.unlock();
		}
	}

	// ------------------------------------------------------------------ //
	// Private helpers //
	// ------------------------------------------------------------------ //

	private void validateAmount(BigDecimal amount) {
		if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
			throw new IllegalArgumentException("Amount must be positive, got: " + amount);
		}
	}

	private void validateActive() {
		if (status != AccountStatus.ACTIVE) {
			throw new IllegalStateException("Account " + accountNumber + " is not ACTIVE (status=" + status + ")");
		}
	}

	// ------------------------------------------------------------------ //
	// Validatable //
	// ------------------------------------------------------------------ //

	@Override
	public boolean isValid() {
		return accountNumber != null && !accountNumber.isBlank() && accountType != null && balance != null
				&& currency != null && currency.length() == 3 && status != null;
	}

	@Override
	public String validationErrors() {
		StringBuilder sb = new StringBuilder();
		if (accountNumber == null || accountNumber.isBlank())
			sb.append("accountNumber required; ");
		if (accountType == null)
			sb.append("accountType required; ");
		if (balance == null)
			sb.append("balance required; ");
		if (currency == null || currency.length() != 3)
			sb.append("currency must be ISO-3; ");
		if (status == null)
			sb.append("status required; ");
		return sb.toString();
	}

	// ------------------------------------------------------------------ //
	// Getters / Setters //
	// ------------------------------------------------------------------ //

	public String getAccountNumber() {
		return accountNumber;
	}

	public void setAccountNumber(String accountNumber) {
		this.accountNumber = accountNumber;
	}

	public AccountType getAccountType() {
		return accountType;
	}

	public void setAccountType(AccountType accountType) {
		this.accountType = accountType;
	}

	public BigDecimal getBalance() {
		return balance;
	}

	public void setBalance(BigDecimal balance) {
		this.balance = balance;
	}

	public String getCurrency() {
		return currency;
	}

	public void setCurrency(String currency) {
		this.currency = currency;
	}

	public Long getCustomerId() {
		return customerId;
	}

	public void setCustomerId(Long customerId) {
		this.customerId = customerId;
	}

	public Long getBankId() {
		return bankId;
	}

	public void setBankId(Long bankId) {
		this.bankId = bankId;
	}

	public AccountStatus getStatus() {
		return status;
	}

	public void setStatus(AccountStatus status) {
		this.status = status;
	}

	@Override
	public String toString() {
		return "Account{id=" + getId() + ", number=" + accountNumber + ", type=" + accountType + ", balance=" + balance
				+ " " + currency + ", status=" + status + "}";
	}
}
