package ru.mirea.pool.backend.filter;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import ru.mirea.pool.backend.domain.Visit;
import ru.mirea.pool.backend.domain.VisitPriority;
import ru.mirea.pool.backend.domain.VisitStatus;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public record VisitFilter(
        String query, Integer lane, VisitStatus status, Boolean individual, VisitPriority priority,
        Boolean archived, LocalDate from, LocalDate to, Integer duration, Comparison comparison, String tag
) {

    public Specification<Visit> specification() {
        if (from != null && to != null && from.isAfter(to)) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST, "Начальная дата позже конечной.");
        }
        return (visit, criteria, builder) -> {
            List<Predicate> conditions = new ArrayList<>();
            if (tag != null && !tag.isBlank()) {
                conditions.add(builder.isMember(tag.strip(), visit.get("tags")));
            }
            if (query != null && !query.isBlank()) {
                String search = "%" + escape(query.strip().toLowerCase(Locale.ROOT)) + "%";
                var client = visit.join("client");
                conditions.add(builder.or(
                        builder.like(builder.lower(visit.get("title")), search, '\\'),
                        builder.like(builder.lower(visit.get("visitNumber")), search, '\\'),
                        builder.like(builder.lower(client.get("firstName")), search, '\\'),
                        builder.like(builder.lower(client.get("lastName")), search, '\\'),
                        builder.like(client.get("phone"), search, '\\')));
            }
            if (lane != null) {
                conditions.add(builder.equal(visit.get("lane").get("number"), lane));
            }
            if (status != null) {
                conditions.add(builder.equal(visit.get("status"), status));
            }
            if (individual != null) {
                conditions.add(builder.equal(visit.get("individual"), individual));
            }
            if (archived != null) {
                conditions.add(builder.equal(visit.get("archived"), archived));
            }
            if (from != null) {
                conditions.add(builder.greaterThanOrEqualTo(visit.get("visitDate"), from));
            }
            if (to != null) {
                conditions.add(builder.lessThanOrEqualTo(visit.get("visitDate"), to));
            }
            if (duration != null) {
                Comparison operator = comparison == null ? Comparison.EQ : comparison;
                conditions.add(switch (operator) {
                    case EQ -> builder.equal(visit.get("durationMinutes"), duration);
                    case LT -> builder.lt(visit.get("durationMinutes"), duration);
                    case LTE -> builder.le(visit.get("durationMinutes"), duration);
                    case GT -> builder.gt(visit.get("durationMinutes"), duration);
                    case GTE -> builder.ge(visit.get("durationMinutes"), duration);
                });
            }
            if (priority != null) {
                conditions.add(switch (priority) {
                    case STANDARD -> builder.isFalse(visit.get("individual"));
                    case ELEVATED -> builder.and(builder.isTrue(visit.get("individual")),
                            builder.lt(visit.get("durationMinutes"), VisitPriority.HIGH_PRIORITY_DURATION));
                    case HIGH -> builder.and(builder.isTrue(visit.get("individual")),
                            builder.ge(visit.get("durationMinutes"), VisitPriority.HIGH_PRIORITY_DURATION));
                });
            }
            return builder.and(conditions.toArray(Predicate[]::new));
        };
    }

    private String escape(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    public enum Comparison {
        EQ, LT, LTE, GT, GTE
    }
}
