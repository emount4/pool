package ru.mirea.pool.backend.dto;

import ru.mirea.pool.backend.domain.Client;
import ru.mirea.pool.backend.domain.Visit;
import ru.mirea.pool.backend.domain.VisitPriority;
import ru.mirea.pool.backend.domain.VisitStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;
import java.util.UUID;

public record VisitResponse(
        UUID id,
        String visitNumber,
        long clientId,
        String clientName,
        int laneNumber,
        String laneName,
        LocalDate visitDate,
        LocalTime startTime,
        int durationMinutes,
        VisitStatus status,
        String title,
        String description,
        boolean individual,
        boolean archived,
        VisitPriority priority,
        long version,
        Instant createdAt,
        Instant updatedAt,
        Set<String> tags
) {

    public static VisitResponse from(Visit visit) {
        Client client = visit.getClient();
        return new VisitResponse(
                visit.getPublicId(),
                visit.getVisitNumber(),
                client.getId(),
                client.getLastName() + " " + client.getFirstName(),
                visit.getLane().getNumber(),
                visit.getLane().getName(),
                visit.getVisitDate(),
                visit.getStartTime(),
                visit.getDurationMinutes(),
                visit.getStatus(),
                visit.getTitle(),
                visit.getDescription(),
                visit.isIndividual(),
                visit.isArchived(),
                visit.getPriority(),
                visit.getVersion(),
                visit.getCreatedAt(),
                visit.getUpdatedAt(),
                visit.getTags()
        );
    }
}
