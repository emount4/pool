package ru.mirea.pool.backend.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.mirea.pool.backend.domain.VisitNote;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VisitNoteRepository extends JpaRepository<VisitNote, UUID> {
    @EntityGraph(attributePaths = "author")
    List<VisitNote> findByVisitIdOrderByPinnedDescCreatedAtDescIdAsc(long visitId);

    @EntityGraph(attributePaths = "author")
    Optional<VisitNote> findByIdAndVisitId(UUID id, long visitId);

    boolean existsByIdAndVisitId(UUID id, long visitId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update VisitNote n set n.content = :content, n.pinned = :pinned,
                n.version = n.version + 1, n.updatedAt = current_timestamp
            where n.id = :id and n.visitId = :visitId and n.version = :version
            """)
    int updateNote(@Param("id") UUID id, @Param("visitId") long visitId, @Param("version") long version,
                   @Param("content") String content, @Param("pinned") boolean pinned);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from VisitNote n where n.id = :id and n.visitId = :visitId")
    int deleteNote(@Param("id") UUID id, @Param("visitId") long visitId);
}
