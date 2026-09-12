package ru.mirea.pool.presentation.console;

import ru.mirea.pool.application.auth.UserSession;
import ru.mirea.pool.application.service.UserManagementService;
import ru.mirea.pool.domain.model.SystemUser;
import ru.mirea.pool.domain.model.UserRole;

import java.util.Arrays;
import java.util.List;

public final class AdminMenu {

    private final InputReader inputReader;
    private final UserManagementService userManagementService;
    private final ConsoleErrorHandler errorHandler;

    public AdminMenu(
            InputReader inputReader,
            UserManagementService userManagementService,
            ConsoleErrorHandler errorHandler
    ) {
        this.inputReader = inputReader;
        this.userManagementService = userManagementService;
        this.errorHandler = errorHandler;
    }

    public void run(UserSession session) {
        boolean running = true;
        while (running) {
            printMenu();
            int choice = inputReader.readIntInRange("Выберите действие: ", 0, 8);
            try {
                switch (choice) {
                    case 1 -> printUsers(userManagementService.getAllUsers(session));
                    case 2 -> createUser(session);
                    case 3 -> findUser(session);
                    case 4 -> changeUsername(session);
                    case 5 -> changeRole(session);
                    case 6 -> changePassword(session);
                    case 7 -> deactivateUser(session);
                    case 8 -> activateUser(session);
                    case 0 -> running = false;
                    default -> throw new IllegalStateException("Неизвестный пункт меню.");
                }
            } catch (RuntimeException exception) {
                errorHandler.handle(exception);
            }
        }
    }

    public void printUsers(List<SystemUser> users) {
        if (users.isEmpty()) {
            System.out.println("Системные пользователи не найдены.");
            return;
        }

        System.out.printf("%-5s %-22s %-12s %-10s %-22s%n",
                "ID", "Username", "Role", "Active", "Created at");
        System.out.println("-".repeat(76));
        users.forEach(user -> System.out.printf("%-5s %-22s %-12s %-10s %-22s%n",
                value(user.getId()),
                user.getUsername(),
                user.getRole(),
                user.isActive() ? "Да" : "Нет",
                value(user.getCreatedAt())));
    }

    private void createUser(UserSession session) {
        String username = inputReader.readNonEmptyString("Username: ");
        char[] password = inputReader.readPassword("Пароль: ");
        try {
            UserRole role = inputReader.readRole("Роль (1/ADMIN, 2/OPERATOR): ");
            SystemUser user = userManagementService.createUser(session, username, password, role);
            System.out.println("Пользователь создан, ID: " + user.getId());
        } finally {
            Arrays.fill(password, '\0');
        }
    }

    private void findUser(UserSession session) {
        long id = inputReader.readLong("ID пользователя: ");
        printUsers(List.of(userManagementService.getUserById(session, id)));
    }

    private void changeUsername(UserSession session) {
        long id = inputReader.readLong("ID пользователя: ");
        String username = inputReader.readNonEmptyString("Новый username: ");
        userManagementService.changeUsername(session, id, username);
        System.out.println("Username изменён.");
    }

    private void changeRole(UserSession session) {
        long id = inputReader.readLong("ID пользователя: ");
        UserRole role = inputReader.readRole("Новая роль (1/ADMIN, 2/OPERATOR): ");
        userManagementService.changeRole(session, id, role);
        System.out.println("Роль изменена.");
    }

    private void changePassword(UserSession session) {
        long id = inputReader.readLong("ID пользователя: ");
        char[] password = inputReader.readPassword("Новый пароль: ");
        try {
            userManagementService.changePassword(session, id, password);
            System.out.println("Пароль изменён.");
        } finally {
            Arrays.fill(password, '\0');
        }
    }

    private void deactivateUser(UserSession session) {
        long id = inputReader.readLong("ID пользователя: ");
        userManagementService.deactivateUser(session, id);
        System.out.println("Пользователь заблокирован.");
    }

    private void activateUser(UserSession session) {
        long id = inputReader.readLong("ID пользователя: ");
        userManagementService.activateUser(session, id);
        System.out.println("Пользователь разблокирован.");
    }

    private String value(Object value) {
        return value == null ? "-" : value.toString();
    }

    private void printMenu() {
        System.out.println();
        System.out.println("========== АДМИНИСТРАТИВНАЯ ПАНЕЛЬ ==========");
        System.out.println("1. Показать системных пользователей");
        System.out.println("2. Создать пользователя");
        System.out.println("3. Найти пользователя по ID");
        System.out.println("4. Изменить username");
        System.out.println("5. Изменить роль");
        System.out.println("6. Сменить пароль пользователя");
        System.out.println("7. Заблокировать пользователя");
        System.out.println("8. Разблокировать пользователя");
        System.out.println("0. Назад");
    }
}
