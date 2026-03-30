package com.iispl.entity;

public interface Validatable {

    /**
     * Returns true when all required fields are present and consistent.
     */
    boolean isValid();

    /**
     * Returns a human-readable description of validation failures,
     * or an empty string when the object is valid.
     */
    String validationErrors();
}
