package ru.mirea.pool.application.auth;

import ru.mirea.pool.application.security.PasswordHasher;
import ru.mirea.pool.application.security.PasswordPolicy;
import ru.mirea.pool.domain.exception.BusinessRuleException;
import ru.mirea.pool.domain.exception.ValidationException;
import ru.mirea.pool.domain.model.SystemUser;
import ru.mirea.pool.domain.model.UserRole;
import ru.mirea.pool.domain.repository.SystemUserRepository;

public final class AdminBootstrap {

    private final SystemUserRepository userRepository;
    private final PasswordHasher passwordHasher;
    private final PasswordPolicy passwordPolicy;

    public AdminBootstrap(
            SystemUserRepository userRepository,
            PasswordHasher passwordHasher,
            PasswordPolicy passwordPolicy
    ) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
        this.passwordPolicy = passwordPolicy;
    }

    public boolean isBootstrapRequired() {
        return userRepository.findAll().isEmpty();
    }

    public SystemUser createFirstAdmin(String username, char[] password) {
        if (!isBootstrapRequired()) {
            throw new BusinessRuleException("Первый администратор уже создан.");
        }

        String normalizedUsername = validateAndNormalizeUsername(username);
        passwordPolicy.validate(password);

        return userRepository.save(new SystemUser(
                normalizedUsername,
                passwordHasher.hash(password),
                UserRole.ADMIN
        ));
    }

    private String validateAndNormalizeUsername(String username) {
        if (username == null) {
            throw new ValidationException("Username обязателен.");
        }

        String normalized = username.trim();
        if (normalized.length() < 3 || normalized.length() > 50) {
            throw new ValidationException("Username должен содержать от 3 до 50 символов.");
        }
        return normalized;
    }
}
