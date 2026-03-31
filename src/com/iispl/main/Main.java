package com.iispl.main;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;

import java.util.List;





public class Main {

	public static void main(String[] args) throws Exception{
		LocalDate settlementDate=LocalDate.now();
		printBanner(settlementDate);
	    
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
