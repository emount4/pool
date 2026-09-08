package ru.mirea.pool.application.auth;

import ru.mirea.pool.domain.model.UserRole;

import java.util.Objects;

public record UserSession(long userId, String username, UserRole role) {

    public UserSession {
        if (userId <= 0) {
            throw new IllegalArgumentException("ID пользователя должен быть положительным.");
        }
        username = Objects.requireNonNull(username, "username");
        role = Objects.requireNonNull(role, "role");
    }
}
