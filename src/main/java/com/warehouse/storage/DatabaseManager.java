package com.warehouse.storage;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;


public class DatabaseManager {

    private final String CONNECTION_URL = "jdbc:sqlite:warehouse.db";
    private Connection connection;

    public DatabaseManager() throws SQLException {
        connection = DriverManager.getConnection(CONNECTION_URL);
    }

    public Connection getConnection(){
        return connection;
    }

    public void initTables() throws SQLException{

        try (Statement stmt = this.connection.createStatement();){

            stmt.execute("CREATE TABLE IF NOT EXISTS products (" +
                    "productId INTEGER PRIMARY KEY," +
                    "name TEXT NOT NULL," +
                    "threshold INTEGER NOT NULL," +
                    "supplierId INTEGER NOT NULL," +
                    "FOREIGN KEY (supplierId) REFERENCES suppliers(supplierId))");

            stmt.execute("CREATE TABLE IF NOT EXISTS suppliers (" +
                    "supplierId INTEGER PRIMARY KEY," +
                    "name TEXT NOT NULL," +
                    "email TEXT NOT NULL)");

            stmt.execute("CREATE TABLE IF NOT EXISTS bins (" +
                    "binId INTEGER PRIMARY KEY," +
                    "locationCode TEXT NOT NULL)");

            stmt.execute("CREATE TABLE IF NOT EXISTS customers (" +
                    "customerId INTEGER PRIMARY KEY," +
                    "name TEXT NOT NULL," +
                    "email TEXT NOT NULL)");

            stmt.execute("CREATE TABLE IF NOT EXISTS stockItems (" +
                    "quantity INTEGER NOT NULL," +
                    "productId INTEGER NOT NULL," +
                    "binId INTEGER NOT NULL," +
                    "PRIMARY KEY(productId,binId),"+
                    "FOREIGN KEY (productId) REFERENCES products (productId)," +
                    "FOREIGN KEY (binId) REFERENCES bins (binId))");
        }
    }

}
