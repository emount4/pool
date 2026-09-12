package ru.mirea.pool.infrastructure.database;

import ru.mirea.pool.infrastructure.exception.DatabaseException;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public final class DatabaseManager {

    private static final String CONFIGURATION_FILE = "application.properties";

    private final String url;
    private final String username;
    private final String password;

    public DatabaseManager() {
        this(loadProperties());
    }

    public DatabaseManager(Properties properties) {
        this.url = requiredValue(properties, "db.url", "DB_URL");
        this.username = requiredValue(properties, "db.username", "DB_USERNAME");
        this.password = requiredValue(properties, "db.password", "DB_PASSWORD");
    }

    public Connection getConnection() {
        try {
            return DriverManager.getConnection(url, username, password);
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
            properties.load(input);
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
