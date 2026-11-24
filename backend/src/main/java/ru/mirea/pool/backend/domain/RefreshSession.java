package ru.mirea.pool.backend.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "refresh_sessions")
public class RefreshSession {

    @Id
    @GeneratedValue
    @Getter
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    @Getter
    private WebUser user;

    @Column(nullable = false, length = 64, unique = true)
    private String tokenHash;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    @Getter
    private Instant expiresAt;

    private Instant revokedAt;

    protected RefreshSession() {
    }

    public RefreshSession(WebUser user, String tokenHash, Instant createdAt, Instant expiresAt) {
        this.user = user;
        this.tokenHash = tokenHash;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }

    public void rotateToken(String tokenHash) {
        this.tokenHash = tokenHash;
    }

    public boolean isUsableAt(Instant now) {
        return revokedAt == null && expiresAt.isAfter(now);
    }

    public void revoke(Instant now) {
        if (revokedAt == null) {
            revokedAt = now;
        }
    }
}
