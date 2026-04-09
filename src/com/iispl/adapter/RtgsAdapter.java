package com.iispl.adapter;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.iispl.entity.IncomingTransaction;
import com.iispl.enums.SourceType;

public class RtgsAdapter implements TransactionAdapter {

    @Override
    public SourceType getSourceType() {
        return SourceType.RTGS;
    }

    @Override
    public IncomingTransaction adapt(String raw) throws AdapterException {

        if (raw == null || raw.isBlank()) {
            throw new AdapterException("RTGS", "Blank payload");
        }

        String[] parts = raw.trim().split(",");

        if (parts.length < 8) {
            throw new AdapterException("RTGS",
                    "Expected 10 fields, got " + parts.length);
        }

        try {
            String txnId = parts[0].trim();
            String sourceSystemStr = parts[1].trim();
            String sourceBank = parts[2].trim();
            String destinationBank = parts[3].trim();
            String fromAccount = parts[4].trim();
            String toAccount = parts[5].trim();
            BigDecimal amount = new BigDecimal(parts[6].trim());
            LocalDate valueDate = LocalDate.parse(parts[7].trim());


            IncomingTransaction transaction = new IncomingTransaction(
                    txnId,
                    SourceType.RTGS,
                    sourceBank,
                    destinationBank,
                    fromAccount,
                    toAccount,
                    amount,
                    valueDate
            );

            if (!transaction.isValid()) {
                throw new AdapterException("RTGS",
                        "Validation failed txnId=" + txnId);
            }

            return transaction;

        } catch (AdapterException e) {
            throw e;
        } catch (Exception e) {
            throw new AdapterException("RTGS",
                    "Parse error: " + e.getMessage(), e);
        }
    }
}