package ru.mirea.pool.infrastructure.database;

import ru.mirea.pool.domain.model.Visit;
import ru.mirea.pool.domain.model.VisitStatus;
import ru.mirea.pool.domain.repository.VisitRepository;
import ru.mirea.pool.infrastructure.exception.DatabaseException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class JdbcVisitRepository implements VisitRepository {

    private static final String SELECT_COLUMNS = """
            id, client_id, visit_date, start_time, duration_minutes,
            lane_number, status, created_by
            """;

    private final DatabaseManager databaseManager;

    public JdbcVisitRepository(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    @Override
    public Visit save(Visit visit) {
        String sql = """
                INSERT INTO visits (
                    client_id, visit_date, start_time, duration_minutes,
                    lane_number, status, created_by
                )
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;

        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     sql,
                     Statement.RETURN_GENERATED_KEYS
             )) {
            setVisitFields(statement, visit);
            statement.executeUpdate();

            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new DatabaseException("База данных не вернула ID посещения.");
                }
                visit.setId(keys.getLong(1));
                return visit;
            }
        } catch (SQLException e) {
            throw databaseError("Не удалось сохранить посещение.", e);
        }
    }

    @Override
    public Optional<Visit> findById(long id) {
        String sql = "SELECT " + SELECT_COLUMNS + " FROM visits WHERE id = ?";

        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next()
                        ? Optional.of(mapVisit(resultSet))
                        : Optional.empty();
            }
        } catch (SQLException e) {
            throw databaseError("Не удалось найти посещение по ID.", e);
        }
    }

    @Override
    public List<Visit> findAll() {
        String sql = "SELECT " + SELECT_COLUMNS
                + " FROM visits ORDER BY visit_date, start_time, id";
        return findMany(sql, null, null);
    }

    @Override
    public void update(Visit visit) {
        String sql = """
                UPDATE visits
                SET client_id = ?, visit_date = ?, start_time = ?, duration_minutes = ?,
                    lane_number = ?, status = ?, created_by = ?
                WHERE id = ?
                """;

        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            setVisitFields(statement, visit);
            statement.setLong(8, visit.getId());
            statement.executeUpdate();
        } catch (SQLException e) {
            throw databaseError("Не удалось обновить посещение.", e);
        }
    }

    @Override
    public void deleteById(long id) {
        String sql = "DELETE FROM visits WHERE id = ?";

        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw databaseError("Не удалось удалить посещение.", e);
        }
    }

    @Override
    public int refreshStatuses(LocalDateTime now) {
        String completeSql = """
                UPDATE visits SET status = 'COMPLETED'
                WHERE status IN ('PLANNED', 'IN_PROGRESS')
                  AND visit_date + start_time + duration_minutes * INTERVAL '1 minute' <= ?
                """;
        String startSql = """
                UPDATE visits SET status = 'IN_PROGRESS'
                WHERE status = 'PLANNED'
                  AND visit_date + start_time <= ?
                  AND visit_date + start_time + duration_minutes * INTERVAL '1 minute' > ?
                """;

        try (Connection connection = databaseManager.getConnection();
             PreparedStatement complete = connection.prepareStatement(completeSql);
             PreparedStatement start = connection.prepareStatement(startSql)) {
            complete.setObject(1, now);
            int completed = complete.executeUpdate();
            start.setObject(1, now);
            start.setObject(2, now);
            return completed + start.executeUpdate();
        } catch (SQLException e) {
            throw databaseError("Не удалось актуализировать статусы посещений.", e);
        }
    }

    @Override
    public List<Visit> findByClientId(long clientId) {
        String sql = "SELECT " + SELECT_COLUMNS
                + " FROM visits WHERE client_id = ? ORDER BY visit_date, start_time, id";
        return findMany(sql, clientId, null);
    }

    @Override
    public List<Visit> findByDate(LocalDate date) {
        String sql = "SELECT " + SELECT_COLUMNS
                + " FROM visits WHERE visit_date = ? ORDER BY start_time, id";
        return findMany(sql, null, date);
    }

    @Override
    public List<Visit> findByStatus(VisitStatus status) {
        String sql = "SELECT " + SELECT_COLUMNS
                + " FROM visits WHERE status = ? ORDER BY visit_date, start_time, id";
        List<Visit> visits = new ArrayList<>();

        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, status.name());
            readVisits(statement, visits);
            return visits;
        } catch (SQLException e) {
            throw databaseError("Не удалось отфильтровать посещения по статусу.", e);
        }
    }

    @Override
    public List<Visit> findByDateRange(LocalDate from, LocalDate to) {
        String sql = "SELECT " + SELECT_COLUMNS + """
                 FROM visits
                 WHERE visit_date BETWEEN ? AND ?
                 ORDER BY visit_date, start_time, id
                """;
        List<Visit> visits = new ArrayList<>();

        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, from);
            statement.setObject(2, to);
            readVisits(statement, visits);
            return visits;
        } catch (SQLException e) {
            throw databaseError("Не удалось отфильтровать посещения по диапазону дат.", e);
        }
    }

    @Override
    public List<Visit> findByClientIdAndDate(long clientId, LocalDate date) {
        String sql = "SELECT " + SELECT_COLUMNS + """
                 FROM visits
                 WHERE client_id = ? AND visit_date = ?
                 ORDER BY start_time, id
                """;
        List<Visit> visits = new ArrayList<>();

        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, clientId);
            statement.setObject(2, date);
            readVisits(statement, visits);
            return visits;
        } catch (SQLException e) {
            throw databaseError("Не удалось найти посещения клиента за дату.", e);
        }
    }

    @Override
    public boolean existsByClientId(long clientId) {
        String sql = "SELECT 1 FROM visits WHERE client_id = ?";

        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, clientId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        } catch (SQLException e) {
            throw databaseError("Не удалось проверить посещения клиента.", e);
        }
    }

    private List<Visit> findMany(String sql, Long clientId, LocalDate date) {
        List<Visit> visits = new ArrayList<>();

        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            if (clientId != null) {
                statement.setLong(1, clientId);
            } else if (date != null) {
                statement.setObject(1, date);
            }
            readVisits(statement, visits);
            return visits;
        } catch (SQLException e) {
            throw databaseError("Не удалось получить список посещений.", e);
        }
    }

    private void readVisits(PreparedStatement statement, List<Visit> target) throws SQLException {
        try (ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                target.add(mapVisit(resultSet));
            }
        }
    }

    private void setVisitFields(PreparedStatement statement, Visit visit) throws SQLException {
        statement.setLong(1, visit.getClientId());
        statement.setObject(2, visit.getVisitDate());
        statement.setObject(3, visit.getStartTime());
        statement.setInt(4, visit.getDurationMinutes());
        statement.setInt(5, visit.getLaneNumber());
        statement.setString(6, visit.getStatus().name());

        if (visit.getCreatedByUserId() == null) {
            statement.setNull(7, Types.BIGINT);
        } else {
            statement.setLong(7, visit.getCreatedByUserId());
        }
    }

    private Visit mapVisit(ResultSet resultSet) throws SQLException {
        return new Visit(
                resultSet.getLong("id"),
                resultSet.getLong("client_id"),
                resultSet.getObject("visit_date", LocalDate.class),
                resultSet.getObject("start_time", java.time.LocalTime.class),
                resultSet.getInt("duration_minutes"),
                resultSet.getInt("lane_number"),
                VisitStatus.valueOf(resultSet.getString("status")),
                resultSet.getObject("created_by", Long.class)
        );
    }

    private DatabaseException databaseError(String message, SQLException cause) {
        return new DatabaseException(message, cause);
    }
}
