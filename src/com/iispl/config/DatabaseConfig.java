package com.iispl.config;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Loads database connection settings from data/db.properties file.
 */
public class DatabaseConfig {

    private static final String PROPERTIES_FILE = "data/db.properties";
    private static final DatabaseConfig INSTANCE = new DatabaseConfig();

    private final String url;
    private final String username;
    private final String password;
    private final String driver;

    private DatabaseConfig() {
        Properties props = loadProperties();

        this.url = props.getProperty("db.url");
        this.username = props.getProperty("db.username");
        this.password = props.getProperty("db.password");
        this.driver = props.getProperty("db.driver", "org.postgresql.Driver");

        // Check that all required properties are set
        if (url == null || username == null || password == null) {
            throw new RuntimeException(
                "db.url, db.username and db.password must all be set in " + PROPERTIES_FILE);
        }

        // Load the JDBC driver class
        try {
            Class.forName(this.driver);
            System.out.println("Database config loaded: " + url);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("JDBC driver not found: " + this.driver
                + ". Add the PostgreSQL driver JAR to the classpath.", e);
        }
    }

    /**
     * Loads db.properties from the data/ folder.
     */
    private Properties loadProperties() {
        Properties props = new Properties();

        try (InputStream is = new FileInputStream(PROPERTIES_FILE)) {
            props.load(is);
            return props;
        } catch (IOException e) {
            throw new RuntimeException("Failed to load " + PROPERTIES_FILE
                + ". Make sure the file exists in the data/ folder.", e);
        }
    }

    public static DatabaseConfig getInstance() {
        return INSTANCE;
    }

    public String getUrl() {
        return url;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getDriver() {
        return driver;
    }

    @Override
    public String toString() {
        return "DatabaseConfig{url=" + url + ", user=" + username + "}";
    }
}