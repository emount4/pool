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
            } catch (OperationCancelledException exception) {
                System.out.println(exception.getMessage());
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

        System.out.printf("%-4s %-11s %-13s %-15s %-20s %-10s%n",
                "ID", "Имя", "Фамилия", "Телефон", "Email", "Дата рождения");
        System.out.println("-".repeat(78));
        clients.forEach(this::printClientRow);
    }

    public void printClient(Client client) {
        printClients(List.of(client));
    }

    private void createClient() {
        printFormHeader("ДОБАВЛЕНИЕ КЛИЕНТА");
        String firstName = inputReader.readCancellableNonEmptyString("Имя: ");
        String lastName = inputReader.readCancellableNonEmptyString("Фамилия: ");
        String phone = inputReader.readCancellableNonEmptyString("Телефон: ");
        String email = inputReader.readCancellableString(
                "Email (можно оставить пустым): "
        );
        LocalDate birthDate = inputReader.readCancellableDate("Дата рождения");

        Client client = clientService.createClient(
                firstName,
                lastName,
                phone,
                email,
                birthDate
        );
        System.out.printf("Клиент №%d создан: %s %s, %s.%n",
                client.getId(), client.getFirstName(), client.getLastName(), client.getPhone());
    }

    private void findClient() {
        printFormHeader("ПОИСК КЛИЕНТА");
        printClient(selectClient());
    }

    private void updateClient() {
        printFormHeader("РЕДАКТИРОВАНИЕ КЛИЕНТА");
        Client current = selectClient();
        long id = current.getId();
        System.out.println("Текущие данные:");
        printClient(current);
        System.out.println("Нажмите Enter, чтобы оставить текущее значение.");

        String firstName = inputReader.readNonEmptyStringWithDefault(
                "Имя [" + current.getFirstName() + "]: ", current.getFirstName()
        );
        String lastName = inputReader.readNonEmptyStringWithDefault(
                "Фамилия [" + current.getLastName() + "]: ", current.getLastName()
        );
        String phone = inputReader.readNonEmptyStringWithDefault(
                "Телефон [" + current.getPhone() + "]: ", current.getPhone()
        );
        String email = inputReader.readOptionalStringWithDefault(
                "Email [" + optionalValue(current.getEmail())
                        + "] (символ - очистит поле): ",
                current.getEmail()
        );
        LocalDate birthDate = inputReader.readDateWithDefault(
                "Дата рождения", current.getBirthDate()
        );

        clientService.updateClient(id, firstName, lastName, phone, email, birthDate);
        System.out.println("Данные клиента обновлены.");
    }

    private void deleteClient() {
        printFormHeader("УДАЛЕНИЕ КЛИЕНТА");
        Client client = selectClient();
        long id = client.getId();
        printClient(client);
        boolean confirmed = inputReader.readConfirmation(
                "Удалить клиента " + client.getFirstName() + " " + client.getLastName()
                        + "? (да/нет): ",
                false
        );
        if (!confirmed) {
            System.out.println("Удаление отменено.");
            return;
        }
        clientService.deleteClient(id);
        System.out.println("Клиент удалён.");
    }

    private Client selectClient() {
        List<Client> clients = clientService.getAllClients();
        if (clients.isEmpty()) {
            System.out.println("Клиенты не найдены.");
            throw new OperationCancelledException();
        }
        printClients(clients);
        while (true) {
            long id = inputReader.readCancellableLong("Введите ID клиента из списка: ");
            if (clients.stream().anyMatch(client -> client.getId() == id)) {
                return clientService.getClientById(id);
            }
            System.out.println("Выберите ID из показанного списка.");
        }
    }

    private void printClientRow(Client client) {
        System.out.printf("%-4s %-11s %-13s %-15s %-20s %-10s%n",
                value(client.getId()),
                shorten(client.getFirstName(), 11),
                shorten(client.getLastName(), 13),
                shorten(client.getPhone(), 15),
                shorten(value(client.getEmail()), 20),
                value(client.getBirthDate()));
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

    private String optionalValue(String value) {
        return value == null || value.isBlank() ? "не указан" : value;
    }

    private void printFormHeader(String title) {
        System.out.println();
        System.out.println("---------- " + title + " ----------");
        System.out.println("Для отмены введите 0 или «назад» на любом шаге.");
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
