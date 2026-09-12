package ru.mirea.pool.application.service;

import ru.mirea.pool.application.dto.StatisticsDto;
import ru.mirea.pool.domain.model.Visit;
import ru.mirea.pool.domain.model.VisitStatus;
import ru.mirea.pool.domain.repository.ClientRepository;
import ru.mirea.pool.domain.repository.VisitRepository;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class StatisticsService {

    private final ClientRepository clientRepository;
    private final VisitRepository visitRepository;

    public StatisticsService(
            ClientRepository clientRepository,
            VisitRepository visitRepository
    ) {
        this.clientRepository = clientRepository;
        this.visitRepository = visitRepository;
    }

    public StatisticsDto getStatistics() {
        List<Visit> visits = visitRepository.findAll();
        Map<VisitStatus, Long> statusCounts = countByStatus(visits);

        long visitsToday = visits.stream()
                .filter(visit -> LocalDate.now().equals(visit.getVisitDate()))
                .count();

        double averageDuration = visits.stream()
                .mapToInt(Visit::getDurationMinutes)
                .average()
                .orElse(0.0);

        Integer mostPopularLane = visits.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        Visit::getLaneNumber,
                        java.util.stream.Collectors.counting()
                ))
                .entrySet()
                .stream()
                .max(Map.Entry.<Integer, Long>comparingByValue()
                        .thenComparing(Map.Entry.comparingByKey(Comparator.reverseOrder())))
                .map(Map.Entry::getKey)
                .orElse(null);

        return new StatisticsDto(
                clientRepository.findAll().size(),
                visits.size(),
                statusCounts,
                visitsToday,
                averageDuration,
                mostPopularLane
        );
    }

    private Map<VisitStatus, Long> countByStatus(List<Visit> visits) {
        Map<VisitStatus, Long> counts = new EnumMap<>(VisitStatus.class);
        for (VisitStatus status : VisitStatus.values()) {
            counts.put(status, 0L);
        }
        visits.forEach(visit -> counts.merge(visit.getStatus(), 1L, Long::sum));
        return counts;
    }
}
