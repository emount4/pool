package ru.mirea.pool.backend.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.Getter;

import java.io.Serializable;
import java.util.UUID;

@Getter
@Entity
@Table(name = "csv_preview_rows")
@IdClass(CsvPreviewRow.Key.class)
public class CsvPreviewRow {
    @Id
    private UUID previewId;

    @Id
    private int rowNumber;

    private String payload;
    private String error;
    private String result;

    protected CsvPreviewRow() {
    }

    public CsvPreviewRow(UUID previewId, int rowNumber, String payload, String error) {
        this.previewId = previewId;
        this.rowNumber = rowNumber;
        this.payload = payload;
        this.error = error;
    }

    public void markApplied(UUID visitId) {
        result = visitId.toString();
    }

    public record Key(UUID previewId, int rowNumber) implements Serializable {
    }
}
