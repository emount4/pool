package ru.mirea.pool.presentation.console;

import ru.mirea.pool.application.auth.UserSession;

public final class ConsoleApplication {

    private final LoginMenu loginMenu;
    private final MainMenu mainMenu;
    private final ConsoleErrorHandler errorHandler;

    public ConsoleApplication(
            LoginMenu loginMenu,
            MainMenu mainMenu,
            ConsoleErrorHandler errorHandler
    ) {
        this.loginMenu = loginMenu;
        this.mainMenu = mainMenu;
        this.errorHandler = errorHandler;
    }

    public void run() {
        try {
            loginMenu.createFirstAdminIfRequired();

            boolean running = true;
            while (running) {
                UserSession session = loginMenu.login();
                MainMenu.MenuResult result = mainMenu.run(session);
                if (result == MainMenu.MenuResult.EXIT) {
                    running = false;
                } else {
                    System.out.println("Вы вышли из учётной записи.");
                }
            }
        } catch (RuntimeException exception) {
            errorHandler.handle(exception);
        }

        System.out.println("Работа программы завершена.");
    }
}
