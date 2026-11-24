package ru.mirea.pool.backend.error;

import org.slf4j.MDC;

import java.util.Map;

public record ApiError(
        String code,
        String message,
        Map<String, String> fields,
        String correlationId
) {

    public static ApiError of(String code, String message) {
        return new ApiError(code, message, Map.of(), MDC.get("correlationId"));
    }
}
