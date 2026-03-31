package com.iispl.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Provides JDBC connections to the PostgreSQL database.
 *
 * Uses a ThreadLocal so that each worker thread gets its own Connection,
 * eliminating contention without a full connection-pool library.
 *
 * Usage:
 *   Connection conn = DBConnection.getConnection();
 *   // ... use conn ...
 *   DBConnection.close();   // call at end of thread task
 */
public class DBConnection {

    /** One connection per thread. */
    private static final ThreadLocal<Connection> THREAD_LOCAL = new ThreadLocal<>();

    /** Returns the calling thread's connection, opening one if needed. */
    public static Connection getConnection() throws SQLException {
        Connection conn = THREAD_LOCAL.get();
        if (conn == null || conn.isClosed()) {
            DatabaseConfig config = getConfig();
            conn = DriverManager.getConnection(
                config.getUrl(),
                config.getUsername(),
                config.getPassword()
            );
            conn.setAutoCommit(false);          // all writes are explicit-commit
            THREAD_LOCAL.set(conn);
            System.out.println("[DBConnection] Opened connection on thread: "
                               + Thread.currentThread().getName());
        }
        return conn;
    }

    /** Commits and closes the connection for the calling thread. */
    public static void close() {
        Connection conn = THREAD_LOCAL.get();
        if (conn != null) {
            try {
                if (!conn.isClosed()) {
                    conn.commit();
                    conn.close();
                }
            } catch (SQLException e) {
                System.err.println("[DBConnection] Error closing connection: " + e.getMessage());
            } finally {
                THREAD_LOCAL.remove();
            }
        }
    }

    /** Rolls back the current thread's transaction without closing. */
    public static void rollback() {
        Connection conn = THREAD_LOCAL.get();
        if (conn != null) {
            try {
                conn.rollback();
            } catch (SQLException e) {
                System.err.println("[DBConnection] Rollback failed: " + e.getMessage());
            }
        }
    }

    /** Commits the current thread's transaction without closing. */
    public static void commit() throws SQLException {
        Connection conn = THREAD_LOCAL.get();
        if (conn != null && !conn.isClosed()) {
            conn.commit();
        }
    }

    private static DatabaseConfig getConfig() throws SQLException {
        try {
            return DatabaseConfig.getInstance();
        } catch (Throwable t) {
            throw new SQLException("Failed to initialize database configuration", t);
        }
    }

    private DBConnection() { /* static utility */ }
}
