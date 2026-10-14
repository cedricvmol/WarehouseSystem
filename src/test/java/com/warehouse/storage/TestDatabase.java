package com.warehouse.storage;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
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
        this.databaseManager = newDatabaseManager(url);
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

    /**
     * KAN-90: {@code DatabaseManager(String url)} does not exist yet. Called via
     * reflection so this test code compiles today; replace with
     * {@code new DatabaseManager(url)} once KAN-90 is done.
     */
    static DatabaseManager newDatabaseManager(String url) throws SQLException {
        try {
            Constructor<DatabaseManager> constructor = DatabaseManager.class.getConstructor(String.class);
            return constructor.newInstance(url);
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException("KAN-90: DatabaseManager(String url) is not implemented yet", e);
        } catch (InvocationTargetException e) {
            if (e.getCause() instanceof SQLException sqlException) {
                throw sqlException;
            }
            throw new IllegalStateException(e.getCause());
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
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
