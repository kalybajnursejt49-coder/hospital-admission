package bed;

import database.Sqlite;
import exception.BedUnavailableException;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

public class BedRepository {
    private final Path file;

    public BedRepository(Path file) {
        this.file = file;
        try (Connection connection = Sqlite.open(file); Statement statement = connection.createStatement()) {
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS bed_capacity (department TEXT PRIMARY KEY, free_count INTEGER NOT NULL)");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS equipment_stock (equipment TEXT PRIMARY KEY, free_count INTEGER NOT NULL)");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS bed_assignments (admission_id INTEGER PRIMARY KEY, department TEXT NOT NULL, equipment TEXT NOT NULL, status TEXT NOT NULL)");
            statement.executeUpdate("INSERT OR IGNORE INTO bed_capacity VALUES ('GENERAL', 3), ('ICU', 1)");
            statement.executeUpdate("INSERT OR IGNORE INTO equipment_stock VALUES ('BASIC', 10), ('MONITOR', 2)");
        } catch (SQLException e) {
            throw Sqlite.failure(e);
        }
    }

    public BedAssignment reserve(int admissionId, String department, String equipment) {
        String place = department.trim().toUpperCase();
        try (Connection connection = Sqlite.open(file)) {
            connection.setAutoCommit(false);
            try (PreparedStatement update = connection.prepareStatement("UPDATE bed_capacity SET free_count = free_count - 1 WHERE department = ? AND free_count > 0")) {
                update.setString(1, place);
                if (update.executeUpdate() == 0) {
                    connection.rollback();
                    throw new BedUnavailableException("Нет свободных коек в отделении " + place);
                }
            }
            try (PreparedStatement insert = connection.prepareStatement("INSERT INTO bed_assignments VALUES (?, ?, ?, 'RESERVED')")) {
                insert.setInt(1, admissionId);
                insert.setString(2, place);
                insert.setString(3, equipment);
                insert.executeUpdate();
            }
            connection.commit();
            return new BedAssignment(admissionId, place, equipment, BedAssignment.Status.RESERVED);
        } catch (SQLException e) {
            throw Sqlite.failure(e);
        }
    }

    public void assignEquipment(int admissionId) {
        try (Connection connection = Sqlite.open(file)) {
            connection.setAutoCommit(false);
            BedAssignment assignment = findByAdmissionId(connection, admissionId);
            if (assignment == null || assignment.getStatus() != BedAssignment.Status.RESERVED) {
                connection.rollback();
                throw new IllegalStateException("Койка не зарезервирована");
            }
            try (PreparedStatement update = connection.prepareStatement("UPDATE equipment_stock SET free_count = free_count - 1 WHERE equipment = ? AND free_count > 0")) {
                update.setString(1, assignment.getEquipment());
                if (update.executeUpdate() == 0) {
                    connection.rollback();
                    throw new BedUnavailableException("Нет оборудования " + assignment.getEquipment());
                }
            }
            try (PreparedStatement update = connection.prepareStatement("UPDATE bed_assignments SET status = 'ASSIGNED' WHERE admission_id = ?")) {
                update.setInt(1, admissionId);
                update.executeUpdate();
            }
            connection.commit();
        } catch (SQLException e) {
            throw Sqlite.failure(e);
        }
    }

    public void release(int admissionId) {
        try (Connection connection = Sqlite.open(file)) {
            connection.setAutoCommit(false);
            BedAssignment assignment = findByAdmissionId(connection, admissionId);
            if (assignment == null || assignment.getStatus() == BedAssignment.Status.RELEASED) {
                connection.rollback();
                return;
            }
            changeCount(connection, "bed_capacity", "department", assignment.getDepartment(), 1);
            if (assignment.getStatus() == BedAssignment.Status.ASSIGNED) {
                changeCount(connection, "equipment_stock", "equipment", assignment.getEquipment(), 1);
            }
            try (PreparedStatement update = connection.prepareStatement("UPDATE bed_assignments SET status = 'RELEASED' WHERE admission_id = ?")) {
                update.setInt(1, admissionId);
                update.executeUpdate();
            }
            connection.commit();
        } catch (SQLException e) {
            throw Sqlite.failure(e);
        }
    }

    public BedAssignment findByAdmissionId(int admissionId) {
        try (Connection connection = Sqlite.open(file)) {
            return findByAdmissionId(connection, admissionId);
        } catch (SQLException e) {
            throw Sqlite.failure(e);
        }
    }

    public ArrayList<BedAssignment> findAll() {
        ArrayList<BedAssignment> assignments = new ArrayList<>();
        try (Connection connection = Sqlite.open(file); Statement statement = connection.createStatement(); ResultSet rows = statement.executeQuery("SELECT * FROM bed_assignments ORDER BY admission_id")) {
            while (rows.next()) {
                assignments.add(read(rows));
            }
            return assignments;
        } catch (SQLException e) {
            throw Sqlite.failure(e);
        }
    }

    public void setFreeBeds(String department, int count) {
        if (department == null || department.trim().isEmpty() || count < 0) {
            throw new IllegalArgumentException("Неверное отделение или число коек");
        }
        try (Connection connection = Sqlite.open(file); PreparedStatement statement = connection.prepareStatement("INSERT INTO bed_capacity(department, free_count) VALUES (?, ?) ON CONFLICT(department) DO UPDATE SET free_count = excluded.free_count")) {
            statement.setString(1, department.trim().toUpperCase());
            statement.setInt(2, count);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw Sqlite.failure(e);
        }
    }

    public int getFreeBeds(String department) {
        try (Connection connection = Sqlite.open(file); PreparedStatement statement = connection.prepareStatement("SELECT free_count FROM bed_capacity WHERE department = ?")) {
            statement.setString(1, department.trim().toUpperCase());
            try (ResultSet rows = statement.executeQuery()) {
                return rows.next() ? rows.getInt(1) : 0;
            }
        } catch (SQLException e) {
            throw Sqlite.failure(e);
        }
    }

    public void reset() {
        try (Connection connection = Sqlite.open(file); Statement statement = connection.createStatement()) {
            statement.executeUpdate("DELETE FROM bed_assignments");
            statement.executeUpdate("DELETE FROM bed_capacity");
            statement.executeUpdate("DELETE FROM equipment_stock");
            statement.executeUpdate("INSERT INTO bed_capacity VALUES ('GENERAL', 3), ('ICU', 1)");
            statement.executeUpdate("INSERT INTO equipment_stock VALUES ('BASIC', 10), ('MONITOR', 2)");
        } catch (SQLException e) {
            throw Sqlite.failure(e);
        }
    }

    private BedAssignment findByAdmissionId(Connection connection, int admissionId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("SELECT * FROM bed_assignments WHERE admission_id = ?")) {
            statement.setInt(1, admissionId);
            try (ResultSet rows = statement.executeQuery()) {
                return rows.next() ? read(rows) : null;
            }
        }
    }

    private BedAssignment read(ResultSet rows) throws SQLException {
        return new BedAssignment(rows.getInt("admission_id"), rows.getString("department"), rows.getString("equipment"), BedAssignment.Status.valueOf(rows.getString("status")));
    }

    private void changeCount(Connection connection, String table, String column, String value, int amount) throws SQLException {
        String sql = "UPDATE " + table + " SET free_count = free_count + ? WHERE " + column + " = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, amount);
            statement.setString(2, value);
            statement.executeUpdate();
        }
    }
}
