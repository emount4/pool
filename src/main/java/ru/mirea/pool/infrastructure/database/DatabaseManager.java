package ru.mirea.pool.infrastructure.database;

import ru.mirea.pool.infrastructure.exception.DatabaseException;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;

public final class DatabaseManager {

    private static final String CONFIGURATION_FILE = "database.properties";

    private final String url;
    private final String username;
    private final String password;
    private final String schema;

    public DatabaseManager() {
        this(loadProperties());
    }

    public DatabaseManager(Properties properties) {
        this.url = requiredValue(properties, "db.url", "DB_URL");
        this.username = requiredValue(properties, "db.username", "DB_USERNAME");
        this.password = requiredValue(properties, "db.password", "DB_PASSWORD");
        this.schema = properties.getProperty("db.schema", "").trim();
    }

    public Connection getConnection() {
        try {
            Properties connectionProperties = new Properties();
            connectionProperties.setProperty("user", username);
            connectionProperties.setProperty("password", password);
            connectionProperties.setProperty("connectTimeout", "5");
            connectionProperties.setProperty("socketTimeout", "20");
            connectionProperties.setProperty("ApplicationName", "Pool Desktop");
            Connection connection = DriverManager.getConnection(url, connectionProperties);
            if (!schema.isBlank()) {
                try { connection.setSchema(schema); }
                catch (SQLException e) { connection.close(); throw e; }
            }
            return connection;
        } catch (SQLException e) {
            throw new DatabaseException("Не удалось подключиться к базе данных.", e);
        }
    }

    private static Properties loadProperties() {
        Properties properties = new Properties();
        ClassLoader classLoader = DatabaseManager.class.getClassLoader();

        try (InputStream input = classLoader.getResourceAsStream(CONFIGURATION_FILE)) {
            if (input == null) {
                throw new DatabaseException(
                        "Файл настроек " + CONFIGURATION_FILE + " не найден."
                );
            }
            properties.load(new java.io.InputStreamReader(input, StandardCharsets.UTF_8));
            Path external = Path.of(System.getProperty("pool.config", CONFIGURATION_FILE));
            if (Files.exists(external)) {
                try (var reader = Files.newBufferedReader(external, StandardCharsets.UTF_8)) {
                    properties.load(reader);
                }
            } else if (System.getProperty("pool.config") != null) {
                throw new DatabaseException("Указанный файл настроек не найден: " + external);
            }
            return properties;
        } catch (IOException e) {
            throw new DatabaseException("Не удалось прочитать настройки базы данных.", e);
        }
    }

    private static String requiredValue(Properties properties, String key, String environmentKey) {
        String environmentValue = System.getenv(environmentKey);
        String value = environmentValue == null || environmentValue.isBlank()
                ? properties.getProperty(key)
                : environmentValue;
        if (value == null || value.isBlank()) {
            throw new DatabaseException(
                    "Не задан параметр " + key + " или переменная " + environmentKey + "."
            );
        }
        return "db.password".equals(key) ? value : value.trim();
    }
}
