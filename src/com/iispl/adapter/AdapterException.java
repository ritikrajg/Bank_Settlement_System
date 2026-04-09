package com.iispl.adapter;

public class AdapterException extends Exception {
    
	private final String source;
    public AdapterException(String source, String message) {
        super("[" + source + "] " + message);
        this.source = source;
    }
    public AdapterException(String source, String message, Throwable cause) {
        super("[" + source + "] " + message, cause);
        this.source = source;
    }
    // hi vishnu
    public String getSource() { return source; }
}
