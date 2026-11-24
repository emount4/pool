package ru.mirea.pool.backend.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import ru.mirea.pool.backend.dto.PageResponse;
import ru.mirea.pool.backend.repository.AuditEventRepository;

import java.time.Instant;
import java.util.UUID;

@RestController
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
public class AuditController {

    private final AuditEventRepository events;

    public AuditController(AuditEventRepository events) {
        this.events = events;
    }

    @GetMapping("/api/admin/audit")
    public PageResponse<AuditRow> list(@RequestParam(defaultValue = "") String query,
                                      @RequestParam(defaultValue = "0") int page,
                                      @RequestParam(defaultValue = "20") int size,
                                      @RequestParam(defaultValue = "false") boolean ascending) {
        if (page < 0 || page > 1_000_000 || size < 1 || size > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Проверьте размер страницы.");
        }
        Sort.Direction direction = ascending ? Sort.Direction.ASC : Sort.Direction.DESC;
        var result = events.search(query, PageRequest.of(page, size,
                Sort.by(new Sort.Order(direction, "occurredAt"), Sort.Order.asc("id"))));
        var rows = result.map(row -> new AuditRow(row.getId(), row.getUsername(), row.getEventType(),
                row.getEntityType(), row.getEntityId(), row.getOccurredAt(), row.getDescription(),
                row.getCorrelationId()));
        return new PageResponse<>(rows.getContent(), page, size, rows.getTotalElements(), rows.getTotalPages());
    }

    public record AuditRow(UUID id, String username, String eventType, String entityType, UUID entityId,
                           Instant occurredAt, String description, String correlationId) {
    }
}
