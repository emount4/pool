package ru.mirea.pool.backend;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import ru.mirea.pool.backend.domain.Visit;
import ru.mirea.pool.backend.domain.VisitPriority;
import ru.mirea.pool.backend.domain.VisitStatus;
import ru.mirea.pool.backend.filter.VisitFilter;
import ru.mirea.pool.backend.repository.VisitRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
@EnabledIfEnvironmentVariable(named = "POOL_TEST_DB_ENABLED", matches = "true")
class VisitFilterTest {

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private VisitRepository visits;

    @Test
    void combinesSearchTagPriorityAndDurationFilters() {
        String marker = UUID.randomUUID().toString();
        String phone = "799" + String.format("%08d", Math.floorMod(UUID.randomUUID().getLeastSignificantBits(), 100000000L));
        Long client = jdbc.queryForObject("""
                INSERT INTO clients(first_name, last_name, phone, birth_date)
                VALUES ('Иван', 'Проверочный', ?, '1990-01-01') RETURNING id
                """, Long.class, phone);
        LocalDate date = LocalDate.now().plusDays(10);
        UUID expected = jdbc.queryForObject("""
                INSERT INTO visits(client_id, lane_number, visit_date, start_time, duration_minutes,
                    status, title, individual)
                VALUES (?, 1, ?, '09:00', 90, 'PLANNED', ?, TRUE) RETURNING public_id
                """, UUID.class, client, date, marker);
        jdbc.update("""
                INSERT INTO visits(client_id, lane_number, visit_date, start_time, duration_minutes,
                    status, title, individual)
                VALUES (?, 2, ?, '12:00', 30, 'PLANNED', ?, FALSE)
                """, client, date, marker);
        jdbc.update("INSERT INTO visit_tags(visit_id, tag) SELECT id, 'занятие' FROM visits WHERE public_id = ?", expected);

        VisitFilter filter = new VisitFilter(marker, 1, VisitStatus.PLANNED, true, VisitPriority.HIGH,
                false, date, date, 60, VisitFilter.Comparison.GT, "занятие");
        assertEquals(List.of(expected), visits.findAll(filter.specification()).stream().map(Visit::getPublicId).toList());

        VisitFilter tooLong = new VisitFilter(marker, null, null, null, null,
                null, null, null, 120, VisitFilter.Comparison.GT, null);
        assertTrue(visits.findAll(tooLong.specification()).isEmpty());
    }
}
