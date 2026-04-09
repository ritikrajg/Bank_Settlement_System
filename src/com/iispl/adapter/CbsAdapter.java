package com.iispl.adapter;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.iispl.entity.IncomingTransaction;
import com.iispl.enums.SourceType;

public class CbsAdapter implements TransactionAdapter {

    @Override
    public SourceType getSourceType() {
        return SourceType.CBS;
    }

    @Override
    public IncomingTransaction adapt(String raw) throws AdapterException {

        if (raw == null || raw.isBlank()) {
            throw new AdapterException("CBS", "Blank payload");
        }

        String[] parts = raw.trim().split("\\|");

        if (parts.length < 8) { // 8 fields are required
            throw new AdapterException("CBS", "Expected 8 fields, got " + parts.length);
        }

        try {
            // Mapping based on your CSV structure
            String txnId = parts[0].trim();
            String sourceSystemStr = parts[1].trim(); // should be CBS
            String sourceBank = parts[2].trim();
            String destinationBank = parts[3].trim();
            String fromAccount = parts[4].trim();
            String toAccount = parts[5].trim();
            BigDecimal amount = new BigDecimal(parts[6].trim());
            LocalDate valueDate = LocalDate.parse(parts[7].trim());

            IncomingTransaction transaction = new IncomingTransaction(
                    txnId,
                    SourceType.CBS,
                    sourceBank,
                    destinationBank,
                    fromAccount,
                    toAccount,
                    amount,
                    valueDate
            );

            if (!transaction.isValid()) {
                throw new AdapterException("CBS", "Validation failed for txnId=" + txnId);
            }

            return transaction;

        } catch (AdapterException e) {
            throw e;
        } catch (Exception e) {
            throw new AdapterException("CBS", "Parse error: " + e.getMessage(), e);
        }
    }
}