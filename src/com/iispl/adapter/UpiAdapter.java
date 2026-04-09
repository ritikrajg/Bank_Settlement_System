package com.iispl.adapter;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import com.iispl.entity.IncomingTransaction;
import com.iispl.enums.SourceType;

/**
 * Adapter for UPI source system.
 * UPI data comes as a JSON file.
 * 
 * Each transaction is passed as a single JSON object string like:
 * {"txnId":"UPI-001","sourceSystem":"UPI","sourceBank":"HDFC",...}
 * 
 * This adapter uses the json-simple library (json-simple-1.1.1.jar)
 * to parse the JSON string — much simpler than manual parsing!
 * 
 * It converts each JSON object into a common IncomingTransaction object.
 */
public class UpiAdapter implements TransactionAdapter {

    // Create the JSON parser once — it can be reused
    private final JSONParser parser = new JSONParser();

    @Override
    public SourceType getSourceType() {
        return SourceType.UPI;
    }

    @Override
    public IncomingTransaction adapt(String raw) throws AdapterException {

        // Step 1: Check if the JSON string is empty
        if (raw == null || raw.isBlank()) {
            throw new AdapterException("UPI", "Blank payload");
        }

        try {
            // Step 2: Parse the JSON string into a JSONObject using json-simple
            JSONObject json = (JSONObject) parser.parse(raw);

            // Step 3: Extract each field from the JSON object
            //         json.get("key") returns the value for that key
            String txnId           = (String) json.get("txnId");
            String sourceBank      = (String) json.get("sourceBank");
            String destinationBank = (String) json.get("destinationBank");
            String fromAccount     = (String) json.get("fromAccount");
            String toAccount       = (String) json.get("toAccount");
            String valueDateStr    = (String) json.get("valueDate");

            // Step 4: Handle the amount field
            //         JSON numbers can come as Long or Double from json-simple
            Object amountObj = json.get("amount");
            BigDecimal amount;
            if (amountObj instanceof Double) {
                amount = BigDecimal.valueOf((Double) amountObj);
            } else if (amountObj instanceof Long) {
                amount = BigDecimal.valueOf((Long) amountObj);
            } else {
                amount = new BigDecimal(amountObj.toString());
            }

            // Step 5: Parse the date string into LocalDate
            LocalDate valueDate = LocalDate.parse(valueDateStr);

            // Step 6: Create the common IncomingTransaction object
            IncomingTransaction transaction = new IncomingTransaction(
                    txnId,
                    SourceType.UPI,
                    sourceBank,
                    destinationBank,
                    fromAccount,
                    toAccount,
                    amount,
                    valueDate
            );

            // Step 7: Validate the transaction
            if (!transaction.isValid()) {
                throw new AdapterException("UPI", "Validation failed for txnId=" + txnId);
            }

            return transaction;

        } catch (AdapterException e) {
            throw e;  // re-throw adapter exceptions as-is
        } catch (Exception e) {
            throw new AdapterException("UPI", "Parse error: " + e.getMessage(), e);
        }
    }
}
