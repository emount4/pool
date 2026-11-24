package ru.mirea.pool.backend.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import ru.mirea.pool.backend.domain.Visit;
import ru.mirea.pool.backend.domain.VisitStatus;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;

@Component
public class VisitRules {

    public static final LocalTime OPENING_TIME = LocalTime.of(7, 0);
    public static final LocalTime CLOSING_TIME = LocalTime.of(22, 0);
    public static final int SLOT_MINUTES = 30;

    private final Clock clock;

    public VisitRules(@org.springframework.beans.factory.annotation.Value("${app.pool-zone:Europe/Moscow}") String zone) {
        this.clock = Clock.system(ZoneId.of(zone));
    }

    public LocalDateTime now() {
        return LocalDateTime.now(clock);
    }

    public void validateDuration(int durationMinutes) {
        if (durationMinutes < 30 || durationMinutes > 180 || durationMinutes % SLOT_MINUTES != 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Длительность должна быть от 30 до 180 минут с шагом 30 минут.");
        }
    }

    public void validateSchedule(LocalDate date, LocalTime start, int durationMinutes) {
        validateDuration(durationMinutes);
        LocalDateTime startsAt = date.atTime(start);
        if (start.getMinute() % SLOT_MINUTES != 0 || start.getSecond() != 0 || start.getNano() != 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Время начала должно быть кратно 30 минутам.");
        }
        if (start.isBefore(OPENING_TIME) || startsAt.plusMinutes(durationMinutes).isAfter(date.atTime(CLOSING_TIME))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Сеанс должен проходить с 07:00 до 22:00.");
        }
        if (!startsAt.isAfter(now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Выберите время в будущем.");
        }
    }

    public void requireEditable(Visit visit) {
        if (visit.isArchived() || visit.getStatus() != VisitStatus.PLANNED
                || !visit.getVisitDate().atTime(visit.getStartTime()).isAfter(now())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Изменить можно только действующую запись на ещё не начавшееся посещение.");
        }
    }
}
