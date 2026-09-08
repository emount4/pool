package ru.mirea.pool.application.security;

import ru.mirea.pool.domain.exception.ValidationException;

public final class PasswordPolicy {

    private static final int MINIMUM_LENGTH = 8;

    public void validate(char[] password) {
        if (password == null || password.length < MINIMUM_LENGTH) {
            throw new ValidationException("Пароль должен содержать не менее 8 символов.");
        }

        boolean hasLetter = false;
        boolean hasDigit = false;
        for (char character : password) {
            hasLetter |= Character.isLetter(character);
            hasDigit |= Character.isDigit(character);
        }

        if (!hasLetter || !hasDigit) {
            throw new ValidationException("Пароль должен содержать хотя бы одну букву и одну цифру.");
        }
    }
}
