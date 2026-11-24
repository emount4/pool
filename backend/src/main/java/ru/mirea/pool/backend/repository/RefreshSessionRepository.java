package ru.mirea.pool.backend.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.mirea.pool.backend.domain.RefreshSession;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface RefreshSessionRepository extends JpaRepository<RefreshSession, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<RefreshSession> findByTokenHash(String tokenHash);

    @Query("""
            select session from RefreshSession session
            join fetch session.user account
            left join fetch account.roles
            where session.id = :id
            """)
    Optional<RefreshSession> findForAuthentication(@Param("id") UUID id);

    @Modifying
    @Query("""
            update RefreshSession session
            set session.revokedAt = :now
            where session.user.id = :userId and session.revokedAt is null
            """)
    int revokeAllForUser(@Param("userId") UUID userId, @Param("now") Instant now);
}
