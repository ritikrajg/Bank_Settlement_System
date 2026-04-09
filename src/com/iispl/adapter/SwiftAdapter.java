package com.iispl.adapter;

import java.io.StringReader;
import java.math.BigDecimal;
import java.time.LocalDate;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import com.iispl.entity.IncomingTransaction;
import com.iispl.enums.SourceType;

/**
 * Adapter for SWIFT source system.
 * SWIFT data comes as an XML file.
 * 
 * Each transaction is passed as a single XML string like:
 * <transaction>
 *     <txnId>SWIFT-001</txnId>
 *     <sourceBank>SBI</sourceBank>
 *     ...
 * </transaction>
 * 
 * This adapter uses Java's built-in XML parser (no external library needed).
 * It converts each XML element into a common IncomingTransaction object.
 */
public class SwiftAdapter implements TransactionAdapter {

    @Override
    public SourceType getSourceType() {
        return SourceType.SWIFT;
    }

    @Override
    public IncomingTransaction adapt(String raw) throws AdapterException {

        // Step 1: Check if the XML string is empty
        if (raw == null || raw.isBlank()) {
            throw new AdapterException("SWIFT", "Blank payload");
        }

        try {
            // Step 2: Parse the XML string into a Document object
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();

            // Wrap the XML string in an InputSource so the parser can read it
            InputSource inputSource = new InputSource(new StringReader(raw));
            Document doc = builder.parse(inputSource);

            // Step 3: Get the root element (which is <transaction>)
            Element root = doc.getDocumentElement();

            // Step 4: Extract each field using the helper method
            String txnId           = getTagValue(root, "txnId");
            String sourceBank      = getTagValue(root, "sourceBank");
            String destinationBank = getTagValue(root, "destinationBank");
            String fromAccount     = getTagValue(root, "fromAccount");
            String toAccount       = getTagValue(root, "toAccount");
            String amountStr       = getTagValue(root, "amount");
            String valueDateStr    = getTagValue(root, "valueDate");

            // Step 5: Convert amount and date from String to proper types
            BigDecimal amount   = new BigDecimal(amountStr);
            LocalDate valueDate = LocalDate.parse(valueDateStr);

            // Step 6: Create the common IncomingTransaction object
            IncomingTransaction transaction = new IncomingTransaction(
                    txnId,
                    SourceType.SWIFT,
                    sourceBank,
                    destinationBank,
                    fromAccount,
                    toAccount,
                    amount,
                    valueDate
            );

            // Step 7: Validate the transaction
            if (!transaction.isValid()) {
                throw new AdapterException("SWIFT", "Validation failed for txnId=" + txnId);
            }

            return transaction;

        } catch (AdapterException e) {
            throw e;  // re-throw adapter exceptions as-is
        } catch (Exception e) {
            throw new AdapterException("SWIFT", "Parse error: " + e.getMessage(), e);
        }
    }

    /**
     * Simple helper method to get the text content of an XML tag.
     * 
     * Example: for XML = <transaction><txnId>SWIFT-001</txnId>...</transaction>
     *   getTagValue(element, "txnId") returns "SWIFT-001"
     * 
     * How it works:
     *   1. Find all elements with the given tag name
     *   2. Get the first one (index 0)
     *   3. Return its text content
     */
    private String getTagValue(Element parent, String tagName) throws AdapterException {

        NodeList nodeList = parent.getElementsByTagName(tagName);

        if (nodeList.getLength() == 0) {
            throw new AdapterException("SWIFT", "Missing XML tag: <" + tagName + ">");
        }

        return nodeList.item(0).getTextContent().trim();
    }
}