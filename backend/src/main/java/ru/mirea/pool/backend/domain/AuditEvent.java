package ru.mirea.pool.backend.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import org.hibernate.annotations.Generated;

import java.time.Instant;
import java.util.UUID;

@Getter
@Entity
@Table(name = "audit_events")
public class AuditEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id")
    private UUID userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private WebUser user;

    private String eventType;
    private String entityType;
    private UUID entityId;
    private String description;
    private String correlationId;

    @Generated
    @Column(insertable = false, updatable = false)
    private Instant occurredAt;

    protected AuditEvent() {
    }

    public AuditEvent(UUID userId, String eventType, String entityType, UUID entityId,
                      String description, String correlationId) {
        this.userId = userId;
        this.eventType = eventType;
        this.entityType = entityType;
        this.entityId = entityId;
        this.description = description;
        this.correlationId = correlationId;
    }
}
