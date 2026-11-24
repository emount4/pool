package ru.mirea.pool.backend.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "csv_previews")
public class CsvPreview {
    @Id
    private UUID id;
    private UUID userId;

    private String mode;

    private Instant expiresAt;

    protected CsvPreview() {
    }

    public CsvPreview(UUID id, UUID userId, String mode, Instant expiresAt) {
        this.id = id;
        this.userId = userId;
        this.mode = mode;
        this.expiresAt = expiresAt;
    }
}
