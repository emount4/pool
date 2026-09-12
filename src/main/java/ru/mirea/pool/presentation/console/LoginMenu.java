package ru.mirea.pool.presentation.console;

import ru.mirea.pool.application.auth.AdminBootstrap;
import ru.mirea.pool.application.auth.AuthService;
import ru.mirea.pool.application.auth.AuthenticationException;
import ru.mirea.pool.application.auth.UserSession;
import ru.mirea.pool.domain.exception.ValidationException;

import java.util.Arrays;

public final class LoginMenu {

    private final InputReader inputReader;
    private final AuthService authService;
    private final AdminBootstrap adminBootstrap;
    private final ConsoleErrorHandler errorHandler;

    public LoginMenu(
            InputReader inputReader,
            AuthService authService,
            AdminBootstrap adminBootstrap,
            ConsoleErrorHandler errorHandler
    ) {
        this.inputReader = inputReader;
        this.authService = authService;
        this.adminBootstrap = adminBootstrap;
        this.errorHandler = errorHandler;
    }

    public void createFirstAdminIfRequired() {
        if (!adminBootstrap.isBootstrapRequired()) {
            return;
        }

        System.out.println("В системе нет пользователей. Создайте первого администратора.");
        while (adminBootstrap.isBootstrapRequired()) {
            String username = inputReader.readNonEmptyString("Логин ADMIN: ");
            char[] password = inputReader.readPassword("Пароль ADMIN: ");
            try {
                adminBootstrap.createFirstAdmin(username, password);
                System.out.println("Первый администратор создан.");
            } catch (ValidationException exception) {
                errorHandler.handle(exception);
            } finally {
                Arrays.fill(password, '\0');
            }
        }
    }

    public UserSession login() {
        while (true) {
            printHeader();
            String username = inputReader.readNonEmptyString("Логин: ");
            char[] password = inputReader.readPassword("Пароль: ");

            try {
                UserSession session = authService.login(username, password);
                System.out.printf("%nДобро пожаловать, %s.%n", session.username());
                System.out.println("Роль: " + session.role());
                return session;
            } catch (AuthenticationException exception) {
                System.out.println(exception.getMessage());
            } catch (RuntimeException exception) {
                errorHandler.handle(exception);
            } finally {
                Arrays.fill(password, '\0');
            }
        }
    }

    private void printHeader() {
        System.out.println();
        System.out.println("========================================");
        System.out.println("     ИНФОРМАЦИОННАЯ СИСТЕМА «БАССЕЙН»");
        System.out.println("========================================");
    }
}
