package ru.mirea.pool.backend.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;
import ru.mirea.pool.backend.domain.Visit;
import ru.mirea.pool.backend.domain.VisitStatus;
import ru.mirea.pool.backend.repository.VisitRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class BulkVisitService {

    private final VisitRepository visits;
    private final VisitRules rules;
    private final AuditService audit;
    private final TransactionTemplate transaction;

    public BulkVisitService(VisitRepository visits, VisitRules rules, AuditService audit,
                            PlatformTransactionManager manager) {
        this.visits = visits;
        this.rules = rules;
        this.audit = audit;
        this.transaction = new TransactionTemplate(manager);
    }

    public List<Result> apply(List<UUID> ids, Action action, VisitStatus status, String tag) {
        if ((action == Action.ADD_TAG || action == Action.REMOVE_TAG)
                && (tag == null || tag.isBlank() || tag.strip().length() > 50)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Укажите тег длиной от 1 до 50 символов.");
        }
        if (action == Action.STATUS && status == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Выберите статус.");
        }
        List<Result> results = new ArrayList<>();
        for (UUID id : ids.stream().distinct().toList()) {
            try {
                transaction.executeWithoutResult(ignored -> update(id, action, status, tag));
                results.add(new Result(id, true, null));
            } catch (ResponseStatusException exception) {
                results.add(new Result(id, false, exception.getReason()));
            } catch (org.springframework.dao.DataAccessException exception) {
                results.add(new Result(id, false, "Конфликт данных. Обновите запись и повторите."));
            }
        }
        return results;
    }

    private void update(UUID id, Action action, VisitStatus status, String tag) {
        Visit visit = visits.findByPublicId(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Посещение не найдено."));
        switch (action) {
            case ARCHIVE -> visit.archive();
            case RESTORE -> visit.restore();
            case ADD_TAG -> visit.addTag(tag.strip());
            case REMOVE_TAG -> visit.removeTag(tag.strip());
            case STATUS -> changeStatus(visit, status);
        }
        visits.flush();
        audit.record("BULK_" + action.name(), "VISIT", id, "Массовая операция над посещением");
    }

    private void changeStatus(Visit visit, VisitStatus status) {
        if (visit.getStatus() == status) {
            return;
        }
        if (status == VisitStatus.CANCELLED) {
            rules.requireEditable(visit);
        } else if (status == VisitStatus.PLANNED && visit.getStatus() == VisitStatus.CANCELLED) {
            if (visit.isArchived()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Сначала восстановите запись из архива.");
            }
            rules.validateSchedule(visit.getVisitDate(), visit.getStartTime(), visit.getDurationMinutes());
        } else {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Можно отменить будущую запись или вернуть отменённую в запланированные.");
        }
        visit.changeStatus(status);
    }

    public enum Action {
        ARCHIVE, RESTORE, STATUS, ADD_TAG, REMOVE_TAG
    }

    public record Result(UUID id, boolean success, String error) {
    }
}
