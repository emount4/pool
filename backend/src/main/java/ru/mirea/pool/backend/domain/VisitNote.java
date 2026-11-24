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
import jakarta.persistence.Version;
import lombok.Getter;
import org.hibernate.annotations.Generated;

import java.time.Instant;
import java.util.UUID;

@Getter
@Entity
@Table(name = "visit_notes")
public class VisitNote {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private long visitId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private WebUser author;

    private String content;
    private boolean pinned;

    @Generated
    @Column(insertable = false, updatable = false)
    private Instant createdAt;

    @Generated
    @Column(insertable = false, updatable = false)
    private Instant updatedAt;

    @Version
    private long version;

    protected VisitNote() {
    }

    public VisitNote(long visitId, WebUser author, String content, boolean pinned) {
        this.visitId = visitId;
        this.author = author;
        this.content = content;
        this.pinned = pinned;
    }
}
