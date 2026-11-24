package ru.mirea.pool.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.mirea.pool.backend.domain.Lane;

import java.time.LocalDate;
import java.util.List;

public interface LaneRepository extends JpaRepository<Lane, Integer> {
    @Query("""
            select l.name as label, count(v.id) as amount
            from Lane l left join Visit v on v.lane = l
                and (cast(:fromDate as LocalDate) is null or v.visitDate >= :fromDate)
                and (cast(:toDate as LocalDate) is null or v.visitDate <= :toDate)
            group by l.number, l.name order by l.number
            """)
    List<LaneCount> countVisitsByLane(@Param("fromDate") LocalDate from, @Param("toDate") LocalDate to);

    interface LaneCount {
        String getLabel();
        long getAmount();
    }
}
