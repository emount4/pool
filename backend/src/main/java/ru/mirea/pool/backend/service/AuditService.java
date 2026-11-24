package ru.mirea.pool.backend.service;

import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import ru.mirea.pool.backend.domain.AuditEvent;
import ru.mirea.pool.backend.repository.AuditEventRepository;

import java.util.UUID;

@Service
public class AuditService {

    private final AuditEventRepository events;

    public AuditService(AuditEventRepository events) {
        this.events = events;
    }

    public void record(String event, String entity, UUID id, String description) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UUID userId = null;
        if (authentication != null && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getName())) {
            userId = UUID.fromString(authentication.getName());
        }
        recordForUser(userId, event, entity, id, description);
    }

    public void recordForUser(UUID userId, String event, String entity, UUID id, String description) {
        events.save(new AuditEvent(userId, event, entity, id, description, MDC.get("correlationId")));
    }
}
