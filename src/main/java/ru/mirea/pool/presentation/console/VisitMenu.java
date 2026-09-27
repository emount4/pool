package ru.mirea.pool.presentation.console;

import ru.mirea.pool.application.auth.UserSession;
import ru.mirea.pool.application.dto.VisitSlotDto;
import ru.mirea.pool.application.service.ClientService;
import ru.mirea.pool.application.service.VisitService;
import ru.mirea.pool.domain.model.Client;
import ru.mirea.pool.domain.model.Visit;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class VisitMenu {

    private static final DateTimeFormatter DATE_OPTION_FORMAT =
            DateTimeFormatter.ofPattern("dd.MM.yyyy, EEEE", new Locale("ru"));
    private static final LocalTime MORNING_END = LocalTime.of(12, 0);
    private static final LocalTime DAY_END = LocalTime.of(17, 0);

    private final InputReader inputReader;
    private final ClientService clientService;
    private final VisitService visitService;
    private final ConsoleErrorHandler errorHandler;

    public VisitMenu(
            InputReader inputReader,
            ClientService clientService,
            VisitService visitService,
            ConsoleErrorHandler errorHandler
    ) {
        this.inputReader = inputReader;
        this.clientService = clientService;
        this.visitService = visitService;
        this.errorHandler = errorHandler;
    }

    public void run(UserSession session) {
        boolean running = true;
        while (running) {
            printMenu();
            int choice = inputReader.readIntInRange("Выберите действие: ", 0, 7);
            try {
                switch (choice) {
                    case 1 -> createVisit(session);
                    case 2 -> printVisits(visitService.getAllVisits());
                    case 3 -> findVisit();
                    case 4 -> updateVisit();
                    case 5 -> deleteVisit();
                    case 6 -> cancelVisit();
                    case 7 -> refreshStatuses();
                    case 0 -> running = false;
                    default -> throw new IllegalStateException("Неизвестный пункт меню.");
                }
            } catch (OperationCancelledException exception) {
                System.out.println(exception.getMessage());
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

        Map<Long, Client> clientsById = clientService.getAllClients().stream()
                .collect(Collectors.toMap(Client::getId, Function.identity()));

        System.out.printf("%-4s %-20s %-10s %-5s %4s %3s %-13s %5s%n",
                "ID", "Клиент", "Дата", "Время", "Мин", "Дор", "Статус", "Автор");
        System.out.println("-".repeat(71));
        visits.forEach(visit -> printVisitRow(visit, clientsById));
    }

    public void printVisit(Visit visit) {
        printVisits(List.of(visit));
    }

    private void createVisit(UserSession session) {
        printFormHeader("СОЗДАНИЕ ПОСЕЩЕНИЯ");
        long clientId = selectClientId();
        Client client = clientService.getClientById(clientId);

        LocalDate date;
        int duration;
        VisitSlotDto slot;
        while (true) {
            date = selectVisitDate();
            duration = selectDuration();
            List<VisitSlotDto> availableSlots = visitService.getAvailableSlots(
                    clientId,
                    date,
                    duration
            );
            if (availableSlots.isEmpty()) {
                System.out.println("На выбранную дату нет подходящих интервалов.");
                if (inputReader.readConfirmation(
                        "Выбрать другую дату или продолжительность? (да/нет, Enter — да): ",
                        true
                )) {
                    continue;
                }
                System.out.println("Запись не создана.");
                return;
            }
            slot = selectSlot(availableSlots);
            break;
        }

        int lane = selectLane(slot.availableLaneNumbers());
        System.out.println();
        System.out.println("Проверьте запись:");
        System.out.println("Клиент: " + client.getFirstName() + " " + client.getLastName());
        System.out.println("Дата: " + date.format(DATE_OPTION_FORMAT));
        System.out.println("Время: " + slot.startTime() + "–" + slot.endTime());
        System.out.println("Дорожка: " + lane);
        if (!inputReader.readConfirmation("Создать посещение? (да/нет, Enter — да): ", true)) {
            System.out.println("Запись не создана.");
            return;
        }

        Visit visit = visitService.createVisit(
                session,
                clientId,
                date,
                slot.startTime(),
                duration,
                lane
        );
        System.out.printf(
                "Посещение №%d создано: %s %s, %s %s, дорожка %d.%n",
                visit.getId(), client.getFirstName(), client.getLastName(),
                visit.getVisitDate(), visit.getStartTime(), visit.getLaneNumber()
        );
    }

    private void findVisit() {
        printFormHeader("ПОИСК ПОСЕЩЕНИЯ");
        printVisit(selectVisit());
    }

    private void updateVisit() {
        printFormHeader("РЕДАКТИРОВАНИЕ ПОСЕЩЕНИЯ");
        Visit current = selectVisit();
        long id = current.getId();
        System.out.println("Текущие данные:");
        printVisit(current);

        long clientId = current.getClientId();
        if (inputReader.readConfirmation(
                "Сменить клиента? (да/нет, Enter — нет): ", false
        )) {
            clientId = selectClientId();
        }
        LocalDate date;
        int duration;
        VisitSlotDto slot;
        while (true) {
            date = selectVisitDate();
            duration = selectDuration();
            List<VisitSlotDto> availableSlots = visitService.getAvailableSlotsForVisit(
                    id, clientId, date, duration
            );
            if (availableSlots.isEmpty()) {
                System.out.println("На выбранную дату нет подходящих интервалов.");
                if (inputReader.readConfirmation(
                        "Выбрать другую дату или продолжительность? (да/нет, Enter — да): ",
                        true
                )) {
                    continue;
                }
                System.out.println("Посещение не изменено.");
                return;
            }
            slot = selectSlot(availableSlots);
            break;
        }

        int lane = selectLane(slot.availableLaneNumbers());
        System.out.printf("Новые данные: %s %s–%s, %d минут, дорожка %d.%n",
                date, slot.startTime(), slot.endTime(), duration, lane);
        if (!inputReader.readConfirmation("Сохранить изменения? (да/нет, Enter — да): ", true)) {
            System.out.println("Посещение не изменено.");
            return;
        }

        visitService.updateVisit(id, clientId, date, slot.startTime(), duration, lane);
        System.out.println("Посещение обновлено.");
    }

    private void deleteVisit() {
        printFormHeader("УДАЛЕНИЕ ПОСЕЩЕНИЯ");
        Visit visit = selectVisit();
        long id = visit.getId();
        printVisit(visit);
        if (!inputReader.readConfirmation("Удалить это посещение? (да/нет): ", false)) {
            System.out.println("Удаление отменено.");
            return;
        }
        visitService.deleteVisit(id);
        System.out.println("Посещение удалено.");
    }

    private void cancelVisit() {
        printFormHeader("ОТМЕНА ПОСЕЩЕНИЯ");
        Visit visit = selectVisit();
        if (!inputReader.readConfirmation("Отменить это посещение? (да/нет): ", false)) {
            System.out.println("Отмена не выполнена.");
            return;
        }
        visitService.cancelVisit(visit.getId());
        System.out.println("Посещение отменено.");
    }

    private void refreshStatuses() {
        int updated = visitService.refreshStatuses();
        System.out.println("Актуализировано посещений: " + updated + ".");
    }

    long selectClientId() {
        while (true) {
            System.out.println();
            System.out.println("Выбор клиента:");
            System.out.println("1. Показать всех клиентов");
            System.out.println("2. Найти по фамилии или телефону");
            int mode = inputReader.readCancellableIntInRange("Выберите способ: ", 1, 2);

            List<Client> clients;
            if (mode == 1) {
                clients = clientService.getAllClients();
            } else {
                String query = inputReader.readCancellableNonEmptyString(
                        "Фамилия, её часть или полный телефон: "
                );
                clients = clientService.searchByLastNameOrPhone(query);
            }
            if (clients.isEmpty()) {
                System.out.println("Клиенты не найдены. Измените способ или запрос.");
                continue;
            }

            printClientChoices(clients);
            if (clients.size() == 1) {
                Client client = clients.get(0);
                if (inputReader.readConfirmation(
                        "Выбрать этого клиента? (да/нет, Enter — да): ", true
                )) {
                    return client.getId();
                }
                continue;
            }

            long selectedId = inputReader.readCancellableLong("Введите ID клиента из списка: ");
            boolean listed = clients.stream().anyMatch(client -> client.getId() == selectedId);
            if (listed) {
                return selectedId;
            }
            System.out.println("Выберите ID из показанного списка.");
        }
    }

    private Visit selectVisit() {
        List<Visit> visits = visitService.getAllVisits();
        if (visits.isEmpty()) {
            System.out.println("Посещения не найдены.");
            throw new OperationCancelledException();
        }
        printVisits(visits);
        while (true) {
            long id = inputReader.readCancellableLong("Введите ID посещения из списка: ");
            if (visits.stream().anyMatch(visit -> visit.getId() == id)) {
                return visitService.getVisitById(id);
            }
            System.out.println("Выберите ID из показанного списка.");
        }
    }

    private LocalDate selectVisitDate() {
        LocalDate today = LocalDate.now();
        int firstDay = 0;
        while (true) {
            System.out.println();
            System.out.println("Дата посещения:");
            for (int index = 0; index < 7; index++) {
                LocalDate date = today.plusDays(firstDay + index);
                String suffix = switch (firstDay + index) {
                    case 0 -> " (сегодня)";
                    case 1 -> " (завтра)";
                    default -> "";
                };
                System.out.printf("%d. %s%s%n", index + 1, date.format(DATE_OPTION_FORMAT), suffix);
            }
            System.out.println("8. Следующие 7 дней");
            if (firstDay > 0) {
                System.out.println("9. Предыдущие 7 дней");
            }

            int choice = inputReader.readCancellableIntInRange(
                    "Выберите дату: ", 1, firstDay > 0 ? 9 : 8
            );
            if (choice <= 7) {
                return today.plusDays(firstDay + choice - 1L);
            }
            if (choice == 8) {
                firstDay += 7;
            } else {
                firstDay -= 7;
            }
        }
    }

    private int selectDuration() {
        System.out.println();
        System.out.println("Продолжительность:");
        for (int index = 1; index <= 6; index++) {
            int minutes = index * 30;
            System.out.printf("%d. %d минут%n", index, minutes);
        }
        int choice = inputReader.readCancellableIntInRange(
                "Выберите продолжительность: ", 1, 6
        );
        return choice * 30;
    }

    private VisitSlotDto selectSlot(List<VisitSlotDto> slots) {
        List<VisitSlotDto> periodSlots = selectSlotPeriod(slots);
        System.out.println();
        System.out.println("Варианты времени:");
        for (int index = 0; index < periodSlots.size(); index++) {
            VisitSlotDto slot = periodSlots.get(index);
            System.out.printf(
                    "%2d. %s–%s  доступные дорожки: %s%n",
                    index + 1,
                    slot.startTime(),
                    slot.endTime(),
                    formatLanes(slot.availableLaneNumbers())
            );
        }
        int choice = inputReader.readCancellableIntInRange(
                "Выберите свободное время: ", 1, periodSlots.size()
        );
        return periodSlots.get(choice - 1);
    }

    private List<VisitSlotDto> selectSlotPeriod(List<VisitSlotDto> slots) {
        List<VisitSlotDto> morning = slots.stream()
                .filter(slot -> slot.startTime().isBefore(MORNING_END))
                .toList();
        List<VisitSlotDto> day = slots.stream()
                .filter(slot -> !slot.startTime().isBefore(MORNING_END))
                .filter(slot -> slot.startTime().isBefore(DAY_END))
                .toList();
        List<VisitSlotDto> evening = slots.stream()
                .filter(slot -> !slot.startTime().isBefore(DAY_END))
                .toList();

        while (true) {
            System.out.println();
            System.out.println("Период посещения:");
            System.out.printf("1. Утро   07:00–12:00 (вариантов: %d)%n", morning.size());
            System.out.printf("2. День   12:00–17:00 (вариантов: %d)%n", day.size());
            System.out.printf("3. Вечер  17:00–22:00 (вариантов: %d)%n", evening.size());
            int choice = inputReader.readCancellableIntInRange("Выберите период: ", 1, 3);
            List<VisitSlotDto> selected = switch (choice) {
                case 1 -> morning;
                case 2 -> day;
                case 3 -> evening;
                default -> throw new IllegalStateException("Неизвестный период.");
            };
            if (!selected.isEmpty()) {
                return selected;
            }
            System.out.println("В этом периоде нет свободного времени. Выберите другой.");
        }
    }

    private int selectLane(List<Integer> availableLanes) {
        if (availableLanes.size() == 1) {
            int lane = availableLanes.get(0);
            System.out.println("Доступна дорожка: " + lane);
            return lane;
        }

        System.out.println("Доступные дорожки: " + formatLanes(availableLanes));
        while (true) {
            int lane = inputReader.readCancellableIntInRange(
                    "Выберите дорожку: ", 1, 8
            );
            if (availableLanes.contains(lane)) {
                return lane;
            }
            System.out.println("Выберите дорожку из списка.");
        }
    }

    private String formatLanes(List<Integer> lanes) {
        return lanes.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(", "));
    }

    private void printClientChoices(List<Client> clients) {
        System.out.printf("%-5s %-20s %-20s %-18s%n", "ID", "Имя", "Фамилия", "Телефон");
        System.out.println("-".repeat(68));
        clients.forEach(client -> System.out.printf("%-5d %-20s %-20s %-18s%n",
                client.getId(), client.getFirstName(), client.getLastName(), client.getPhone()));
    }

    private void printVisitRow(Visit visit, Map<Long, Client> clientsById) {
        System.out.printf("%-4s %-20s %-10s %-5s %4d %3d %-13s %5s%n",
                value(visit.getId()),
                clientLabel(visit.getClientId(), clientsById),
                value(visit.getVisitDate()),
                value(visit.getStartTime()),
                visit.getDurationMinutes(),
                visit.getLaneNumber(),
                ConsoleLabels.visitStatus(visit.getStatus()),
                value(visit.getCreatedByUserId()));
    }

    private String clientLabel(Long clientId, Map<Long, Client> clientsById) {
        Client client = clientsById.get(clientId);
        if (client == null) {
            return "ID " + clientId;
        }
        return shorten(client.getFirstName() + " " + client.getLastName() + " [#" + clientId + "]", 20);
    }

    private String shorten(String value, int maxLength) {
        if (value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength - 3) + "...";
    }

    private String value(Object value) {
        return value == null ? "-" : value.toString();
    }

    private void printFormHeader(String title) {
        System.out.println();
        System.out.println("---------- " + title + " ----------");
        System.out.println("Для отмены введите 0 или «назад» на любом шаге.");
    }

    private void printMenu() {
        System.out.println();
        System.out.println("========== ПОСЕЩЕНИЯ ==========");
        System.out.println("1. Создать посещение");
        System.out.println("2. Показать все посещения");
        System.out.println("3. Найти посещение по ID");
        System.out.println("4. Изменить посещение");
        System.out.println("5. Удалить посещение");
        System.out.println("6. Отменить посещение");
        System.out.println("7. Актуализировать статусы");
        System.out.println("0. Назад");
    }
}
