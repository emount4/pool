package ru.mirea.pool.domain.repository;

import ru.mirea.pool.domain.model.Visit;
import ru.mirea.pool.domain.model.VisitStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface VisitRepository {

    Visit save(Visit visit);

    Optional<Visit> findById(long id);

    List<Visit> findAll();

    void update(Visit visit);

    void deleteById(long id);

    int refreshStatuses(LocalDateTime now);

    List<Visit> findByClientId(long clientId);

    List<Visit> findByDate(LocalDate date);

    List<Visit> findByStatus(VisitStatus status);

    List<Visit> findByDateRange(LocalDate from, LocalDate to);

    List<Visit> findByClientIdAndDate(long clientId, LocalDate date);

    boolean existsByClientId(long clientId);
}
