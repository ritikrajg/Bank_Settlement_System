package com.iispl.entity;

import java.math.BigDecimal;
import java.util.concurrent.locks.ReentrantLock;

/**
 * In-memory NPCI settlement account for a member bank.
 *
 * During the netting phase, a net-debit bank deposits its payable amount into
 * this account. Balance mutations are guarded by a ReentrantLock so the object
 * stays safe even if the phase is parallelized later.
 */
public class NpciAccount extends BaseEntity {

    private final String bankCode;
    private BigDecimal balance;
    private final transient ReentrantLock balanceLock = new ReentrantLock();

    public NpciAccount(String bankCode) {
        this.bankCode = bankCode;
        this.balance = BigDecimal.ZERO;
    }

    public void credit(BigDecimal amount) {
        validateAmount(amount);
        balanceLock.lock();
        try {
            balance = balance.add(amount);
        } finally {
            balanceLock.unlock();
        }
    }

    public void debit(BigDecimal amount) {
        validateAmount(amount);
        balanceLock.lock();
        try {
            if (balance.compareTo(amount) < 0) {
                throw new IllegalStateException(
                        "Insufficient NPCI balance for bank " + bankCode
                                + ": available=" + balance + ", requested=" + amount);
            }
            balance = balance.subtract(amount);
        } finally {
            balanceLock.unlock();
        }
    }

    public BigDecimal getBalanceSnapshot() {
        balanceLock.lock();
        try {
            return balance;
        } finally {
            balanceLock.unlock();
        }
    }

    public String getBankCode() {
        return bankCode;
    }

    private void validateAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive, got: " + amount);
        }
    }

    @Override
    public String toString() {
        return "NpciAccount{bankCode=" + bankCode + ", balance=" + getBalanceSnapshot() + "}";
    }
}
