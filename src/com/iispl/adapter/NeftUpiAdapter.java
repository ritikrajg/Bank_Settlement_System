package com.iispl.adapter;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.iispl.entity.IncomingTransaction;
import com.iispl.enums.SourceType;
import com.iispl.enums.TransactionType;

public class NeftUpiAdapter implements TransactionAdapter {
    private final SourceType sourceType;

    public NeftUpiAdapter(SourceType sourceType) {
        if (sourceType != SourceType.NEFT && sourceType != SourceType.UPI) {
            throw new IllegalArgumentException("NeftUpiAdapter supports NEFT or UPI only");
        }
        this.sourceType = sourceType;
    }

    @Override
    public SourceType getSourceType() { return sourceType; }

    @Override
    public IncomingTransaction adapt(String raw) throws AdapterException {
        if (raw == null || raw.isBlank()) {
            throw new AdapterException(sourceType.name(), "Blank payload");
        }

        String[] parts = raw.trim().split("\t");
        if (parts.length < 10) {
            throw new AdapterException(sourceType.name(), "Expected 10 tab fields, got " + parts.length);
        }

        try {
            IncomingTransaction transaction = new IncomingTransaction();
            transaction.setSourceRef(parts[1].trim());
            transaction.setSourceSystem(sourceType);
            transaction.setSourceBank(parts[2].trim());
            transaction.setDestinationBank(parts[3].trim());
            transaction.setFromAccount(parts[4].trim());
            transaction.setToAccount(parts[5].trim());
            transaction.setAmount(new BigDecimal(parts[6].trim()));
            transaction.setCurrency(parts[7].trim().toUpperCase());
            transaction.setTxnType(TransactionType.valueOf(parts[8].trim().toUpperCase()));
            transaction.setValueDate(LocalDate.parse(parts[9].trim()));
            transaction.setRawPayload(raw);

            if (!transaction.isValid()) {
                throw new AdapterException(sourceType.name(), "Validation failed ref=" + transaction.getSourceRef());
            }
            return transaction;
        } catch (AdapterException e) {
            throw e;
        } catch (Exception e) {
            throw new AdapterException(sourceType.name(), "Parse error: " + e.getMessage(), e);
        }
    }
}
