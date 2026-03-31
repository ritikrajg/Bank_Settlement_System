package com.iispl.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Loads database connection settings from db.properties.
 */
public class DatabaseConfig {

    private static final String PROPERTIES_FILE = "db.properties";
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

        if (url == null || username == null || password == null) {
            throw new RuntimeException(
                "db.url, db.username and db.password must all be set in " + PROPERTIES_FILE);
        }

        try {
            Class.forName(this.driver);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("JDBC driver not found: " + this.driver
                + ". Add the PostgreSQL driver JAR to the classpath.", e);
        }
    }

    private Properties loadProperties() {
        Properties props = new Properties();

        try (InputStream is = openPropertiesStream()) {
            if (is == null) {
                throw new RuntimeException("Cannot find " + PROPERTIES_FILE
                    + ". Add it to the runtime classpath or keep it at src/main/resources/db.properties.");
            }
            props.load(is);
            return props;
        } catch (IOException e) {
            throw new RuntimeException("Failed to load " + PROPERTIES_FILE, e);
        }
    }

    private InputStream openPropertiesStream() throws IOException {
        ClassLoader classLoader = getClass().getClassLoader();
        String[] classpathCandidates = {
            PROPERTIES_FILE,
            "main/resources/" + PROPERTIES_FILE
        };

        for (String candidate : classpathCandidates) {
            InputStream stream = classLoader.getResourceAsStream(candidate);
            if (stream != null) {
                return stream;
            }
        }

        Path[] fileCandidates = {
            Path.of("src", "main", "resources", PROPERTIES_FILE),
            Path.of("bin", "main", "resources", PROPERTIES_FILE)
        };

        for (Path candidate : fileCandidates) {
            if (Files.exists(candidate)) {
                return Files.newInputStream(candidate);
            }
        }

        return null;
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
