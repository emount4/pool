package ru.mirea.pool.application.service;

import ru.mirea.pool.domain.exception.BusinessRuleException;
import ru.mirea.pool.domain.exception.EntityNotFoundException;
import ru.mirea.pool.domain.exception.ValidationException;
import ru.mirea.pool.domain.model.Client;
import ru.mirea.pool.domain.repository.ClientRepository;
import ru.mirea.pool.domain.repository.VisitRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

public final class ClientService {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$"
    );

    private final ClientRepository clientRepository;
    private final VisitRepository visitRepository;

    public ClientService(
            ClientRepository clientRepository,
            VisitRepository visitRepository
    ) {
        this.clientRepository = clientRepository;
        this.visitRepository = visitRepository;
    }

    public Client createClient(
            String firstName,
            String lastName,
            String phone,
            String email,
            LocalDate birthDate
    ) {
        Client client = buildValidatedClient(
                null,
                firstName,
                lastName,
                phone,
                email,
                birthDate
        );
        ensureContactsAreUnique(client, null);
        return clientRepository.save(client);
    }

    public Client getClientById(long clientId) {
        validateId(clientId);
        return findRequiredClient(clientId);
    }

    public List<Client> getAllClients() {
        return clientRepository.findAll();
    }

    public Client updateClient(
            long clientId,
            String firstName,
            String lastName,
            String phone,
            String email,
            LocalDate birthDate
    ) {
        validateId(clientId);
        findRequiredClient(clientId);

        Client updatedClient = buildValidatedClient(
                clientId,
                firstName,
                lastName,
                phone,
                email,
                birthDate
        );
        ensureContactsAreUnique(updatedClient, clientId);
        clientRepository.update(updatedClient);
        return updatedClient;
    }

    public void deleteClient(long clientId) {
        validateId(clientId);
        findRequiredClient(clientId);

        if (visitRepository.existsByClientId(clientId)) {
            throw new BusinessRuleException(
                    "Нельзя удалить клиента, у которого существуют посещения."
            );
        }
        clientRepository.deleteById(clientId);
    }

    public List<Client> searchByLastName(String lastName) {
        String normalizedLastName = requireNonEmpty(lastName, "Фамилия для поиска обязательна.");
        return clientRepository.findByLastName(normalizedLastName);
    }

    private Client buildValidatedClient(
            Long id,
            String firstName,
            String lastName,
            String phone,
            String email,
            LocalDate birthDate
    ) {
        String normalizedFirstName = validateName(firstName, "Имя");
        String normalizedLastName = validateName(lastName, "Фамилия");
        String normalizedPhone = requireNonEmpty(phone, "Телефон обязателен.");
        String normalizedEmail = normalizeEmail(email);
        validateBirthDate(birthDate);

        return new Client(
                id,
                normalizedFirstName,
                normalizedLastName,
                normalizedPhone,
                normalizedEmail,
                birthDate
        );
    }

    private String validateName(String value, String fieldName) {
        String normalized = requireNonEmpty(value, fieldName + " обязательно.");
        if (normalized.length() < 2) {
            throw new ValidationException(fieldName + " должно содержать минимум 2 символа.");
        }
        return normalized;
    }

    private String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            return null;
        }

        String normalized = email.trim();
        if (!EMAIL_PATTERN.matcher(normalized).matches()) {
            throw new ValidationException("Некорректный формат email.");
        }
        return normalized;
    }

    private void validateBirthDate(LocalDate birthDate) {
        if (birthDate == null) {
            throw new ValidationException("Дата рождения обязательна.");
        }
        if (birthDate.isAfter(LocalDate.now())) {
            throw new ValidationException("Дата рождения не может быть в будущем.");
        }
    }

    private void ensureContactsAreUnique(Client client, Long currentClientId) {
        clientRepository.findByPhone(client.getPhone())
                .filter(existing -> !Objects.equals(existing.getId(), currentClientId))
                .ifPresent(existing -> {
                    throw new BusinessRuleException("Клиент с таким телефоном уже существует.");
                });

        if (client.getEmail() != null) {
            clientRepository.findByEmail(client.getEmail())
                    .filter(existing -> !Objects.equals(existing.getId(), currentClientId))
                    .ifPresent(existing -> {
                        throw new BusinessRuleException("Клиент с таким email уже существует.");
                    });
        }
    }

    private Client findRequiredClient(long clientId) {
        return clientRepository.findById(clientId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Клиент с ID " + clientId + " не найден."
                ));
    }

    private String requireNonEmpty(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new ValidationException(message);
        }
        return value.trim();
    }

    private void validateId(long id) {
        if (id <= 0) {
            throw new ValidationException("ID клиента должен быть положительным.");
        }
    }
}
