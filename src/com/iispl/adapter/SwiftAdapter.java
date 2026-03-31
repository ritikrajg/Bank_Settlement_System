package com.iispl.adapter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import com.iispl.entity.IncomingTransaction;
import com.iispl.enums.SourceType;
import com.iispl.enums.TransactionType;

public class SwiftAdapter implements TransactionAdapter {
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    @Override
    public SourceType getSourceType() { return SourceType.SWIFT; }

    @Override
    public IncomingTransaction adapt(String raw) throws AdapterException {
        if (raw == null || raw.isBlank()) {
            throw new AdapterException("SWIFT", "Blank payload");
        }

        try {
            String ref = null;
            String srcBank = null;
            String dstBank = null;
            String fromAccount = null;
            String toAccount = null;
            String field32 = null;
            String type = null;

            for (String line : raw.split("\n")) {
                String trimmed = line.trim();
                if (trimmed.startsWith(":20:")) ref = trimmed.substring(4).trim();
                else if (trimmed.startsWith(":50:")) srcBank = trimmed.substring(4).trim();
                else if (trimmed.startsWith(":59:")) dstBank = trimmed.substring(4).trim();
                else if (trimmed.startsWith(":50A:")) fromAccount = trimmed.substring(5).trim();
                else if (trimmed.startsWith(":59A:")) toAccount = trimmed.substring(5).trim();
                else if (trimmed.startsWith(":32A:")) field32 = trimmed.substring(5).trim();
                else if (trimmed.startsWith(":23B:")) type = trimmed.substring(5).trim();
            }

            if (ref == null || srcBank == null || dstBank == null
                    || fromAccount == null || toAccount == null
                    || field32 == null || type == null) {
                throw new AdapterException("SWIFT", "Missing required fields in payload");
            }

            IncomingTransaction transaction = new IncomingTransaction();
            transaction.setSourceRef(ref);
            transaction.setSourceSystem(SourceType.SWIFT);
            transaction.setSourceBank(srcBank);
            transaction.setDestinationBank(dstBank);
            transaction.setFromAccount(fromAccount);
            transaction.setToAccount(toAccount);
            transaction.setValueDate(LocalDate.parse(field32.substring(0, 8), FMT));
            transaction.setCurrency(field32.substring(8, 11).toUpperCase());
            transaction.setAmount(new BigDecimal(field32.substring(11)));
            transaction.setTxnType(TransactionType.valueOf(type.toUpperCase()));
            transaction.setRawPayload(raw);

            if (!transaction.isValid()) {
                throw new AdapterException("SWIFT", "Validation failed ref=" + ref);
            }
            return transaction;
        } catch (AdapterException e) {
            throw e;
        } catch (Exception e) {
            throw new AdapterException("SWIFT", "Parse error: " + e.getMessage(), e);
        }
    }
}
