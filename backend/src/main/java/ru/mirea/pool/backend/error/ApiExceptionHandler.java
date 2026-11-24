package ru.mirea.pool.backend.error;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;
import ru.mirea.pool.backend.security.SecurityErrors;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApiExceptionHandler.class);

    private final SecurityErrors securityErrors;

    public ApiExceptionHandler(SecurityErrors securityErrors) {
        this.securityErrors = securityErrors;
    }

    @ExceptionHandler(AccessDeniedException.class)
    public void handleAccessDenied(HttpServletRequest request, HttpServletResponse response,
                                   AccessDeniedException exception) throws IOException {
        securityErrors.handle(request, response, exception);
    }

    @ExceptionHandler(org.springframework.web.servlet.resource.NoResourceFoundException.class)
    public ResponseEntity<?> handleNotFound(jakarta.servlet.http.HttpServletRequest request) throws java.io.IOException {
        if (request.getRequestURI().startsWith("/api/")) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiError.of("NOT_FOUND", "Адрес API не найден."));
        }
        String html = new org.springframework.core.io.ClassPathResource("static/error/404.html")
                .getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).contentType(org.springframework.http.MediaType.TEXT_HTML)
                .body(html);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception exception) {
        LOGGER.error("Неожиданная ошибка запроса", exception);
        return ResponseEntity.internalServerError()
                .body(ApiError.of("INTERNAL_ERROR", "Не удалось выполнить запрос. Повторите попытку."));
    }

    @ExceptionHandler(org.springframework.web.multipart.MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiError> handleLargeFile() {
        return ResponseEntity.badRequest().body(ApiError.of("BAD_REQUEST", "Размер CSV должен быть до 1 МБ."));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiError> handleStatus(ResponseStatusException exception) {
        HttpStatus status = HttpStatus.resolve(exception.getStatusCode().value());
        String code = status == null ? "REQUEST_FAILED" : status.name();
        return ResponseEntity.status(exception.getStatusCode()).body(ApiError.of(code, exception.getReason()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException exception) {
        Map<String, String> fields = new LinkedHashMap<>();
        exception.getBindingResult().getFieldErrors()
                .forEach(error -> fields.putIfAbsent(error.getField(), error.getDefaultMessage()));
        return ResponseEntity.badRequest().body(new ApiError("VALIDATION_FAILED",
                "Проверьте поля запроса.", fields, MDC.get("correlationId")));
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ApiError> handleInvalidRequest() {
        return ResponseEntity.badRequest().body(ApiError.of("BAD_REQUEST", "Неверный формат запроса."));
    }

    @ExceptionHandler({DataIntegrityViolationException.class, OptimisticLockingFailureException.class})
    public ResponseEntity<ApiError> handleConflict() {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiError.of("CONFLICT", "Данные конфликтуют с существующей записью."));
    }
}
