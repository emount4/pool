package ru.mirea.pool.application;

import ru.mirea.pool.application.auth.*;
import ru.mirea.pool.application.service.*;
import ru.mirea.pool.application.security.PasswordPolicy;
import ru.mirea.pool.infrastructure.database.*;
import ru.mirea.pool.infrastructure.security.Pbkdf2PasswordHasher;

/** Composition root: controllers depend on services, never on SQL. */
public final class AppContext {
    public final AuthService auth;
    public final AdminBootstrap bootstrap;
    public final ClientService clients;
    public final VisitService visits;
    public final UserManagementService users;
    public final StatisticsService statistics;

    public AppContext() {
        this(new DatabaseManager());
    }

    public AppContext(DatabaseManager database) {
        var clientRepo = new JdbcClientRepository(database);
        var visitRepo = new JdbcVisitRepository(database);
        var userRepo = new JdbcSystemUserRepository(database);
        var hasher = new Pbkdf2PasswordHasher();
        var policy = new PasswordPolicy();
        auth = new AuthService(userRepo, hasher);
        bootstrap = new AdminBootstrap(userRepo, hasher, policy);
        clients = new ClientService(clientRepo, visitRepo);
        visits = new VisitService(clientRepo, visitRepo);
        users = new UserManagementService(userRepo, hasher, policy);
        statistics = new StatisticsService(clientRepo, visitRepo);
    }
}
