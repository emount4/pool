package ru.mirea.pool.backend.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ru.mirea.pool.backend.dto.PageResponse;
import ru.mirea.pool.backend.dto.VisitRequest;
import ru.mirea.pool.backend.dto.VisitResponse;
import ru.mirea.pool.backend.filter.VisitFilter;
import ru.mirea.pool.backend.service.VisitService;
import ru.mirea.pool.backend.service.VisitSlotService;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/visits")
@SecurityRequirement(name = "bearerAuth")
public class VisitController {

    private final VisitService visits;
    private final VisitSlotService slots;

    public VisitController(VisitService visits, VisitSlotService slots) {
        this.visits = visits;
        this.slots = slots;
    }

    @GetMapping("/available-slots")
    @PreAuthorize("hasAnyRole('VIEWER', 'OPERATOR', 'ADMIN')")
    public List<VisitSlotService.VisitSlot> availableSlots(
            @RequestParam LocalDate date,
            @RequestParam int duration,
            @RequestParam long clientId,
            @RequestParam(required = false) UUID except
    ) {
        return slots.find(date, duration, clientId, except);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('VIEWER', 'OPERATOR', 'ADMIN')")
    public PageResponse<VisitResponse> list(
            @ModelAttribute VisitFilter filter,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "visitDate") String sort,
            @RequestParam(defaultValue = "false") boolean descending
    ) {
        return visits.list(filter, page, size, sort, descending);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('VIEWER', 'OPERATOR', 'ADMIN')")
    public VisitResponse find(@PathVariable UUID id) {
        return visits.find(id);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public VisitResponse create(@Valid @RequestBody VisitRequest request) {
        return visits.create(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")
    public VisitResponse update(@PathVariable UUID id, @Valid @RequestBody VisitRequest request) {
        return visits.update(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void archive(@PathVariable UUID id) {
        visits.archive(id);
    }

    @PostMapping("/{id}/restore")
    @PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")
    public VisitResponse restore(@PathVariable UUID id) {
        return visits.restore(id);
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")
    public VisitResponse cancel(@PathVariable UUID id) {
        return visits.cancel(id);
    }

    @DeleteMapping("/{id}/permanent")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void permanentlyDelete(@PathVariable UUID id) {
        visits.permanentlyDelete(id);
    }
}
