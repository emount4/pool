package ru.mirea.pool.application.auth;

import ru.mirea.pool.application.security.PasswordHasher;
import ru.mirea.pool.domain.model.SystemUser;
import ru.mirea.pool.domain.repository.SystemUserRepository;

public final class AuthService {

    private static final String INVALID_CREDENTIALS = "Неверный логин или пароль.";

    private final SystemUserRepository userRepository;
    private final PasswordHasher passwordHasher;

    public AuthService(
            SystemUserRepository userRepository,
            PasswordHasher passwordHasher
    ) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
    }

    public UserSession login(String username, char[] password) {
        String normalizedUsername = normalizeUsername(username);
        SystemUser user = userRepository.findByUsername(normalizedUsername)
                .orElseThrow(this::authenticationFailed);

        if (!user.isActive() || !passwordHasher.matches(password, user.getPasswordHash())) {
            throw authenticationFailed();
        }

        return new UserSession(user.getId(), user.getUsername(), user.getRole());
    }

    private String normalizeUsername(String username) {
        return username == null ? "" : username.trim();
    }

    private AuthenticationException authenticationFailed() {
        return new AuthenticationException(INVALID_CREDENTIALS);
    }
}
