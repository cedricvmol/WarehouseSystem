package com.warehouse.storage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Test fixture: one fresh SQLite file per test under {@code target/test-db/}.
 * The file stays after the run so it can be inspected with SQL / DBeaver.
 * Never touches the real {@code warehouse.db}.
 */
final class TestDatabase implements AutoCloseable {

    static final Path DIR = Path.of("target", "test-db");

    final Path file;
    final String url;
    final DatabaseManager databaseManager;
    final Connection connection;

    private TestDatabase(Path file) throws SQLException {
        this.file = file;
        this.url = "jdbc:sqlite:" + file.toString().replace('\\', '/');
        this.databaseManager = new DatabaseManager(url);
        this.databaseManager.initTables();
        this.connection = databaseManager.getConnection();
    }

    /** Empty database with the production schema, named after the test. */
    static TestDatabase fresh(String name) throws SQLException, IOException {
        Files.createDirectories(DIR);
        Path file = DIR.resolve(name.replaceAll("[^A-Za-z0-9_-]", "_") + ".db");
        Files.deleteIfExists(file);
        return new TestDatabase(file);
    }

    /** Runs a statement directly on the database, bypassing the DAOs. */
    void execute(String sql) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    /** Runs a {@code SELECT COUNT(*) ...} (or any single-int query) and returns the value. */
    int queryInt(String sql, Object... params) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new IllegalStateException("No row for: " + sql);
                }
                return rs.getInt(1);
            }
        }
    }

    /** Runs a single-string query, e.g. {@code SELECT orderStatus FROM salesOrders WHERE ...}. */
    String queryString(String sql, Object... params) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new IllegalStateException("No row for: " + sql);
                }
                return rs.getString(1);
            }
        }
    }

    @Override
    public void close() throws SQLException {
        connection.close();
    }
}
