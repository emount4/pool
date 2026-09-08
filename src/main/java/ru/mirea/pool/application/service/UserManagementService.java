package ru.mirea.pool.application.service;

import ru.mirea.pool.application.auth.UserSession;
import ru.mirea.pool.application.security.PasswordHasher;
import ru.mirea.pool.application.security.PasswordPolicy;
import ru.mirea.pool.domain.exception.AccessDeniedException;
import ru.mirea.pool.domain.exception.BusinessRuleException;
import ru.mirea.pool.domain.exception.EntityNotFoundException;
import ru.mirea.pool.domain.exception.ValidationException;
import ru.mirea.pool.domain.model.SystemUser;
import ru.mirea.pool.domain.model.UserRole;
import ru.mirea.pool.domain.repository.SystemUserRepository;

import java.util.List;

public final class UserManagementService {

    private final SystemUserRepository userRepository;
    private final PasswordHasher passwordHasher;
    private final PasswordPolicy passwordPolicy;

    public UserManagementService(
            SystemUserRepository userRepository,
            PasswordHasher passwordHasher,
            PasswordPolicy passwordPolicy
    ) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
        this.passwordPolicy = passwordPolicy;
    }

    public List<SystemUser> getAllUsers(UserSession actor) {
        requireAdmin(actor);
        return userRepository.findAll();
    }

    public SystemUser getUserById(UserSession actor, long userId) {
        requireAdmin(actor);
        validateUserId(userId);
        return findRequiredUser(userId);
    }

    public SystemUser createUser(
            UserSession actor,
            String username,
            char[] password,
            UserRole role
    ) {
        requireAdmin(actor);
        String normalizedUsername = validateAndNormalizeUsername(username);
        validateRole(role);

        if (userRepository.existsByUsername(normalizedUsername)) {
            throw new BusinessRuleException("Пользователь с таким username уже существует.");
        }

        passwordPolicy.validate(password);
        SystemUser user = new SystemUser(
                normalizedUsername,
                passwordHasher.hash(password),
                role
        );
        return userRepository.save(user);
    }

    public void changeUsername(
            UserSession actor,
            long targetUserId,
            String newUsername
    ) {
        requireAdmin(actor);
        validateUserId(targetUserId);
        String normalizedUsername = validateAndNormalizeUsername(newUsername);
        SystemUser target = findRequiredUser(targetUserId);

        if (!target.getUsername().equals(normalizedUsername)
                && userRepository.existsByUsername(normalizedUsername)) {
            throw new BusinessRuleException("Пользователь с таким username уже существует.");
        }

        target.setUsername(normalizedUsername);
        userRepository.update(target);
    }

    public void changeRole(
            UserSession actor,
            long targetUserId,
            UserRole newRole
    ) {
        requireAdmin(actor);
        validateUserId(targetUserId);
        validateRole(newRole);
        SystemUser target = findRequiredUser(targetUserId);

        if (isSameUser(actor, target) && newRole != UserRole.ADMIN) {
            throw new BusinessRuleException("Администратор не может снять роль ADMIN с самого себя.");
        }

        if (target.isActive()
                && target.getRole() == UserRole.ADMIN
                && newRole != UserRole.ADMIN) {
            ensureAnotherActiveAdminExists();
        }

        target.setRole(newRole);
        userRepository.update(target);
    }

    public void changePassword(
            UserSession actor,
            long targetUserId,
            char[] newPassword
    ) {
        requireAdmin(actor);
        validateUserId(targetUserId);
        SystemUser target = findRequiredUser(targetUserId);
        passwordPolicy.validate(newPassword);

        target.setPasswordHash(passwordHasher.hash(newPassword));
        userRepository.update(target);
    }

    public void deactivateUser(UserSession actor, long targetUserId) {
        requireAdmin(actor);
        validateUserId(targetUserId);
        SystemUser target = findRequiredUser(targetUserId);

        if (isSameUser(actor, target)) {
            throw new BusinessRuleException("Администратор не может заблокировать самого себя.");
        }

        if (!target.isActive()) {
            return;
        }

        if (target.getRole() == UserRole.ADMIN) {
            ensureAnotherActiveAdminExists();
        }

        target.setActive(false);
        userRepository.update(target);
    }

    public void activateUser(UserSession actor, long targetUserId) {
        requireAdmin(actor);
        validateUserId(targetUserId);
        SystemUser target = findRequiredUser(targetUserId);

        if (target.isActive()) {
            return;
        }

        target.setActive(true);
        userRepository.update(target);
    }

    private void requireAdmin(UserSession actor) {
        if (actor == null || actor.role() != UserRole.ADMIN) {
            throw new AccessDeniedException("Недостаточно прав для выполнения операции.");
        }
    }

    private SystemUser findRequiredUser(long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Системный пользователь с ID " + userId + " не найден."
                ));
    }

    private void ensureAnotherActiveAdminExists() {
        if (userRepository.countActiveAdmins() <= 1) {
            throw new BusinessRuleException(
                    "В системе должен остаться хотя бы один активный администратор."
            );
        }
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

    private void validateRole(UserRole role) {
        if (role == null) {
            throw new ValidationException("Роль пользователя обязательна.");
        }
    }

    private void validateUserId(long userId) {
        if (userId <= 0) {
            throw new ValidationException("ID пользователя должен быть положительным.");
        }
    }

    private boolean isSameUser(UserSession actor, SystemUser target) {
        return target.getId() != null && actor.userId() == target.getId();
    }
}
