package ru.mirea.pool.infrastructure.database;

import ru.mirea.pool.domain.model.SystemUser;
import ru.mirea.pool.domain.model.UserRole;
import ru.mirea.pool.domain.repository.SystemUserRepository;
import ru.mirea.pool.infrastructure.exception.DatabaseException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class JdbcSystemUserRepository implements SystemUserRepository {

    private static final String SELECT_COLUMNS =
            "id, username, password_hash, role, active, created_at";

    private final DatabaseManager databaseManager;

    public JdbcSystemUserRepository(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    @Override
    public SystemUser save(SystemUser user) {
        String sql = """
                INSERT INTO system_users (username, password_hash, role, active, created_at)
                VALUES (?, ?, ?, ?, ?)
                """;

        LocalDateTime createdAt = user.getCreatedAt() == null
                ? LocalDateTime.now()
                : user.getCreatedAt();

        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     sql,
                     Statement.RETURN_GENERATED_KEYS
             )) {
            statement.setString(1, user.getUsername());
            statement.setString(2, user.getPasswordHash());
            statement.setString(3, user.getRole().name());
            statement.setBoolean(4, user.isActive());
            statement.setTimestamp(5, Timestamp.valueOf(createdAt));
            statement.executeUpdate();

            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new DatabaseException("База данных не вернула ID пользователя.");
                }
                user.setId(keys.getLong(1));
                user.setCreatedAt(createdAt);
                return user;
            }
        } catch (SQLException e) {
            throw databaseError("Не удалось сохранить системного пользователя.", e);
        }
    }

    @Override
    public Optional<SystemUser> findById(long id) {
        String sql = "SELECT " + SELECT_COLUMNS + " FROM system_users WHERE id = ?";

        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next()
                        ? Optional.of(mapUser(resultSet))
                        : Optional.empty();
            }
        } catch (SQLException e) {
            throw databaseError("Не удалось найти системного пользователя по ID.", e);
        }
    }

    @Override
    public Optional<SystemUser> findByUsername(String username) {
        String sql = "SELECT " + SELECT_COLUMNS + " FROM system_users WHERE username = ?";

        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, username);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next()
                        ? Optional.of(mapUser(resultSet))
                        : Optional.empty();
            }
        } catch (SQLException e) {
            throw databaseError("Не удалось найти системного пользователя по username.", e);
        }
    }

    @Override
    public List<SystemUser> findAll() {
        String sql = "SELECT " + SELECT_COLUMNS + " FROM system_users ORDER BY id";
        List<SystemUser> users = new ArrayList<>();

        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                users.add(mapUser(resultSet));
            }
            return users;
        } catch (SQLException e) {
            throw databaseError("Не удалось получить список системных пользователей.", e);
        }
    }

    @Override
    public void update(SystemUser user) {
        String sql = """
                UPDATE system_users
                SET username = ?, password_hash = ?, role = ?, active = ?
                WHERE id = ?
                """;

        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, user.getUsername());
            statement.setString(2, user.getPasswordHash());
            statement.setString(3, user.getRole().name());
            statement.setBoolean(4, user.isActive());
            statement.setLong(5, user.getId());
            statement.executeUpdate();
        } catch (SQLException e) {
            throw databaseError("Не удалось обновить системного пользователя.", e);
        }
    }

    @Override
    public boolean existsByUsername(String username) {
        String sql = "SELECT 1 FROM system_users WHERE username = ?";

        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, username);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        } catch (SQLException e) {
            throw databaseError("Не удалось проверить существование username.", e);
        }
    }

    @Override
    public long countActiveAdmins() {
        String sql = """
                SELECT COUNT(*)
                FROM system_users
                WHERE active = TRUE AND role = 'ADMIN'
                """;

        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            resultSet.next();
            return resultSet.getLong(1);
        } catch (SQLException e) {
            throw databaseError("Не удалось подсчитать активных администраторов.", e);
        }
    }

    private SystemUser mapUser(ResultSet resultSet) throws SQLException {
        return new SystemUser(
                resultSet.getLong("id"),
                resultSet.getString("username"),
                resultSet.getString("password_hash"),
                UserRole.valueOf(resultSet.getString("role")),
                resultSet.getBoolean("active"),
                resultSet.getTimestamp("created_at").toLocalDateTime()
        );
    }

    private DatabaseException databaseError(String message, SQLException cause) {
        return new DatabaseException(message, cause);
    }
}
