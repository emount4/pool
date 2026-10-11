package ru.mirea.pool.presentation.fx.controller;

import javafx.fxml.FXML;
import javafx.scene.layout.BorderPane;
import javafx.scene.control.*;
import javafx.collections.*;
import javafx.stage.*;
import ru.mirea.pool.application.AppContext;
import ru.mirea.pool.application.auth.UserSession;
import ru.mirea.pool.domain.model.*;
import ru.mirea.pool.infrastructure.export.TableExporter;
import ru.mirea.pool.presentation.fx.PoolApplication;
import ru.mirea.pool.presentation.fx.util.*;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

public final class MainController {
    @FXML private BorderPane root;
    @FXML private Label userLabel, statusLabel;
    @FXML private TabPane tabs;
    @FXML private Tab visitsTab, usersTab;
    @FXML private ClientsController clientsController;
    @FXML private VisitsController visitsController;
    @FXML private StatisticsController statisticsController;
    @FXML private UsersController usersController;
    public final ObservableList<Client> clients = FXCollections.observableArrayList();
    public final ObservableList<Visit> visits = FXCollections.observableArrayList();
    public final ObservableList<SystemUser> users = FXCollections.observableArrayList();
    private Map<Long, Client> clientIndex = Map.of();
    public AppContext context;
    public UserSession session;
    public FxTasks tasks;
    private PoolApplication application;

    public void configure(AppContext context, UserSession session, FxTasks tasks, PoolApplication app) {
        this.context = context; this.session = session; this.tasks = tasks; this.application = app;
        userLabel.setText(session.username() + "  ·  " + Labels.role(session.role()));
        clientsController.configure(this);
        visitsController.configure(this);
        statisticsController.configure(this);
        usersController.configure(this);
        if (session.role() != UserRole.ADMIN) tabs.getTabs().remove(usersTab);
        reload();
    }
    public Window owner() { return root.getScene() == null ? null : root.getScene().getWindow(); }
    public Client client(long id) { return clientIndex.get(id); }
    public <T> void run(String message, Callable<T> work, Consumer<T> done) { tasks.run(root, statusLabel, message, work, done); }
    public void mutate(String message, Runnable action) {
        run(message, () -> { action.run(); return true; }, ignored -> reload());
    }
    @FXML public void reload() {
        run("Обновление данных…", () -> {
            context.visits.refreshStatuses();
            return new Snapshot(context.clients.getAllClients(), context.visits.getAllVisits(),
                    session.role() == UserRole.ADMIN ? context.users.getAllUsers(session) : List.of());
        }, data -> {
            var index = new HashMap<Long, Client>();
            data.clients().forEach(c -> index.put(c.getId(), c));
            clientIndex = Map.copyOf(index);
            clients.setAll(data.clients()); visits.setAll(data.visits()); users.setAll(data.users());
            visitsController.applyFilters();
            statisticsController.refresh();
            users.stream().filter(u -> u.getId() == session.userId()).findFirst()
                    .ifPresent(u -> userLabel.setText(u.getUsername() + "  ·  " + Labels.role(u.getRole())));
            statusLabel.setText("Обновлено в " + java.time.LocalTime.now().format(Labels.TIME)
                    + "  ·  Клиентов: " + clients.size() + "  ·  Посещений: " + visits.size());
        });
    }
    public void showVisits(Client client) {
        tabs.getSelectionModel().select(visitsTab);
        visitsController.forClient(client);
    }
    public void export(String name, List<String> headers, List<List<String>> rows) {
        var chooser = new FileChooser();
        chooser.setTitle("Экспорт текущей таблицы");
        chooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("Excel (*.xlsx)", "*.xlsx"),
                new FileChooser.ExtensionFilter("CSV UTF-8 (*.csv)", "*.csv"));
        chooser.setInitialFileName(name + "-" + java.time.LocalDate.now() + ".xlsx");
        var file = chooser.showSaveDialog(owner());
        if (file == null) return;
        String extension = chooser.getSelectedExtensionFilter().getExtensions().getFirst().substring(1);
        java.nio.file.Path path = file.toPath();
        if (!file.getName().toLowerCase(Locale.ROOT).endsWith(extension)) {
            path = file.toPath().resolveSibling(file.getName().replaceFirst("(?i)\\.(xlsx|csv)$", "") + extension);
            if (java.nio.file.Files.exists(path) && !Ui.confirm(owner(), "Заменить файл?", path.toString())) return;
        }
        final var destination = path;
        run("Сохранение экспорта…", () -> { new TableExporter().export(destination, headers, rows); return rows.size(); }, count -> {
            statusLabel.setText("Экспортировано строк: " + count);
            Ui.info(owner(), "Экспорт сохранён", "Строк: " + count + "\n" + destination.toAbsolutePath());
        });
    }
    @FXML private void logout() { application.showLogin(); }
    private record Snapshot(List<Client> clients, List<Visit> visits, List<SystemUser> users) { }
}
