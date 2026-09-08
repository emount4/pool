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
        this.url = requiredProperty(properties, "db.url");
        this.username = requiredProperty(properties, "db.username");
        this.password = requiredProperty(properties, "db.password");
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

    private static String requiredProperty(Properties properties, String key) {
        String value = properties.getProperty(key);
        if (value == null || value.isBlank()) {
            throw new DatabaseException("Не задано обязательное свойство: " + key);
        }
        return value.trim();
    }
}
