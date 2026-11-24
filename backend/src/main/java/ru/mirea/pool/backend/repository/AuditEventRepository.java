package ru.mirea.pool.backend.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.mirea.pool.backend.domain.AuditEvent;

import java.time.Instant;
import java.util.UUID;

public interface AuditEventRepository extends JpaRepository<AuditEvent, UUID> {
    @Query("""
            select a.id as id, u.username as username, a.eventType as eventType,
                a.entityType as entityType, a.entityId as entityId, a.occurredAt as occurredAt,
                a.description as description, a.correlationId as correlationId
            from AuditEvent a left join a.user u
            where locate(lower(:query), lower(concat(a.eventType, ' ', a.description, ' ',
                coalesce(u.username, ''), ' ', a.entityType, ' ', coalesce(cast(a.entityId as String), '')))) > 0
            """)
    Page<AuditEntry> search(@Param("query") String query, Pageable pageable);

    interface AuditEntry {
        UUID getId();
        String getUsername();
        String getEventType();
        String getEntityType();
        UUID getEntityId();
        Instant getOccurredAt();
        String getDescription();
        String getCorrelationId();
    }
}
