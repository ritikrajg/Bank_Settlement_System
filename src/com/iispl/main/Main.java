package com.iispl.main;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.iispl.adapter.AdapterRegistry;
import com.iispl.adapter.CbsAdapter;
import com.iispl.adapter.NeftAdapter;
import com.iispl.adapter.RtgsAdapter;
import com.iispl.adapter.SwiftAdapter;
import com.iispl.adapter.UpiAdapter;
import com.iispl.dao.AccountDao;
import com.iispl.dao.CustomerDao;

import com.iispl.dao.TransactionDao;
import com.iispl.enums.SourceType;
import com.iispl.services.PipelineOrchestrator;

/**
 * Entry point for the IISPL Bank Settlement System.
 *
 * What this class does (step by step):
 *  1. Print startup banner with the settlement date.
 *  2. Register all channel adapters (CBS, RTGS, SWIFT, NEFT, UPI).
 *  3. Load raw transaction files from the data/ directory.
 *     - CBS  : TXT file  (pipe | delimited)
 *     - RTGS : CSV file  (comma , delimited)
 *     - SWIFT: XML file  (XML tags)
 *     - NEFT : TXT file  (tab delimited)
 *     - UPI  : JSON file (JSON objects)
 *  4. Wire up DAOs and hand control to the PipelineOrchestrator.
 */
public class Main {

    public static void main(String[] args) throws Exception {

        LocalDate settlementDate = LocalDate.now();
        printBanner(settlementDate);

        // ─── Step 1: Register Adapters ───
        // Each adapter knows how to parse its own file format
        AdapterRegistry registry = new AdapterRegistry();
        registry.register(new CbsAdapter());       // parses TXT (pipe-delimited)
        registry.register(new RtgsAdapter());       // parses CSV (comma-delimited)
        registry.register(new SwiftAdapter());      // parses XML
        registry.register(new NeftAdapter());       // parses TXT (tab-delimited)
        registry.register(new UpiAdapter());        // parses JSON

        // ─── Step 2: Load transaction data from 5 different file formats ───
        Map<SourceType, List<String>> payloads = new LinkedHashMap<>();

        payloads.put(SourceType.CBS,   readLines("data/cbs.txt"));              // pipe-delimited TXT
        payloads.put(SourceType.RTGS,  readLines("data/rtgs.csv"));             // comma-separated CSV
        payloads.put(SourceType.SWIFT, readXmlTransactions("data/swift.xml"));   // XML file
        payloads.put(SourceType.NEFT,  readLines("data/neft.txt"));             // tab-delimited TXT
        payloads.put(SourceType.UPI,   readJsonObjects("data/upi.json"));       // JSON file

        // Count total transactions loaded
        int totalTransactions = 0;
        for (List<String> list : payloads.values()) {
            totalTransactions += list.size();
        }

        System.out.printf("Input loaded: %d transactions from %d sources.%n",
                totalTransactions, payloads.size());

        // Print count per source
        for (Map.Entry<SourceType, List<String>> entry : payloads.entrySet()) {
            System.out.printf("  %-6s : %d transactions%n",
                    entry.getKey(), entry.getValue().size());
        }
        System.out.println();

        // ─── Step 3: Create DAO objects ───
        TransactionDao     txnDao      = new TransactionDao();
        
        AccountDao         accountDao  = new AccountDao();
       
        CustomerDao        customerDao = new CustomerDao();

        // ─── Step 4: Run the full pipeline ───
       
    }

    /**
     * Prints the application startup banner.
     */
    private static void printBanner(LocalDate settlementDate) {
        System.out.println("╔══════════════════════════════════════╗");
        System.out.println("║      IISPL Bank Settlement System    ║");
        System.out.println("╚══════════════════════════════════════╝");
        System.out.println("Settlement Date : " + settlementDate);
        System.out.println();
    }

    // ═══════════════════════════════════════════════════════════════════
    //  FILE READING METHODS — one per file format
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Reads a line-delimited TXT or CSV file.
     * Used for: CBS (pipe-delimited), RTGS (comma-delimited), NEFT (tab-delimited)
     *
     * Each non-empty line becomes one raw payload string.
     */
    private static List<String> readLines(String path) throws IOException {

        List<String> lines = Files.readAllLines(Path.of(path), StandardCharsets.UTF_8);
        List<String> result = new ArrayList<>();

        for (String line : lines) {
            String trimmed = line.trim();
            if (!trimmed.isEmpty()) {
                result.add(trimmed);
            }
        }

        return result;
    }

    /**
     * Reads an XML file and extracts each <transaction> element
     * as a separate XML string.
     * Used for: SWIFT
     */
    private static List<String> readXmlTransactions(String path) throws Exception {

        List<String> transactions = new ArrayList<>();

        // Step 1: Parse the XML file
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(Path.of(path).toFile());

        // Step 2: Get all <transaction> elements
        NodeList nodeList = doc.getElementsByTagName("transaction");

        // Step 3: Convert each element back to an XML string
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);

            if (node.getNodeType() == Node.ELEMENT_NODE) {
                Element element = (Element) node;

                // Build XML string for this one transaction
                StringBuilder xml = new StringBuilder();
                xml.append("<transaction>");
                xml.append("<txnId>").append(getTagText(element, "txnId")).append("</txnId>");
                xml.append("<sourceSystem>").append(getTagText(element, "sourceSystem")).append("</sourceSystem>");
                xml.append("<sourceBank>").append(getTagText(element, "sourceBank")).append("</sourceBank>");
                xml.append("<destinationBank>").append(getTagText(element, "destinationBank")).append("</destinationBank>");
                xml.append("<fromAccount>").append(getTagText(element, "fromAccount")).append("</fromAccount>");
                xml.append("<toAccount>").append(getTagText(element, "toAccount")).append("</toAccount>");
                xml.append("<amount>").append(getTagText(element, "amount")).append("</amount>");
                xml.append("<valueDate>").append(getTagText(element, "valueDate")).append("</valueDate>");
                xml.append("</transaction>");

                transactions.add(xml.toString());
            }
        }

        return transactions;
    }

    /**
     * Helper: gets the text content of a child XML tag.
     */
    private static String getTagText(Element parent, String tagName) {
        NodeList list = parent.getElementsByTagName(tagName);
        if (list.getLength() > 0) {
            return list.item(0).getTextContent().trim();
        }
        return "";
    }

    /**
     * Reads a JSON file containing an array of objects.
     * Extracts each { ... } JSON object as a separate string.
     * Used for: UPI
     *
     * How it works:
     *   - Read the entire file
     *   - Walk through characters, find matching { and } pairs
     *   - Each pair = one transaction
     */
    private static List<String> readJsonObjects(String path) throws IOException {

        String content = Files.readString(Path.of(path), StandardCharsets.UTF_8).trim();
        List<String> objects = new ArrayList<>();

        int depth = 0;      // tracks how deep we are inside { }
        int start = -1;     // start index of the current JSON object

        for (int i = 0; i < content.length(); i++) {
            char c = content.charAt(i);

            if (c == '{') {
                if (depth == 0) {
                    start = i;      // beginning of a new JSON object
                }
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0 && start != -1) {
                    // Found a complete { ... } block
                    objects.add(content.substring(start, i + 1));
                    start = -1;
                }
            }
        }

        return objects;
    }
}