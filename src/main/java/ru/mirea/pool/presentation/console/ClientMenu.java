package ru.mirea.pool.presentation.console;

import ru.mirea.pool.application.service.ClientService;
import ru.mirea.pool.domain.model.Client;

import java.time.LocalDate;
import java.util.List;

public final class ClientMenu {

    private final InputReader inputReader;
    private final ClientService clientService;
    private final ConsoleErrorHandler errorHandler;

    public ClientMenu(
            InputReader inputReader,
            ClientService clientService,
            ConsoleErrorHandler errorHandler
    ) {
        this.inputReader = inputReader;
        this.clientService = clientService;
        this.errorHandler = errorHandler;
    }

    public void run() {
        boolean running = true;
        while (running) {
            printMenu();
            int choice = inputReader.readIntInRange("Выберите действие: ", 0, 5);
            try {
                switch (choice) {
                    case 1 -> createClient();
                    case 2 -> printClients(clientService.getAllClients());
                    case 3 -> findClient();
                    case 4 -> updateClient();
                    case 5 -> deleteClient();
                    case 0 -> running = false;
                    default -> throw new IllegalStateException("Неизвестный пункт меню.");
                }
            } catch (RuntimeException exception) {
                errorHandler.handle(exception);
            }
        }
    }

    public void printClients(List<Client> clients) {
        if (clients.isEmpty()) {
            System.out.println("Клиенты не найдены.");
            return;
        }

        System.out.printf("%-5s %-16s %-18s %-18s %-30s %-12s%n",
                "ID", "Имя", "Фамилия", "Телефон", "Email", "Дата рождения");
        System.out.println("-".repeat(105));
        clients.forEach(this::printClientRow);
    }

    public void printClient(Client client) {
        printClients(List.of(client));
    }

    private void createClient() {
        String firstName = inputReader.readNonEmptyString("Имя: ");
        String lastName = inputReader.readNonEmptyString("Фамилия: ");
        String phone = inputReader.readNonEmptyString("Телефон: ");
        String email = inputReader.readString("Email (можно оставить пустым): ");
        LocalDate birthDate = inputReader.readDate("Дата рождения");

        Client client = clientService.createClient(
                firstName,
                lastName,
                phone,
                email,
                birthDate
        );
        System.out.println("Клиент создан, ID: " + client.getId());
    }

    private void findClient() {
        long id = inputReader.readLong("ID клиента: ");
        printClient(clientService.getClientById(id));
    }

    private void updateClient() {
        long id = inputReader.readLong("ID клиента: ");
        Client current = clientService.getClientById(id);
        System.out.println("Текущие данные:");
        printClient(current);

        String firstName = inputReader.readNonEmptyString("Новое имя: ");
        String lastName = inputReader.readNonEmptyString("Новая фамилия: ");
        String phone = inputReader.readNonEmptyString("Новый телефон: ");
        String email = inputReader.readString("Новый email (можно оставить пустым): ");
        LocalDate birthDate = inputReader.readDate("Новая дата рождения");

        clientService.updateClient(id, firstName, lastName, phone, email, birthDate);
        System.out.println("Данные клиента обновлены.");
    }

    private void deleteClient() {
        long id = inputReader.readLong("ID клиента: ");
        clientService.deleteClient(id);
        System.out.println("Клиент удалён.");
    }

    private void printClientRow(Client client) {
        System.out.printf("%-5s %-16s %-18s %-18s %-30s %-12s%n",
                value(client.getId()),
                client.getFirstName(),
                client.getLastName(),
                client.getPhone(),
                value(client.getEmail()),
                value(client.getBirthDate()));
    }

    private String value(Object value) {
        return value == null ? "-" : value.toString();
    }

    private void printMenu() {
        System.out.println();
        System.out.println("========== КЛИЕНТЫ ==========");
        System.out.println("1. Добавить клиента");
        System.out.println("2. Показать всех клиентов");
        System.out.println("3. Найти клиента по ID");
        System.out.println("4. Изменить клиента");
        System.out.println("5. Удалить клиента");
        System.out.println("0. Назад");
    }
}
