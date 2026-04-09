package com.iispl.adapter;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.iispl.entity.IncomingTransaction;
import com.iispl.enums.SourceType;

/**
 * Adapter for NEFT source system.
 * NEFT data comes as tab-delimited TXT file.
 * 
 * Format: txnId\tNEFT\tsourceBank\tdestBank\tfromAcc\ttoAcc\tamount\tvalueDate
 * 
 * This adapter reads each tab-separated line and converts it
 * into a common IncomingTransaction object.
 */
public class NeftAdapter implements TransactionAdapter {

    @Override
    public SourceType getSourceType() {
        return SourceType.NEFT;
    }

    @Override
    public IncomingTransaction adapt(String raw) throws AdapterException {

        // Step 1: Check if the line is empty
        if (raw == null || raw.isBlank()) {
            throw new AdapterException("NEFT", "Blank payload");
        }

        // Step 2: Split the line by whitespace (tabs/spaces)
        String[] parts = raw.trim().split("\\s+");

        // Step 3: Make sure we have all 8 fields
        if (parts.length < 8) {
            throw new AdapterException("NEFT", "Expected 8 fields, got " + parts.length);
        }

        try {
            // Step 4: Extract each field from the split parts
            String txnId           = parts[0].trim();   // e.g. NEFT-001
            String sourceSystemStr = parts[1].trim();    // e.g. NEFT
            String sourceBank      = parts[2].trim();    // e.g. SBI
            String destinationBank = parts[3].trim();    // e.g. PNB
            String fromAccount     = parts[4].trim();    // e.g. SBI1001
            String toAccount       = parts[5].trim();    // e.g. PNB4001
            BigDecimal amount      = new BigDecimal(parts[6].trim());  // e.g. 25000.00
            LocalDate valueDate    = LocalDate.parse(parts[7].trim()); // e.g. 2026-03-29

            // Step 5: Create the common IncomingTransaction object
            IncomingTransaction transaction = new IncomingTransaction(
                    txnId,
                    SourceType.NEFT,
                    sourceBank,
                    destinationBank,
                    fromAccount,
                    toAccount,
                    amount,
                    valueDate
            );

            // Step 6: Validate the transaction
            if (!transaction.isValid()) {
                throw new AdapterException("NEFT", "Validation failed for txnId=" + txnId);
            }

            return transaction;

        } catch (AdapterException e) {
            throw e;  // re-throw adapter exceptions as-is
        } catch (Exception e) {
            throw new AdapterException("NEFT", "Parse error: " + e.getMessage(), e);
        }
    }
}
