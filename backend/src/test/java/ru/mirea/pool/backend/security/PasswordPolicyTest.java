package ru.mirea.pool.backend.security;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PasswordPolicyTest {

    @Test
    void validatesCompositionAndBcryptByteLimit() {
        assertDoesNotThrow(() -> PasswordPolicy.validate("Пароль123"));
        assertThrows(ResponseStatusException.class, () -> PasswordPolicy.validate("Password"));
        assertThrows(ResponseStatusException.class, () -> PasswordPolicy.validate("A123456"));
        assertThrows(ResponseStatusException.class, () -> PasswordPolicy.validate("Я".repeat(36) + "1"));
    }
}
