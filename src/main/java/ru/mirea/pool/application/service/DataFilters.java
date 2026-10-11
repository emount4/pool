package ru.mirea.pool.application.service;

import ru.mirea.pool.domain.model.*;
import ru.mirea.pool.domain.exception.ValidationException;
import java.time.LocalDate;
import java.time.Period;
import java.util.Locale;

/** Pure predicates shared by tables and tests. Null filter values mean 'all'. */
public final class DataFilters {
    private DataFilters() { }
    public static boolean search(String query, String... fields) {
        String joined = String.join(" ", java.util.Arrays.stream(fields).map(s -> s == null ? "" : s).toList())
                .toLowerCase(Locale.ROOT);
        for (String word : (query == null ? "" : query.trim()).toLowerCase(Locale.ROOT).split("\\s+")) {
            if (!joined.contains(word)) return false;
        }
        return true;
    }
    public static boolean client(Client c, String query, int ageGroup, int emailGroup, LocalDate today) {
        int age = Period.between(c.getBirthDate(), today).getYears();
        boolean hasEmail = c.getEmail() != null && !c.getEmail().isBlank();
        return search(query, c.getFirstName(), c.getLastName(), c.getPhone(), c.getEmail())
                && (ageGroup == 0 || (ageGroup == 1 ? age < 18 : age >= 18))
                && (emailGroup == 0 || (emailGroup == 1 ? hasEmail : !hasEmail));
    }
    public static boolean visit(Visit v, Client c, String query, VisitStatus status, Integer lane,
                                LocalDate from, LocalDate to, Long clientId) {
        return (c == null ? query == null || query.isBlank() : search(query, c.getLastName(), c.getFirstName(), c.getPhone(), c.getEmail()))
                && (status == null || status == v.getStatus())
                && (lane == null || lane == v.getLaneNumber())
                && (clientId == null || clientId.equals(v.getClientId()))
                && inPeriod(v.getVisitDate(), from, to);
    }
    public static boolean inPeriod(LocalDate date, LocalDate from, LocalDate to) {
        return (from == null || !date.isBefore(from)) && (to == null || !date.isAfter(to));
    }
    public static void validatePeriod(LocalDate from, LocalDate to) {
        if (from != null && to != null && from.isAfter(to))
            throw new ValidationException("Начало периода не может быть позже окончания.");
    }
}
