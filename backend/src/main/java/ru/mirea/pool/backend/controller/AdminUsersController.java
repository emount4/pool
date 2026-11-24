package ru.mirea.pool.backend.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.mirea.pool.backend.domain.UserRole;
import ru.mirea.pool.backend.dto.UserResponse;
import ru.mirea.pool.backend.service.UserManagementService;

import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/users")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUsersController {

    private final UserManagementService users;

    public AdminUsersController(UserManagementService users) {
        this.users = users;
    }

    @GetMapping
    public UserManagementService.UserPage list(@RequestParam(defaultValue = "0") int page,
                                               @RequestParam(defaultValue = "20") int size) {
        return users.list(page, size);
    }

    @PostMapping
    public ResponseEntity<UserManagementService.TemporaryPassword> create(@Valid @RequestBody CreateUser request) {
        return ResponseEntity.status(HttpStatus.CREATED).cacheControl(CacheControl.noStore())
                .body(users.create(request.username(), request.displayName(), request.roles()));
    }

    @PutMapping("/{id}")
    public UserResponse update(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id, @Valid @RequestBody UpdateUser request) {
        return users.update(UUID.fromString(jwt.getSubject()), id, request.username(),
                request.displayName(), request.roles(), request.active());
    }

    @PostMapping("/{id}/reset-password")
    public ResponseEntity<UserManagementService.TemporaryPassword> resetPassword(@PathVariable UUID id) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(users.resetPassword(id));
    }

    public record CreateUser(
            @NotBlank @Size(min = 3, max = 50) String username,
            @NotBlank @Size(max = 100) String displayName,
            @NotEmpty Set<@NotNull UserRole> roles
    ) {
    }

    public record UpdateUser(
            @NotBlank @Size(min = 3, max = 50) String username,
            @NotBlank @Size(max = 100) String displayName,
            @NotEmpty Set<@NotNull UserRole> roles,
            @NotNull Boolean active
    ) {
    }
}
