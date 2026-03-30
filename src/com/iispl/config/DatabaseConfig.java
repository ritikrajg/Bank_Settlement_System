package com.iispl.config;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Loads database connection settings from db.properties on the classpath.
 *
 * Expected keys:
 *   db.url      = jdbc:postgresql://localhost:5432/bank_settlement
 *   db.username = postgres
 *   db.password = secret
 *   db.driver   = org.postgresql.Driver
 */
public class DatabaseConfig {

    private static final String PROPERTIES_FILE = "db.properties";
    private static final DatabaseConfig INSTANCE = new DatabaseConfig();

    private final String url;
    private final String username;
    private final String password;
    private final String driver;

    private DatabaseConfig() {
        Properties props = new Properties();
        try (InputStream is = getClass().getClassLoader()
                                        .getResourceAsStream(PROPERTIES_FILE)) {
            if (is == null) {
                throw new RuntimeException("Cannot find " + PROPERTIES_FILE
                    + " on classpath. Create src/resources/db.properties.");
            }
            props.load(is);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load " + PROPERTIES_FILE, e);
        }

        this.url      = props.getProperty("db.url");
        this.username = props.getProperty("db.username");
        this.password = props.getProperty("db.password");
        this.driver   = props.getProperty("db.driver", "org.postgresql.Driver");

        if (url == null || username == null || password == null) {
            throw new RuntimeException(
                "db.url, db.username and db.password must all be set in " + PROPERTIES_FILE);
        }

        // Load driver class
        try {
            Class.forName(this.driver);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("JDBC driver not found: " + this.driver
                + ". Add the PostgreSQL driver JAR to the classpath.", e);
        }
    }

    public static DatabaseConfig getInstance() {
        return INSTANCE;
    }

    public String getUrl()      { return url; }
    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public String getDriver()   { return driver; }

    @Override
    public String toString() {
        return "DatabaseConfig{url=" + url + ", user=" + username + "}";
    }
}
