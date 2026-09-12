package ru.mirea.pool;

import ru.mirea.pool.application.auth.AdminBootstrap;
import ru.mirea.pool.application.auth.AuthService;
import ru.mirea.pool.application.security.PasswordHasher;
import ru.mirea.pool.application.security.PasswordPolicy;
import ru.mirea.pool.application.service.ClientService;
import ru.mirea.pool.application.service.StatisticsService;
import ru.mirea.pool.application.service.UserManagementService;
import ru.mirea.pool.application.service.VisitService;
import ru.mirea.pool.domain.repository.ClientRepository;
import ru.mirea.pool.domain.repository.SystemUserRepository;
import ru.mirea.pool.domain.repository.VisitRepository;
import ru.mirea.pool.infrastructure.database.DatabaseManager;
import ru.mirea.pool.infrastructure.database.JdbcClientRepository;
import ru.mirea.pool.infrastructure.database.JdbcSystemUserRepository;
import ru.mirea.pool.infrastructure.database.JdbcVisitRepository;
import ru.mirea.pool.infrastructure.export.DataExporter;
import ru.mirea.pool.infrastructure.export.ExcelExporter;
import ru.mirea.pool.infrastructure.security.Pbkdf2PasswordHasher;
import ru.mirea.pool.presentation.console.AdminMenu;
import ru.mirea.pool.presentation.console.ClientMenu;
import ru.mirea.pool.presentation.console.ConsoleApplication;
import ru.mirea.pool.presentation.console.ConsoleErrorHandler;
import ru.mirea.pool.presentation.console.InputReader;
import ru.mirea.pool.presentation.console.LoginMenu;
import ru.mirea.pool.presentation.console.MainMenu;
import ru.mirea.pool.presentation.console.VisitMenu;

public class Main {

    public static void main(String[] args) {
        DatabaseManager databaseManager = new DatabaseManager();

        SystemUserRepository systemUserRepository =
                new JdbcSystemUserRepository(databaseManager);
        ClientRepository clientRepository =
                new JdbcClientRepository(databaseManager);
        VisitRepository visitRepository =
                new JdbcVisitRepository(databaseManager);

        PasswordHasher passwordHasher = new Pbkdf2PasswordHasher();
        PasswordPolicy passwordPolicy = new PasswordPolicy();
        DataExporter dataExporter = new ExcelExporter();

        AuthService authService = new AuthService(systemUserRepository, passwordHasher);
        AdminBootstrap adminBootstrap = new AdminBootstrap(
                systemUserRepository,
                passwordHasher,
                passwordPolicy
        );
        UserManagementService userManagementService = new UserManagementService(
                systemUserRepository,
                passwordHasher,
                passwordPolicy
        );
        ClientService clientService = new ClientService(clientRepository, visitRepository);
        VisitService visitService = new VisitService(clientRepository, visitRepository);
        StatisticsService statisticsService = new StatisticsService(
                clientRepository,
                visitRepository
        );

        InputReader inputReader = new InputReader();
        ConsoleErrorHandler errorHandler = new ConsoleErrorHandler();
        LoginMenu loginMenu = new LoginMenu(
                inputReader,
                authService,
                adminBootstrap,
                errorHandler
        );
        ClientMenu clientMenu = new ClientMenu(inputReader, clientService, errorHandler);
        VisitMenu visitMenu = new VisitMenu(inputReader, visitService, errorHandler);
        AdminMenu adminMenu = new AdminMenu(
                inputReader,
                userManagementService,
                errorHandler
        );
        MainMenu mainMenu = new MainMenu(
                inputReader,
                clientService,
                visitService,
                statisticsService,
                userManagementService,
                dataExporter,
                clientMenu,
                visitMenu,
                adminMenu,
                errorHandler
        );

        ConsoleApplication application = new ConsoleApplication(
                loginMenu,
                mainMenu,
                errorHandler
        );
        application.run();
    }
}
