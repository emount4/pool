package ru.mirea.pool.backend.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import ru.mirea.pool.backend.domain.UserRole;
import ru.mirea.pool.backend.domain.WebUser;
import ru.mirea.pool.backend.dto.UserResponse;
import ru.mirea.pool.backend.repository.RefreshSessionRepository;
import ru.mirea.pool.backend.repository.WebUserRepository;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Set;
import java.util.UUID;

@Service
public class UserManagementService {

    private final WebUserRepository users;
    private final RefreshSessionRepository sessions;
    private final PasswordEncoder encoder;
    private final AuditService audit;
    private final SecureRandom random = new SecureRandom();

    public UserManagementService(WebUserRepository users, RefreshSessionRepository sessions,
                                 PasswordEncoder encoder, AuditService audit) {
        this.users = users;
        this.sessions = sessions;
        this.encoder = encoder;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public UserPage list(int page, int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Размер страницы должен быть от 1 до 100.");
        }
        Page<UserResponse> result = users.findAll(PageRequest.of(page, size, Sort.by("username", "id")))
                .map(UserResponse::from);
        return new UserPage(result.getContent(), result.getNumber(), result.getSize(),
                result.getTotalElements(), result.getTotalPages());
    }

    @Transactional
    public TemporaryPassword create(String username, String displayName, Set<UserRole> roles) {
        String login = validateUsername(username);
        if (users.findByUsernameIgnoreCase(login).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Этот логин уже используется.");
        }
        String password = temporaryPassword();
        WebUser user = users.saveAndFlush(new WebUser(login, displayName.strip(), encoder.encode(password), roles));
        audit.record("USER_CREATED", "USER", user.getId(), "Создан пользователь");
        return new TemporaryPassword(UserResponse.from(user), password);
    }

    @Transactional
    public UserResponse update(UUID actor, UUID id, String username, String displayName, Set<UserRole> roles,
                               boolean active) {
        WebUser user = findUser(id);
        if (actor.equals(id) && (!active || !roles.contains(UserRole.ADMIN))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Нельзя отключить свою учётную запись или снять с себя роль администратора.");
        }
        String login = validateUsername(username);
        if (users.findByUsernameIgnoreCase(login).filter(other -> !other.getId().equals(id)).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Этот логин уже используется.");
        }
        user.updateAccess(login, displayName.strip(), roles, active);
        audit.record("USER_UPDATED", "USER", id, "Изменён доступ пользователя");
        if (!active) {
            sessions.revokeAllForUser(id, Instant.now());
        }
        return UserResponse.from(user);
    }

    @Transactional
    public TemporaryPassword resetPassword(UUID id) {
        WebUser user = findUser(id);
        String password = temporaryPassword();
        user.setTemporaryPassword(encoder.encode(password));
        audit.record("PASSWORD_RESET", "USER", id, "Выдан временный пароль");
        sessions.revokeAllForUser(id, Instant.now());
        return new TemporaryPassword(UserResponse.from(user), password);
    }

    private WebUser findUser(UUID id) {
        return users.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Пользователь не найден."));
    }

    private String validateUsername(String username) {
        String login = username.strip();
        if (login.length() < 3) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Логин должен содержать не менее 3 символов.");
        }
        return login;
    }

    private String temporaryPassword() {
        byte[] bytes = new byte[18];
        random.nextBytes(bytes);
        return "A1" + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public record TemporaryPassword(UserResponse user, String temporaryPassword) {
    }

    public record UserPage(java.util.List<UserResponse> items, int page, int size, long totalItems, int totalPages) {
    }
}
