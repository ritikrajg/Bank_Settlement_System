package com.iispl.adapter;

import com.iispl.entity.IncomingTransaction;
import com.iispl.enums.SourceType;

/**
 * Strategy interface — one implementation per source system.
 * Adding a new source = one new class + one registry entry.
 */
public interface TransactionAdapter {
    IncomingTransaction adapt(String rawPayload) throws AdapterException;
    SourceType getSourceType();
}

