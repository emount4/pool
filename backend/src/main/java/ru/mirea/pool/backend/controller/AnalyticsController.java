package ru.mirea.pool.backend.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.mirea.pool.backend.service.AnalyticsService;

import java.time.LocalDate;

@RestController
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasAnyRole('VIEWER', 'OPERATOR', 'ADMIN')")
public class AnalyticsController {

    private final AnalyticsService analytics;

    public AnalyticsController(AnalyticsService analytics) {
        this.analytics = analytics;
    }

    @GetMapping("/api/analytics/summary")
    public AnalyticsService.Summary summary(@RequestParam(required = false) LocalDate from,
                                            @RequestParam(required = false) LocalDate to) {
        return analytics.summary(from, to);
    }
}
