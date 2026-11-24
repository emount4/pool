package ru.mirea.pool.backend.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.mirea.pool.backend.domain.CsvPreviewRow;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CsvPreviewRowRepository extends JpaRepository<CsvPreviewRow, CsvPreviewRow.Key> {
    @Query("select r.rowNumber from CsvPreviewRow r where r.previewId = :previewId order by r.rowNumber")
    List<Integer> findRowNumbers(@Param("previewId") UUID previewId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<CsvPreviewRow> findByPreviewIdAndRowNumber(UUID previewId, int rowNumber);
}
