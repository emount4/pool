package ru.mirea.pool.domain.model;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Objects;

@Getter
@Setter
public class SystemUser {

    private Long id;
    private String username;
    private String passwordHash;
    private UserRole role;
    private boolean active;
    private LocalDateTime createdAt;

    public SystemUser() {
    }

    public SystemUser(String username, String passwordHash, UserRole role) {
        this(null, username, passwordHash, role, true, LocalDateTime.now());
    }

    public SystemUser(
            Long id,
            String username,
            String passwordHash,
            UserRole role,
            boolean active,
            LocalDateTime createdAt
    ) {
        this.id = id;
        this.username = username;
        this.passwordHash = passwordHash;
        this.role = role;
        this.active = active;
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "SystemUser{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", role=" + role +
                ", active=" + active +
                ", createdAt=" + createdAt +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof SystemUser that)) {
            return false;
        }
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
