package com.birdsenger.utils;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.SQLException;

public class DatabaseManager {
    private static DatabaseManager instance;
    private HikariDataSource dataSource;

    // Your VPS Configuration
    private static final String DB_HOST = System.getenv().getOrDefault("DB_HOST", "13.229.69.180");
    private static final String DB_PORT = System.getenv().getOrDefault("DB_PORT", "5432");
    private static final String DB_NAME = System.getenv().getOrDefault("DB_NAME", "birdsenger");
    private static final String DB_USER = System.getenv().getOrDefault("DB_USER", "birduser");
    private static final String DB_PASSWORD = System.getenv().getOrDefault("DB_PASSWORD", "BirdSecure2024!");

    private static final String DB_URL = String.format("jdbc:postgresql://%s:%s/%s",
            DB_HOST, DB_PORT, DB_NAME);

    private DatabaseManager() {
        setupConnectionPool();
    }

    public static DatabaseManager getInstance() {
        if (instance == null) {
            synchronized (DatabaseManager.class) {
                if (instance == null) {
                    instance = new DatabaseManager();
                }
            }
        }
        return instance;
    }

    private void setupConnectionPool() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(DB_URL);
        config.setUsername(DB_USER);
        config.setPassword(DB_PASSWORD);
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);

        // PostgreSQL optimizations
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

        dataSource = new HikariDataSource(config);
        System.out.println("✅ Database connection pool initialized: " + DB_URL);
    }

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }

    public boolean testConnection() {
        try (Connection conn = getConnection()) {
            boolean isValid = conn != null && !conn.isClosed();
            System.out.println("🔍 Database connection test: " + (isValid ? "SUCCESS ✅" : "FAILED ❌"));
            return isValid;
        } catch (SQLException e) {
            System.err.println("❌ Database connection failed: " + e.getMessage());
            return false;
        }
    }
}