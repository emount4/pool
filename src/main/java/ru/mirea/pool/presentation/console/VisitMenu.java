package ru.mirea.pool.presentation.console;

import ru.mirea.pool.application.auth.UserSession;
import ru.mirea.pool.application.service.VisitService;
import ru.mirea.pool.domain.model.Visit;
import ru.mirea.pool.domain.model.VisitStatus;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public final class VisitMenu {

    private final InputReader inputReader;
    private final VisitService visitService;
    private final ConsoleErrorHandler errorHandler;

    public VisitMenu(
            InputReader inputReader,
            VisitService visitService,
            ConsoleErrorHandler errorHandler
    ) {
        this.inputReader = inputReader;
        this.visitService = visitService;
        this.errorHandler = errorHandler;
    }

    public void run(UserSession session) {
        boolean running = true;
        while (running) {
            printMenu();
            int choice = inputReader.readIntInRange("Выберите действие: ", 0, 6);
            try {
                switch (choice) {
                    case 1 -> createVisit(session);
                    case 2 -> printVisits(visitService.getAllVisits());
                    case 3 -> findVisit();
                    case 4 -> updateVisit();
                    case 5 -> deleteVisit();
                    case 6 -> changeStatus();
                    case 0 -> running = false;
                    default -> throw new IllegalStateException("Неизвестный пункт меню.");
                }
            } catch (RuntimeException exception) {
                errorHandler.handle(exception);
            }
        }
    }

    public void printVisits(List<Visit> visits) {
        if (visits.isEmpty()) {
            System.out.println("Посещения не найдены.");
            return;
        }

        System.out.printf("%-5s %-10s %-12s %-8s %-12s %-10s %-14s %-10s%n",
                "ID", "Client ID", "Дата", "Время", "Минут", "Дорожка", "Статус", "Создал");
        System.out.println("-".repeat(92));
        visits.forEach(this::printVisitRow);
    }

    public void printVisit(Visit visit) {
        printVisits(List.of(visit));
    }

    private void createVisit(UserSession session) {
        long clientId = inputReader.readLong("ID клиента: ");
        LocalDate date = inputReader.readDate("Дата посещения");
        LocalTime time = inputReader.readTime("Время начала");
        int duration = inputReader.readIntInRange("Продолжительность (минуты): ", 30, 180);
        int lane = inputReader.readIntInRange("Номер дорожки: ", 1, 8);

        Visit visit = visitService.createVisit(session, clientId, date, time, duration, lane);
        System.out.println("Посещение создано, ID: " + visit.getId());
    }

    private void findVisit() {
        long id = inputReader.readLong("ID посещения: ");
        printVisit(visitService.getVisitById(id));
    }

    private void updateVisit() {
        long id = inputReader.readLong("ID посещения: ");
        Visit current = visitService.getVisitById(id);
        System.out.println("Текущие данные:");
        printVisit(current);

        long clientId = inputReader.readLong("Новый ID клиента: ");
        LocalDate date = inputReader.readDate("Новая дата посещения");
        LocalTime time = inputReader.readTime("Новое время начала");
        int duration = inputReader.readIntInRange("Новая продолжительность: ", 30, 180);
        int lane = inputReader.readIntInRange("Новый номер дорожки: ", 1, 8);

        visitService.updateVisit(id, clientId, date, time, duration, lane);
        System.out.println("Посещение обновлено.");
    }

    private void deleteVisit() {
        long id = inputReader.readLong("ID посещения: ");
        visitService.deleteVisit(id);
        System.out.println("Посещение удалено.");
    }

    private void changeStatus() {
        long id = inputReader.readLong("ID посещения: ");
        VisitStatus status = inputReader.readVisitStatus("Новый статус: ");
        visitService.changeStatus(id, status);
        System.out.println("Статус посещения изменён.");
    }

    private void printVisitRow(Visit visit) {
        System.out.printf("%-5s %-10s %-12s %-8s %-12d %-10d %-14s %-10s%n",
                value(visit.getId()),
                value(visit.getClientId()),
                value(visit.getVisitDate()),
                value(visit.getStartTime()),
                visit.getDurationMinutes(),
                visit.getLaneNumber(),
                value(visit.getStatus()),
                value(visit.getCreatedByUserId()));
    }

    private String value(Object value) {
        return value == null ? "-" : value.toString();
    }

    private void printMenu() {
        System.out.println();
        System.out.println("========== ПОСЕЩЕНИЯ ==========");
        System.out.println("1. Создать посещение");
        System.out.println("2. Показать все посещения");
        System.out.println("3. Найти посещение по ID");
        System.out.println("4. Изменить посещение");
        System.out.println("5. Удалить посещение");
        System.out.println("6. Изменить статус посещения");
        System.out.println("0. Назад");
    }
}
