package ru.mirea.pool.backend.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.mirea.pool.backend.domain.VisitStatus;
import ru.mirea.pool.backend.repository.VisitRepository;

@Component
public class VisitStatusUpdater {

    private final VisitRepository visits;
    private final VisitRules rules;

    public VisitStatusUpdater(VisitRepository visits, VisitRules rules) {
        this.visits = visits;
        this.rules = rules;
    }

    @Scheduled(fixedDelayString = "${app.status-refresh-delay:60000}")
    @Transactional
    public void refresh() {
        visits.refreshStatuses(rules.now(), VisitStatus.PLANNED, VisitStatus.IN_PROGRESS, VisitStatus.COMPLETED);
    }
}
