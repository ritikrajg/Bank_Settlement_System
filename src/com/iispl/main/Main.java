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

import com.iispl.adapter.AdapterRegistry;
import com.iispl.adapter.CbsAdapter;
import com.iispl.adapter.NeftUpiAdapter;
import com.iispl.adapter.RtgsAdapter;
import com.iispl.adapter.SwiftAdapter;
import com.iispl.dao.TransactionDao;
import com.iispl.enums.SourceType;
import com.iispl.services.PipelineOrchestrator;

public class Main {

	public static void main(String[] args) throws Exception{
		LocalDate settlementDate=LocalDate.now();
		printBanner(settlementDate);
	    AdapterRegistry registry=new AdapterRegistry();
	    registry.register(new CbsAdapter());
        registry.register(new RtgsAdapter());
        registry.register(new SwiftAdapter());
        registry.register(new NeftUpiAdapter(SourceType.NEFT));
        registry.register(new NeftUpiAdapter(SourceType.UPI));
        
        Map<SourceType, List<String>> payloads = new LinkedHashMap<>();
        payloads.put(SourceType.CBS, readLines("data/cbs.txt"));
        payloads.put(SourceType.RTGS, readLines("data/rtgs.csv"));
        payloads.put(SourceType.NEFT, readLines("data/neft.txt"));
        payloads.put(SourceType.UPI, readLines("data/upi.txt"));
        payloads.put(SourceType.SWIFT, readSwiftMessages("data/swift.txt"));
        
        int totalPayloads = payloads.values().stream().mapToInt(List::size).sum();
        System.out.printf("Loaded %d payloads from %d source systems.%n",
                totalPayloads, payloads.size());
        TransactionDao txnDao=new TransactionDao();
        PipelineOrchestrator pipelineOrchestrator=new PipelineOrchestrator(registry, txnDao);
        pipelineOrchestrator.runPipeline(payloads);
	}
	
	private static void printBanner(LocalDate settlementDate) {
        System.out.println("============================================================");
        System.out.println("BANK SETTLEMENT SYSTEM");
        System.out.println("Settlement Date : " + settlementDate);
        System.out.println("Mode            : Demo Run");
        System.out.println("============================================================");
    }
	
	private static List<String> readSwiftMessages(String path) throws IOException {
        String content = Files.readString(Path.of(path), StandardCharsets.UTF_8).trim();
        if (content.isEmpty()) {
            return List.of();
        }

        List<String> messages = new ArrayList<>();
        for (String block : content.split("\\R\\s*\\R")) {
            String message = block.trim();
            if (!message.isEmpty()) {
                messages.add(message);
            }
        }
        return messages;
    }
	
	private static List<String> readLines(String path) throws IOException {
        return Files.readAllLines(Path.of(path), StandardCharsets.UTF_8).stream()
                .map(String::trim)
                .filter(line -> !line.isEmpty())
                .toList();
    }
}