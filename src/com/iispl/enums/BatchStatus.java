package com.iispl.enums;

public enum BatchStatus {
    INITIATED,     // Batch created
    PROCESSING,    // Settlement running
    PARTIAL,
    COMPLETED,     // Success
    FAILED         // Failure
}