package ru.mirea.pool.infrastructure.security;

import ru.mirea.pool.application.security.PasswordHasher;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

public final class Pbkdf2PasswordHasher implements PasswordHasher {

    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int ITERATIONS = 120_000;
    private static final int SALT_LENGTH_BYTES = 16;
    private static final int HASH_LENGTH_BITS = 256;

    private final SecureRandom secureRandom;

    public Pbkdf2PasswordHasher() {
        this.secureRandom = new SecureRandom();
    }

    @Override
    public String hash(char[] password) {
        if (password == null) {
            throw new IllegalArgumentException("Пароль не может быть null.");
        }

        byte[] salt = new byte[SALT_LENGTH_BYTES];
        secureRandom.nextBytes(salt);
        byte[] derivedHash = derive(password, salt, ITERATIONS, HASH_LENGTH_BITS);

        return ITERATIONS
                + ":" + Base64.getEncoder().encodeToString(salt)
                + ":" + Base64.getEncoder().encodeToString(derivedHash);
    }

    @Override
    public boolean matches(char[] password, String storedHash) {
        if (password == null || storedHash == null) {
            return false;
        }

        try {
            String[] parts = storedHash.split(":", -1);
            if (parts.length != 3) {
                return false;
            }

            int iterations = Integer.parseInt(parts[0]);
            byte[] salt = Base64.getDecoder().decode(parts[1]);
            byte[] expectedHash = Base64.getDecoder().decode(parts[2]);
            if (iterations <= 0 || salt.length == 0 || expectedHash.length == 0) {
                return false;
            }

            byte[] actualHash = derive(
                    password,
                    salt,
                    iterations,
                    expectedHash.length * Byte.SIZE
            );
            return MessageDigest.isEqual(expectedHash, actualHash);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private byte[] derive(char[] password, byte[] salt, int iterations, int keyLengthBits) {
        PBEKeySpec keySpec = new PBEKeySpec(password, salt, iterations, keyLengthBits);
        try {
            return SecretKeyFactory.getInstance(ALGORITHM)
                    .generateSecret(keySpec)
                    .getEncoded();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("PBKDF2 недоступен в текущем JDK.", e);
        } finally {
            keySpec.clearPassword();
        }
    }
}
