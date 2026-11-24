package ru.mirea.pool.backend.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ru.mirea.pool.backend.service.LaneService;

import java.util.List;

@RestController
@RequestMapping("/api/lanes")
@SecurityRequirement(name = "bearerAuth")
public class LaneController {

    private final LaneService lanes;

    public LaneController(LaneService lanes) {
        this.lanes = lanes;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('VIEWER', 'OPERATOR', 'ADMIN')")
    public List<LaneService.LaneResponse> list() {
        return lanes.list();
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public LaneService.LaneResponse create(@Valid @RequestBody LaneRequest request) {
        return lanes.create(request.name(), request.active());
    }

    @PutMapping("/{number}")
    @PreAuthorize("hasRole('ADMIN')")
    public LaneService.LaneResponse update(@PathVariable int number, @Valid @RequestBody LaneRequest request) {
        return lanes.update(number, request.name(), request.active());
    }

    public record LaneRequest(@NotBlank @Size(max = 100) String name, @NotNull Boolean active) {
    }
}
