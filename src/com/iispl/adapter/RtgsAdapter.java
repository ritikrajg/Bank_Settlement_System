package com.iispl.adapter;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.iispl.entity.IncomingTransaction;
import com.iispl.enums.SourceType;
import com.iispl.enums.TransactionType;

public class RtgsAdapter implements TransactionAdapter {
    @Override
    public SourceType getSourceType() { return SourceType.RTGS; }

    @Override
    public IncomingTransaction adapt(String raw) throws AdapterException {
        if (raw == null || raw.isBlank()) {
            throw new AdapterException("RTGS", "Blank payload");
        }

        String[] parts = raw.trim().split(",");
        if (parts.length < 10) {
            throw new AdapterException("RTGS", "Expected 10 fields, got " + parts.length);
        }

        try {
            IncomingTransaction transaction = new IncomingTransaction();
            transaction.setSourceRef(parts[1].trim());
            transaction.setSourceSystem(SourceType.RTGS);
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
                throw new AdapterException("RTGS", "Validation failed ref=" + transaction.getSourceRef());
            }
            return transaction;
        } catch (AdapterException e) {
            throw e;
        } catch (Exception e) {
            throw new AdapterException("RTGS", "Parse error: " + e.getMessage(), e);
        }
    }
}
