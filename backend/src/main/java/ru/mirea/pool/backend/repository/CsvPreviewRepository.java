package ru.mirea.pool.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.mirea.pool.backend.domain.CsvPreview;

import java.util.UUID;

public interface CsvPreviewRepository extends JpaRepository<CsvPreview, UUID> {
    @Query("""
            select count(p) > 0 from CsvPreview p
            where p.id = :id and p.userId = :userId and p.expiresAt > current_timestamp
            """)
    boolean existsValid(@Param("id") UUID id, @Param("userId") UUID userId);
}
