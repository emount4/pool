package ru.mirea.pool.backend.dto;

import ru.mirea.pool.backend.domain.UserRole;
import ru.mirea.pool.backend.domain.WebUser;

import java.util.Set;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String username,
        String displayName,
        Set<UserRole> roles,
        boolean active,
        boolean passwordChangeRequired
) {

    public static UserResponse from(WebUser user) {
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getDisplayName(),
                user.getRoles(),
                user.isActive(),
                user.isPasswordChangeRequired()
        );
    }
}
