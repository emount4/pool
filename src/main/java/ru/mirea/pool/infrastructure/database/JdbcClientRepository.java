package ru.mirea.pool.infrastructure.database;

import ru.mirea.pool.domain.model.Client;
import ru.mirea.pool.domain.repository.ClientRepository;
import ru.mirea.pool.infrastructure.exception.DatabaseException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class JdbcClientRepository implements ClientRepository {

    private static final String SELECT_COLUMNS =
            "id, first_name, last_name, phone, email, birth_date";

    private final DatabaseManager databaseManager;

    public JdbcClientRepository(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    @Override
    public Client save(Client client) {
        String sql = """
                INSERT INTO clients (first_name, last_name, phone, email, birth_date)
                VALUES (?, ?, ?, ?, ?)
                """;

        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     sql,
                     Statement.RETURN_GENERATED_KEYS
             )) {
            setClientFields(statement, client);
            statement.executeUpdate();

            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new DatabaseException("База данных не вернула ID клиента.");
                }
                client.setId(keys.getLong(1));
                return client;
            }
        } catch (SQLException e) {
            throw databaseError("Не удалось сохранить клиента.", e);
        }
    }

    @Override
    public Optional<Client> findById(long id) {
        return findOne("SELECT " + SELECT_COLUMNS + " FROM clients WHERE id = ?", id);
    }

    @Override
    public List<Client> findAll() {
        String sql = "SELECT " + SELECT_COLUMNS + " FROM clients ORDER BY id";
        List<Client> clients = new ArrayList<>();

        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                clients.add(mapClient(resultSet));
            }
            return clients;
        } catch (SQLException e) {
            throw databaseError("Не удалось получить список клиентов.", e);
        }
    }

    @Override
    public void update(Client client) {
        String sql = """
                UPDATE clients
                SET first_name = ?, last_name = ?, phone = ?, email = ?, birth_date = ?
                WHERE id = ?
                """;

        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            setClientFields(statement, client);
            statement.setLong(6, client.getId());
            statement.executeUpdate();
        } catch (SQLException e) {
            throw databaseError("Не удалось обновить клиента.", e);
        }
    }

    @Override
    public void deleteById(long id) {
        String sql = "DELETE FROM clients WHERE id = ?";

        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw databaseError("Не удалось удалить клиента.", e);
        }
    }

    @Override
    public Optional<Client> findByPhone(String phone) {
        return findOne("SELECT " + SELECT_COLUMNS + " FROM clients WHERE phone = ?", phone);
    }

    @Override
    public Optional<Client> findByEmail(String email) {
        return findOne("SELECT " + SELECT_COLUMNS + " FROM clients WHERE email = ?", email);
    }

    @Override
    public List<Client> findByLastName(String lastName) {
        String sql = """
                SELECT id, first_name, last_name, phone, email, birth_date
                FROM clients
                WHERE LOWER(last_name) LIKE LOWER(?)
                ORDER BY last_name, first_name, id
                """;
        List<Client> clients = new ArrayList<>();

        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, "%" + lastName + "%");
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    clients.add(mapClient(resultSet));
                }
            }
            return clients;
        } catch (SQLException e) {
            throw databaseError("Не удалось выполнить поиск клиентов по фамилии.", e);
        }
    }

    @Override
    public boolean existsById(long id) {
        String sql = "SELECT 1 FROM clients WHERE id = ?";

        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        } catch (SQLException e) {
            throw databaseError("Не удалось проверить существование клиента.", e);
        }
    }

    private Optional<Client> findOne(String sql, long value) {
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, value);
            return readSingleClient(statement);
        } catch (SQLException e) {
            throw databaseError("Не удалось найти клиента.", e);
        }
    }

    private Optional<Client> findOne(String sql, String value) {
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, value);
            return readSingleClient(statement);
        } catch (SQLException e) {
            throw databaseError("Не удалось найти клиента.", e);
        }
    }

    private Optional<Client> readSingleClient(PreparedStatement statement) throws SQLException {
        try (ResultSet resultSet = statement.executeQuery()) {
            return resultSet.next()
                    ? Optional.of(mapClient(resultSet))
                    : Optional.empty();
        }
    }

    private void setClientFields(PreparedStatement statement, Client client) throws SQLException {
        statement.setString(1, client.getFirstName());
        statement.setString(2, client.getLastName());
        statement.setString(3, client.getPhone());
        statement.setString(4, client.getEmail());
        statement.setObject(5, client.getBirthDate());
    }

    private Client mapClient(ResultSet resultSet) throws SQLException {
        return new Client(
                resultSet.getLong("id"),
                resultSet.getString("first_name"),
                resultSet.getString("last_name"),
                resultSet.getString("phone"),
                resultSet.getString("email"),
                resultSet.getObject("birth_date", java.time.LocalDate.class)
        );
    }

    private DatabaseException databaseError(String message, SQLException cause) {
        return new DatabaseException(message, cause);
    }
}
