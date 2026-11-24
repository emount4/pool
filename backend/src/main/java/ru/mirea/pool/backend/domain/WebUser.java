package ru.mirea.pool.backend.domain;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import lombok.Getter;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Getter
@Entity
@Table(name = "web_users")
public class WebUser {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false, length = 50)
    private String username;

    @Column(nullable = false, length = 100)
    private String displayName;

    @Column(nullable = false, length = 60)
    private String passwordHash;

    @Column(nullable = false)
    private boolean active;

    @Column(nullable = false)
    private boolean passwordChangeRequired;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "web_user_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private Set<UserRole> roles = new HashSet<>();

    protected WebUser() {
    }

    public WebUser(String username, String displayName, String passwordHash, Set<UserRole> roles) {
        this.username = username;
        this.displayName = displayName;
        this.roles.addAll(roles);
        this.active = true;
        setTemporaryPassword(passwordHash);
    }

    public Set<UserRole> getRoles() {
        return Set.copyOf(roles);
    }

    public void changePassword(String passwordHash) {
        this.passwordHash = passwordHash;
        this.passwordChangeRequired = false;
    }

    public void setTemporaryPassword(String passwordHash) {
        this.passwordHash = passwordHash;
        this.passwordChangeRequired = true;
    }

    public void updateAccess(String username, String displayName, Set<UserRole> roles, boolean active) {
        this.username = username;
        this.displayName = displayName;
        this.roles.clear();
        this.roles.addAll(roles);
        this.active = active;
    }
}
