package com.phantasm.db;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object (DAO) for managing encrypted bytecode BLOB storage and execution audit logs in SQLite.
 * Part of Core Academic Module 5: Collections Framework & Database Connectivity (JDBC).
 */
public class BytecodeRepository {

    private final String dbUrl;

    static {
        try {
            // Explicitly load SQLite JDBC driver
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            System.err.println("[BytecodeRepository] SQLite JDBC Driver not found on classpath: " + e.getMessage());
        }
    }

    public record EncryptedClassRecord(
            String className,
            byte[] iv,
            int payloadLength,
            byte[] checksum,
            byte[] ciphertext
    ) {}

    public BytecodeRepository(String dbPath) {
        this.dbUrl = "jdbc:sqlite:" + dbPath;
    }

    public BytecodeRepository() {
        this("phantasm.db");
    }

    /**
     * Initializes database tables: encrypted_classes and audit_logs.
     */
    public synchronized void initializeTables() throws SQLException {
        String createClassesTable = """
            CREATE TABLE IF NOT EXISTS encrypted_classes (
                class_name TEXT PRIMARY KEY,
                iv BLOB NOT NULL,
                payload_len INTEGER NOT NULL,
                checksum BLOB NOT NULL,
                ciphertext BLOB NOT NULL,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            );
            """;

        String createAuditTable = """
            CREATE TABLE IF NOT EXISTS audit_logs (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                class_name TEXT NOT NULL,
                status TEXT NOT NULL,
                message TEXT NOT NULL,
                timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            );
            """;

        try (Connection conn = DriverManager.getConnection(dbUrl);
             Statement stmt = conn.createStatement()) {
            stmt.execute(createClassesTable);
            stmt.execute(createAuditTable);
        }
    }

    /**
     * Inserts or replaces an encrypted bytecode binary record.
     */
    public void saveEncryptedClass(String className, byte[] iv, int payloadLen, byte[] checksum, byte[] ciphertext) throws SQLException {
        String sql = """
            INSERT INTO encrypted_classes (class_name, iv, payload_len, checksum, ciphertext, updated_at)
            VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
            ON CONFLICT(class_name) DO UPDATE SET
                iv = excluded.iv,
                payload_len = excluded.payload_len,
                checksum = excluded.checksum,
                ciphertext = excluded.ciphertext,
                updated_at = CURRENT_TIMESTAMP;
            """;

        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, className);
            pstmt.setBytes(2, iv);
            pstmt.setInt(3, payloadLen);
            pstmt.setBytes(4, checksum);
            pstmt.setBytes(5, ciphertext);
            pstmt.executeUpdate();
        }
    }

    /**
     * Retrieves an encrypted bytecode record by fully qualified class name.
     */
    public EncryptedClassRecord fetchEncryptedClass(String className) throws SQLException {
        String sql = "SELECT class_name, iv, payload_len, checksum, ciphertext FROM encrypted_classes WHERE class_name = ?";

        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, className);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return new EncryptedClassRecord(
                            rs.getString("class_name"),
                            rs.getBytes("iv"),
                            rs.getInt("payload_len"),
                            rs.getBytes("checksum"),
                            rs.getBytes("ciphertext")
                    );
                }
            }
        }
        return null;
    }

    /**
     * Logs an event to the audit_logs table.
     */
    public void logAuditEvent(String className, String status, String message) {
        String sql = "INSERT INTO audit_logs (class_name, status, message) VALUES (?, ?, ?)";

        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, className);
            pstmt.setString(2, status);
            pstmt.setString(3, message);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[BytecodeRepository] Failed to write audit log: " + e.getMessage());
        }
    }

    /**
     * Retrieves all recorded audit logs.
     */
    public List<String> getAuditLogs() throws SQLException {
        List<String> logs = new ArrayList<>();
        String sql = "SELECT id, class_name, status, message, timestamp FROM audit_logs ORDER BY id ASC";

        try (Connection conn = DriverManager.getConnection(dbUrl);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                logs.add(String.format("[%s] ID:%d | Class: %-30s | Status: %-8s | Message: %s",
                        rs.getString("timestamp"),
                        rs.getInt("id"),
                        rs.getString("class_name"),
                        rs.getString("status"),
                        rs.getString("message")));
            }
        }
        return logs;
    }
}
