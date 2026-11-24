package ru.mirea.pool.backend.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.security.access.prepost.PreAuthorize;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import ru.mirea.pool.backend.filter.VisitFilter;
import ru.mirea.pool.backend.service.CsvService;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/visits")
@SecurityRequirement(name = "bearerAuth")
public class CsvController {

    private final CsvService csv;

    public CsvController(CsvService csv) {
        this.csv = csv;
    }

    @GetMapping("/export")
    @PreAuthorize("hasAnyRole('VIEWER', 'OPERATOR', 'ADMIN')")
    public ResponseEntity<byte[]> export(@ModelAttribute VisitFilter filter,
            @RequestParam(defaultValue = "visitDate") String sort,
            @RequestParam(defaultValue = "false") boolean descending) {
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=pool-visits.csv")
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(csv.export(filter, sort, descending).getBytes(StandardCharsets.UTF_8));
    }

    @PostMapping(value = "/import/preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")
    public CsvService.Preview preview(@AuthenticationPrincipal Jwt jwt, @RequestParam MultipartFile file,
                                      @RequestParam CsvService.Mode mode) throws IOException {
        if (file.isEmpty() || file.getSize() > 1_000_000) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Выберите CSV размером до 1 МБ.");
        }
        return csv.preview(UUID.fromString(jwt.getSubject()), new String(file.getBytes(), StandardCharsets.UTF_8), mode);
    }

    @PostMapping("/import/apply")
    @PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")
    public List<CsvService.RowResult> apply(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody ApplyRequest request) {
        return csv.apply(UUID.fromString(jwt.getSubject()), request.previewId());
    }

    public record ApplyRequest(@NotNull UUID previewId) {
    }
}
