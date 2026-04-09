package com.iispl.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Provides JDBC connections to the PostgreSQL database.
 *
 * Uses a ThreadLocal so that each worker thread gets its own Connection.
 * This avoids conflicts when multiple threads use the database at the same time.
 *
 * Usage:
 *   Connection conn = DBConnection.getConnection();
 *   // ... use conn ...
 *   DBConnection.commit();   // save changes
 *   DBConnection.close();    // release connection at end of thread
 */
public class DBConnection {

    /** One connection per thread. */
    private static final ThreadLocal<Connection> THREAD_LOCAL = new ThreadLocal<>();

    /**
     * Returns the calling thread's database connection.
     * If no connection exists yet, it creates a new one.
     */
    public static Connection getConnection() throws SQLException {
        Connection conn = THREAD_LOCAL.get();

        if (conn == null || conn.isClosed()) {
            DatabaseConfig config = DatabaseConfig.getInstance();

            conn = DriverManager.getConnection(
                config.getUrl(),
                config.getUsername(),
                config.getPassword()
            );
            conn.setAutoCommit(false);  // we will commit manually
            THREAD_LOCAL.set(conn);
        }

        return conn;
    }

    /**
     * Commits and closes the connection for the calling thread.
     * Call this when the thread is done with its work.
     */
    public static void close() {
        Connection conn = THREAD_LOCAL.get();
        if (conn != null) {
            try {
                if (!conn.isClosed()) {
                    conn.commit();
                    conn.close();
                }
            } catch (SQLException e) {
                System.err.println("Database close error: " + e.getMessage());
            } finally {
                THREAD_LOCAL.remove();
            }
        }
    }

    /**
     * Rolls back the current thread's transaction (undo changes).
     * Call this when something goes wrong.
     */
    public static void rollback() {
        Connection conn = THREAD_LOCAL.get();
        if (conn != null) {
            try {
                conn.rollback();
            } catch (SQLException e) {
                System.err.println("Database rollback error: " + e.getMessage());
            }
        }
    }

    /**
     * Commits the current thread's transaction (save changes).
     */
    public static void commit() throws SQLException {
        Connection conn = THREAD_LOCAL.get();
        if (conn != null && !conn.isClosed()) {
            conn.commit();
        }
    }

    // Private constructor — this class only has static methods
    private DBConnection() { }
}