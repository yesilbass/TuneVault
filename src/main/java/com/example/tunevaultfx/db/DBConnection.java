package com.example.tunevaultfx.db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Provides pooled database connections via HikariCP.
 *
 * <p>Before running the app, create the {@code tune_vault_db} database and tables
 * by applying {@code /db/schema.sql} from the classpath (source:
 * {@code src/main/resources/db/schema.sql}) with the MySQL client.</p>
 *
 * Benefits over the old DriverManager approach:
 *  - Connections are kept open and reused — no TCP handshake on every query.
 *  - Pool size of 10 means up to 10 concurrent queries without waiting.
 *  - Connection validation (SELECT 1) ensures stale connections are
 *    replaced automatically.
 *
 * Supports environment variable / system property overrides so the same
 * binary can be used in dev and production without recompiling.
 */
public final class DBConnection {

    private static final String DEFAULT_URL =
            "jdbc:mysql://localhost:3306/tune_vault_db" +
                    "?serverTimezone=America/New_York" +
                    "&useSSL=false" +
                    "&allowPublicKeyRetrieval=true" +
                    "&rewriteBatchedStatements=true" +
                    "&cachePrepStmts=true" +
                    "&prepStmtCacheSize=250" +
                    "&prepStmtCacheSqlLimit=2048";

    private static final String DEFAULT_USER     = "root";
    private static final String DEFAULT_PASSWORD = "mysqlpassword";

    private static final HikariDataSource dataSource;

    static {
        String url = firstNonBlank(
                System.getenv("TUNEVAULT_DB_URL"),
                System.getProperty("tunevault.db.url"),
                DEFAULT_URL);

        String user = firstNonBlank(
                System.getenv("TUNEVAULT_DB_USER"),
                System.getProperty("tunevault.db.user"),
                DEFAULT_USER);

        String password = firstNonBlank(
                System.getenv("TUNEVAULT_DB_PASSWORD"),
                System.getProperty("tunevault.db.password"),
                DEFAULT_PASSWORD);

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(url);
        config.setUsername(user);
        config.setPassword(password);
        config.setDriverClassName("com.mysql.cj.jdbc.Driver");

        // Pool sizing — 10 max is plenty for a desktop app
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);

        // How long to wait for a connection from the pool (ms)
        config.setConnectionTimeout(5_000);

        // How long an idle connection stays alive (ms)
        config.setIdleTimeout(300_000);

        // Max lifetime of any connection — forces refresh before server drops it
        config.setMaxLifetime(600_000);

        // Lightweight ping to validate connections before handing them out
        config.setConnectionTestQuery("SELECT 1");

        // Pool name shows up in logs / JMX
        config.setPoolName("TuneVaultPool");

        dataSource = new HikariDataSource(config);
    }

    private DBConnection() {}

    /**
     * Returns a live connection from the pool.
     * Callers MUST close the connection (try-with-resources) to return it
     * to the pool — not to the database.
     */
    public static Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    /**
     * Shuts the pool down cleanly.
     * Call this from your Application.stop() override.
     */
    public static void shutdown() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }

    private static String firstNonBlank(String... values) {
        for (String v : values) {
            if (v != null && !v.isBlank()) return v;
        }
        return "";
    }
}