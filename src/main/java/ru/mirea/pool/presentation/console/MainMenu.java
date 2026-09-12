package ru.mirea.pool.presentation.console;

import ru.mirea.pool.application.auth.UserSession;
import ru.mirea.pool.application.dto.StatisticsDto;
import ru.mirea.pool.application.service.ClientService;
import ru.mirea.pool.application.service.StatisticsService;
import ru.mirea.pool.application.service.UserManagementService;
import ru.mirea.pool.application.service.VisitService;
import ru.mirea.pool.domain.exception.AccessDeniedException;
import ru.mirea.pool.domain.model.UserRole;
import ru.mirea.pool.domain.model.VisitStatus;
import ru.mirea.pool.infrastructure.export.DataExporter;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class MainMenu {

    private static final DateTimeFormatter EXPORT_TIMESTAMP =
            DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private final InputReader inputReader;
    private final ClientService clientService;
    private final VisitService visitService;
    private final StatisticsService statisticsService;
    private final UserManagementService userManagementService;
    private final DataExporter dataExporter;
    private final ClientMenu clientMenu;
    private final VisitMenu visitMenu;
    private final AdminMenu adminMenu;
    private final ConsoleErrorHandler errorHandler;

    public MainMenu(
            InputReader inputReader,
            ClientService clientService,
            VisitService visitService,
            StatisticsService statisticsService,
            UserManagementService userManagementService,
            DataExporter dataExporter,
            ClientMenu clientMenu,
            VisitMenu visitMenu,
            AdminMenu adminMenu,
            ConsoleErrorHandler errorHandler
    ) {
        this.inputReader = inputReader;
        this.clientService = clientService;
        this.visitService = visitService;
        this.statisticsService = statisticsService;
        this.userManagementService = userManagementService;
        this.dataExporter = dataExporter;
        this.clientMenu = clientMenu;
        this.visitMenu = visitMenu;
        this.adminMenu = adminMenu;
        this.errorHandler = errorHandler;
    }

    public MenuResult run(UserSession session) {
        while (true) {
            printMenu(session);
            int choice = inputReader.readIntInRange("Выберите действие: ", 0, 9);

            try {
                switch (choice) {
                    case 1 -> clientMenu.run();
                    case 2 -> visitMenu.run(session);
                    case 3 -> runSearchMenu();
                    case 4 -> runFilterAndSortMenu();
                    case 5 -> printStatistics(statisticsService.getStatistics());
                    case 6 -> exportData();
                    case 7 -> runTablesMenu(session);
                    case 8 -> openAdminMenu(session);
                    case 9 -> {
                        return MenuResult.LOGOUT;
                    }
                    case 0 -> {
                        return MenuResult.EXIT;
                    }
                    default -> throw new IllegalStateException("Неизвестный пункт меню.");
                }
            } catch (RuntimeException exception) {
                errorHandler.handle(exception);
            }
        }
    }

    private void runSearchMenu() {
        boolean running = true;
        while (running) {
            System.out.println();
            System.out.println("========== ПОИСК ==========");
            System.out.println("1. Посещения по ID клиента");
            System.out.println("2. Посещения по дате");
            System.out.println("3. Клиенты по фамилии");
            System.out.println("0. Назад");

            int choice = inputReader.readIntInRange("Выберите действие: ", 0, 3);
            try {
                switch (choice) {
                    case 1 -> visitMenu.printVisits(visitService.searchByClient(
                            inputReader.readLong("ID клиента: ")
                    ));
                    case 2 -> visitMenu.printVisits(visitService.searchByDate(
                            inputReader.readDate("Дата посещения")
                    ));
                    case 3 -> clientMenu.printClients(clientService.searchByLastName(
                            inputReader.readNonEmptyString("Фамилия или её часть: ")
                    ));
                    case 0 -> running = false;
                    default -> throw new IllegalStateException("Неизвестный пункт меню.");
                }
            } catch (RuntimeException exception) {
                errorHandler.handle(exception);
            }
        }
    }

    private void runFilterAndSortMenu() {
        boolean running = true;
        while (running) {
            System.out.println();
            System.out.println("========== ФИЛЬТРАЦИЯ И СОРТИРОВКА ==========");
            System.out.println("1. Фильтр по статусу");
            System.out.println("2. Фильтр по диапазону дат");
            System.out.println("3. Дата и время по возрастанию");
            System.out.println("4. Дата и время по убыванию");
            System.out.println("5. Продолжительность по возрастанию");
            System.out.println("0. Назад");

            int choice = inputReader.readIntInRange("Выберите действие: ", 0, 5);
            try {
                switch (choice) {
                    case 1 -> visitMenu.printVisits(visitService.filterByStatus(
                            inputReader.readVisitStatus("Статус: ")
                    ));
                    case 2 -> filterByDateRange();
                    case 3 -> visitMenu.printVisits(visitService.sortByDateAscending());
                    case 4 -> visitMenu.printVisits(visitService.sortByDateDescending());
                    case 5 -> visitMenu.printVisits(visitService.sortByDurationAscending());
                    case 0 -> running = false;
                    default -> throw new IllegalStateException("Неизвестный пункт меню.");
                }
            } catch (RuntimeException exception) {
                errorHandler.handle(exception);
            }
        }
    }

    private void filterByDateRange() {
        LocalDate from = inputReader.readDate("Начальная дата");
        LocalDate to = inputReader.readDate("Конечная дата");
        visitMenu.printVisits(visitService.filterByDateRange(from, to));
    }

    private void printStatistics(StatisticsDto statistics) {
        System.out.println();
        System.out.println("========== СТАТИСТИКА ==========");
        System.out.println("Всего клиентов: " + statistics.totalClients());
        System.out.println("Всего посещений: " + statistics.totalVisits());
        for (VisitStatus status : VisitStatus.values()) {
            System.out.printf("%s: %d%n", status, statistics.visitsByStatus().getOrDefault(status, 0L));
        }
        System.out.println("Посещений сегодня: " + statistics.visitsToday());
        System.out.printf("Средняя продолжительность: %.2f мин.%n",
                statistics.averageDurationMinutes());
        System.out.println("Самая популярная дорожка: "
                + (statistics.mostPopularLane() == null ? "нет данных" : statistics.mostPopularLane()));
    }

    private void exportData() {
        String defaultPath = Path.of(
                "exports",
                "pool-data-" + LocalDateTime.now().format(EXPORT_TIMESTAMP) + ".xlsx"
        ).toString();
        String enteredPath = inputReader.readString(
                "Путь к файлу (Enter — " + defaultPath + "): "
        ).trim();
        Path destination = Path.of(enteredPath.isEmpty() ? defaultPath : enteredPath);

        dataExporter.export(
                clientService.getAllClients(),
                visitService.getAllVisits(),
                destination
        );
        System.out.println("Данные экспортированы: " + destination.toAbsolutePath());
    }

    private void runTablesMenu(UserSession session) {
        boolean running = true;
        while (running) {
            System.out.println();
            System.out.println("========== ТАБЛИЦЫ БАЗЫ ДАННЫХ ==========");
            System.out.println("1. clients");
            System.out.println("2. visits");
            if (session.role() == UserRole.ADMIN) {
                System.out.println("3. system_users");
            }
            System.out.println("0. Назад");

            int choice = inputReader.readIntInRange("Выберите действие: ", 0, 3);
            try {
                switch (choice) {
                    case 1 -> clientMenu.printClients(clientService.getAllClients());
                    case 2 -> visitMenu.printVisits(visitService.getAllVisits());
                    case 3 -> printSystemUsers(session);
                    case 0 -> running = false;
                    default -> throw new IllegalStateException("Неизвестный пункт меню.");
                }
            } catch (RuntimeException exception) {
                errorHandler.handle(exception);
            }
        }
    }

    private void printSystemUsers(UserSession session) {
        if (session.role() != UserRole.ADMIN) {
            throw new AccessDeniedException("Таблица system_users доступна только ADMIN.");
        }
        adminMenu.printUsers(userManagementService.getAllUsers(session));
    }

    private void openAdminMenu(UserSession session) {
        if (session.role() != UserRole.ADMIN) {
            throw new AccessDeniedException("Административная панель доступна только ADMIN.");
        }
        adminMenu.run(session);
    }

    private void printMenu(UserSession session) {
        System.out.println();
        System.out.println("========================================");
        System.out.println("     ИНФОРМАЦИОННАЯ СИСТЕМА «БАССЕЙН»");
        System.out.println("========================================");
        System.out.println("Пользователь: " + session.username());
        System.out.println("Роль: " + session.role());
        System.out.println();
        System.out.println("1. Клиенты");
        System.out.println("2. Посещения");
        System.out.println("3. Поиск");
        System.out.println("4. Фильтрация и сортировка");
        System.out.println("5. Статистика");
        System.out.println("6. Экспорт данных");
        System.out.println("7. Вывести таблицы базы данных");
        if (session.role() == UserRole.ADMIN) {
            System.out.println("8. Административная панель");
        }
        System.out.println("9. Выйти из учётной записи");
        System.out.println("0. Завершить программу");
        System.out.println();
    }

    public enum MenuResult {
        LOGOUT,
        EXIT
    }
}
