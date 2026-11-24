package ru.mirea.pool.backend.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.security.access.prepost.PreAuthorize;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ru.mirea.pool.backend.service.NotesService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/visits/{visitId}/notes")
@SecurityRequirement(name = "bearerAuth")
public class NotesController {

    private final NotesService notes;

    public NotesController(NotesService notes) {
        this.notes = notes;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('VIEWER', 'OPERATOR', 'ADMIN')")
    public List<NotesService.NoteResponse> list(@PathVariable UUID visitId) {
        return notes.list(visitId);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public NotesService.NoteResponse create(@PathVariable UUID visitId, @AuthenticationPrincipal Jwt jwt,
                                            @Valid @RequestBody NoteRequest request) {
        return notes.create(visitId, UUID.fromString(jwt.getSubject()), request.content(), request.pinned());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")
    public NotesService.NoteResponse update(@PathVariable UUID visitId, @PathVariable UUID id,
                                            @Valid @RequestBody NoteUpdate request) {
        return notes.update(visitId, id, request.content(), request.pinned(), request.version());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID visitId, @PathVariable UUID id) {
        notes.delete(visitId, id);
    }

    public record NoteRequest(@NotBlank @Size(max = 4000) String content, boolean pinned) {
    }

    public record NoteUpdate(@NotBlank @Size(max = 4000) String content, boolean pinned,
                             @NotNull @PositiveOrZero Long version) {
    }
}
