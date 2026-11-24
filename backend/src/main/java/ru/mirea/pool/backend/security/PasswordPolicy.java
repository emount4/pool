package ru.mirea.pool.backend.security;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;

public final class PasswordPolicy {

    private PasswordPolicy() {
    }

    public static void validate(String password) {
        if (password.length() < 8
                || password.getBytes(StandardCharsets.UTF_8).length > 72
                || password.codePoints().noneMatch(Character::isLetter)
                || password.codePoints().noneMatch(Character::isDigit)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Пароль должен содержать минимум 8 символов, букву и цифру и занимать до 72 байт.");
        }
    }
}
