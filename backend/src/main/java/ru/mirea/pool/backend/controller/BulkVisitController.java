package ru.mirea.pool.backend.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.security.access.prepost.PreAuthorize;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import ru.mirea.pool.backend.domain.VisitStatus;
import ru.mirea.pool.backend.service.BulkVisitService;

import java.util.List;
import java.util.UUID;

@RestController
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")
public class BulkVisitController {

    private final BulkVisitService bulk;

    public BulkVisitController(BulkVisitService bulk) {
        this.bulk = bulk;
    }

    @PostMapping("/api/visits/bulk")
    public List<BulkVisitService.Result> apply(@Valid @RequestBody BulkRequest request) {
        return bulk.apply(request.ids(), request.action(), request.status(), request.tag());
    }

    public record BulkRequest(@NotEmpty @Size(max = 100) List<@NotNull UUID> ids,
                              @NotNull BulkVisitService.Action action, VisitStatus status, String tag) {
    }
}
