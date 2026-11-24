package ru.mirea.pool.backend.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import ru.mirea.pool.backend.domain.VisitPriority;
import ru.mirea.pool.backend.repository.LaneRepository;
import ru.mirea.pool.backend.repository.VisitRepository;

import java.time.LocalDate;
import java.util.List;

@Service
public class AnalyticsService {
    private final VisitRepository visits;
    private final LaneRepository lanes;

    public AnalyticsService(VisitRepository visits, LaneRepository lanes) {
        this.visits = visits;
        this.lanes = lanes;
    }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public Summary summary(LocalDate from, LocalDate to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Начальная дата позже конечной.");
        }

        var counts = visits.summarize(from, to, VisitPriority.HIGH_PRIORITY_DURATION);
        Totals totals = new Totals(counts.getTotal(), counts.getTotal() - counts.getArchived(),
                counts.getArchived(), counts.getIndividual(), counts.getAverageDuration());
        List<Group> laneCounts = lanes.countVisitsByLane(from, to).stream()
                .map(row -> new Group(row.getLabel(), row.getAmount())).toList();
        List<Group> statuses = visits.countByStatus(from, to).stream()
                .map(row -> new Group(row.getStatus().name(), row.getAmount())).toList();
        List<Group> priorities = List.of(
                new Group(VisitPriority.ELEVATED.name(), counts.getIndividual() - counts.getHigh()),
                new Group(VisitPriority.HIGH.name(), counts.getHigh()),
                new Group(VisitPriority.STANDARD.name(), counts.getTotal() - counts.getIndividual()))
                .stream().filter(group -> group.count() > 0).toList();
        return new Summary(totals, laneCounts, statuses, priorities);
    }

    public record Totals(long total, long active, long archived, long individual, double averageDuration) {
    }

    public record Group(String label, long count) {
    }

    public record Summary(Totals totals, List<Group> lanes, List<Group> statuses, List<Group> priorities) {
    }
}
