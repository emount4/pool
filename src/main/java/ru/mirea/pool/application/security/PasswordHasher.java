package ru.mirea.pool.application.security;

public interface PasswordHasher {

    String hash(char[] password);

    boolean matches(char[] password, String storedHash);
}
