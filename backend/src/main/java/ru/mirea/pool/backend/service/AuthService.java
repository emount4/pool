package ru.mirea.pool.backend.service;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import ru.mirea.pool.backend.config.SecurityConfig;
import ru.mirea.pool.backend.domain.RefreshSession;
import ru.mirea.pool.backend.domain.WebUser;
import ru.mirea.pool.backend.domain.UserRole;
import ru.mirea.pool.backend.dto.UserResponse;
import ru.mirea.pool.backend.repository.RefreshSessionRepository;
import ru.mirea.pool.backend.repository.WebUserRepository;
import ru.mirea.pool.backend.security.LoginAttempts;
import ru.mirea.pool.backend.security.PasswordPolicy;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;
import java.util.Set;

@Service
public class AuthService {

    private static final Duration ACCESS_LIFETIME = Duration.ofMinutes(15);
    private static final Duration REFRESH_LIFETIME = Duration.ofDays(14);

    private final WebUserRepository users;
    private final RefreshSessionRepository sessions;
    private final PasswordEncoder passwords;
    private final JwtEncoder encoder;
    private final LoginAttempts attempts;
    private final AuditService audit;
    private final SecureRandom random = new SecureRandom();
    private final String dummyHash;

    public AuthService(WebUserRepository users, RefreshSessionRepository sessions,
                       PasswordEncoder passwords, JwtEncoder encoder, LoginAttempts attempts, AuditService audit) {
        this.users = users;
        this.sessions = sessions;
        this.passwords = passwords;
        this.encoder = encoder;
        this.attempts = attempts;
        this.audit = audit;
        this.dummyHash = passwords.encode(UUID.randomUUID().toString());
    }

    @Transactional(noRollbackFor = ResponseStatusException.class)
    public Tokens login(String username, String password, String address) {
        String login = username.strip();
        attempts.register(login, address);
        WebUser user = users.findByUsernameIgnoreCase(login).orElse(null);
        String hash = user == null ? dummyHash : user.getPasswordHash();
        if (!matches(password, hash) || user == null || !user.isActive()) {
            audit.recordForUser(user == null ? null : user.getId(), "LOGIN_FAILED", "USER",
                    user == null ? null : user.getId(), "Неудачная попытка входа");
            throw unauthorized("Неверный логин или пароль.");
        }
        attempts.clear(login, address);
        audit.recordForUser(user.getId(), "LOGIN", "USER", user.getId(), "Вход в систему");
        return startSession(user);
    }

    @Transactional
    public Tokens register(String username, String displayName, String password, String passwordConfirmation) {
        String login = username.strip();
        if (login.length() < 3) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Логин должен содержать не менее 3 символов.");
        }
        if (!password.equals(passwordConfirmation)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Подтверждение пароля не совпадает.");
        }
        PasswordPolicy.validate(password);
        if (users.findByUsernameIgnoreCase(login).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Этот логин уже используется.");
        }
        WebUser user = new WebUser(login, displayName.strip(), passwords.encode(password), Set.of(UserRole.VIEWER));
        user.changePassword(user.getPasswordHash());
        users.saveAndFlush(user);
        audit.recordForUser(user.getId(), "USER_REGISTERED", "USER", user.getId(), "Регистрация пользователя");
        return startSession(user);
    }

    @Transactional
    public Tokens refresh(String token) {
        Instant now = Instant.now();
        RefreshSession session = sessions.findByTokenHash(hash(token))
                .filter(value -> value.isUsableAt(now) && value.getUser().isActive())
                .orElseThrow(() -> unauthorized("Сессия завершена."));
        String replacement = nextToken();
        session.rotateToken(hash(replacement));
        return issue(session, replacement, now);
    }

    @Transactional
    public void logout(String token) {
        sessions.findByTokenHash(hash(token)).ifPresent(session -> {
            session.revoke(Instant.now());
            audit.recordForUser(session.getUser().getId(), "LOGOUT", "USER",
                    session.getUser().getId(), "Выход из системы");
        });
    }

    @Transactional(readOnly = true)
    public UserResponse current(UUID userId) {
        return UserResponse.from(findUser(userId));
    }

    @Transactional
    public Tokens changePassword(UUID userId, String oldPassword, String newPassword) {
        WebUser user = findUser(userId);
        if (!matches(oldPassword, user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Текущий пароль указан неверно.");
        }
        PasswordPolicy.validate(newPassword);
        if (matches(newPassword, user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Выберите другой пароль.");
        }
        user.changePassword(passwords.encode(newPassword));
        audit.recordForUser(userId, "PASSWORD_CHANGED", "USER", userId, "Пароль изменён");
        sessions.revokeAllForUser(userId, Instant.now());
        return startSession(user);
    }

    private WebUser findUser(UUID userId) {
        return users.findById(userId).filter(WebUser::isActive)
                .orElseThrow(() -> unauthorized("Учётная запись недоступна."));
    }

    private Tokens startSession(WebUser user) {
        Instant now = Instant.now();
        String token = nextToken();
        RefreshSession session = sessions.save(new RefreshSession(user, hash(token), now, now.plus(REFRESH_LIFETIME)));
        return issue(session, token, now);
    }

    private Tokens issue(RefreshSession session, String refreshToken, Instant now) {
        WebUser user = session.getUser();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(SecurityConfig.ISSUER)
                .subject(user.getId().toString())
                .issuedAt(now)
                .expiresAt(now.plus(ACCESS_LIFETIME))
                .id(UUID.randomUUID().toString())
                .claim("sid", session.getId().toString())
                .build();
        String accessToken = encoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
        return new Tokens(accessToken, refreshToken, ACCESS_LIFETIME.toSeconds(), UserResponse.from(user));
    }

    private boolean matches(String password, String hash) {
        return password.getBytes(StandardCharsets.UTF_8).length <= 72 && passwords.matches(password, hash);
    }

    private String nextToken() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private ResponseStatusException unauthorized(String message) {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, message);
    }

    public record Tokens(String accessToken, String refreshToken, long expiresIn, UserResponse user) {
    }
}
