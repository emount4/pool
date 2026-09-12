package ru.mirea.pool.application.dto;

import ru.mirea.pool.domain.model.VisitStatus;

import java.util.Map;

public record StatisticsDto(
        long totalClients,
        long totalVisits,
        Map<VisitStatus, Long> visitsByStatus,
        long visitsToday,
        double averageDurationMinutes,
        Integer mostPopularLane
) {
    public StatisticsDto {
        visitsByStatus = Map.copyOf(visitsByStatus);
    }
}
