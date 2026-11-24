package ru.mirea.pool.backend.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.mirea.pool.backend.domain.Visit;
import ru.mirea.pool.backend.domain.VisitStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VisitRepository extends JpaRepository<Visit, Long>, JpaSpecificationExecutor<Visit> {

    @EntityGraph(attributePaths = {"client", "lane"})
    Optional<Visit> findByPublicId(UUID publicId);

    Optional<Visit> findByVisitNumber(String visitNumber);

    @EntityGraph(attributePaths = {"client", "lane"})
    List<Visit> findByVisitDateAndStatusNot(LocalDate visitDate, VisitStatus status);

    String DATE_RANGE = """
            (cast(:fromDate as LocalDate) is null or v.visitDate >= :fromDate)
            and (cast(:toDate as LocalDate) is null or v.visitDate <= :toDate)
            """;

    @Query("""
            select count(v) as total,
                coalesce(sum(case when v.archived = true then 1 else 0 end), 0) as archived,
                coalesce(sum(case when v.individual = true then 1 else 0 end), 0) as individual,
                coalesce(sum(case when v.individual = true and v.durationMinutes >= :highDuration
                    then 1 else 0 end), 0) as high,
                coalesce(avg(v.durationMinutes), 0.0) as averageDuration
            from Visit v where
            """ + DATE_RANGE)
    VisitCounts summarize(@Param("fromDate") LocalDate from, @Param("toDate") LocalDate to,
                          @Param("highDuration") int highDuration);

    @Query("select v.status as status, count(v) as amount from Visit v where "
            + DATE_RANGE + " group by v.status order by v.status")
    List<StatusCount> countByStatus(@Param("fromDate") LocalDate from, @Param("toDate") LocalDate to);

    interface VisitCounts {
        long getTotal();
        long getArchived();
        long getIndividual();
        long getHigh();
        double getAverageDuration();
    }

    interface StatusCount {
        VisitStatus getStatus();
        long getAmount();
    }

    String START_AT = "cast(concat(cast(v.visitDate as String), ' ', cast(v.startTime as String)) as LocalDateTime)";
    String END_AT = START_AT + " + (v.durationMinutes * 1 minute)";

    @Modifying
    @Query("update Visit v set v.status = case when (" + END_AT + ") <= :now "
            + "then :completed else :inProgress end "
            + "where (v.status = :planned and " + START_AT + " <= :now) "
            + "or (v.status = :inProgress and (" + END_AT + ") <= :now)")
    int refreshStatuses(@Param("now") LocalDateTime now, @Param("planned") VisitStatus planned,
                        @Param("inProgress") VisitStatus inProgress, @Param("completed") VisitStatus completed);
}