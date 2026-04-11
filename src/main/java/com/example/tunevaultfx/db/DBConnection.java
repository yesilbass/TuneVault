package com.example.tunevaultfx.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class DBConnection {
    private static final String DEFAULT_URL =
            "jdbc:mysql://localhost:3306/tune_vault_db?serverTimezone=America/New_York&useSSL=false&allowPublicKeyRetrieval=true";
    private static final String DEFAULT_USER = "root";
    private static final String DEFAULT_PASSWORD = "mysqlpassword";

    private DBConnection() {}

    public static Connection getConnection() throws SQLException {
        String url = firstNonBlank(System.getenv("TUNEVAULT_DB_URL"), System.getProperty("tunevault.db.url"), DEFAULT_URL);
        String user = firstNonBlank(System.getenv("TUNEVAULT_DB_USER"), System.getProperty("tunevault.db.user"), DEFAULT_USER);
        String password = firstNonBlank(System.getenv("TUNEVAULT_DB_PASSWORD"), System.getProperty("tunevault.db.password"), DEFAULT_PASSWORD);

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new SQLException("MySQL JDBC driver not found.", e);
        }

        return DriverManager.getConnection(url, user, password);
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
    }
}