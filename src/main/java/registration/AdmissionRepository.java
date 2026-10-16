package registration;

import database.Sqlite;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

public class AdmissionRepository {
    private final Path file;

    public AdmissionRepository(Path file) {
        this.file = file;
        try (Connection connection = Sqlite.open(file); Statement statement = connection.createStatement()) {
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS admissions (id INTEGER PRIMARY KEY AUTOINCREMENT, patient_name TEXT NOT NULL, insurance_number TEXT NOT NULL, status TEXT NOT NULL)");
        } catch (SQLException e) {
            throw Sqlite.failure(e);
        }
    }

    public Admission saveAdmission(String name, String insuranceNumber) {
        String sql = "INSERT INTO admissions(patient_name, insurance_number, status) VALUES(?, ?, ?)";
        try (Connection connection = Sqlite.open(file); PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, name);
            statement.setString(2, insuranceNumber);
            statement.setString(3, Admission.Status.REGISTERED.name());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                keys.next();
                return new Admission(keys.getInt(1), name, insuranceNumber, Admission.Status.REGISTERED);
            }
        } catch (SQLException e) {
            throw Sqlite.failure(e);
        }
    }

    public Admission findById(int id) {
        try (Connection connection = Sqlite.open(file); PreparedStatement statement = connection.prepareStatement("SELECT * FROM admissions WHERE id = ?")) {
            statement.setInt(1, id);
            try (ResultSet rows = statement.executeQuery()) {
                if (rows.next()) {
                    return read(rows);
                }
                return null;
            }
        } catch (SQLException e) {
            throw Sqlite.failure(e);
        }
    }

    public ArrayList<Admission> findAll() {
        ArrayList<Admission> admissions = new ArrayList<>();
        try (Connection connection = Sqlite.open(file); Statement statement = connection.createStatement(); ResultSet rows = statement.executeQuery("SELECT * FROM admissions ORDER BY id")) {
            while (rows.next()) {
                admissions.add(read(rows));
            }
            return admissions;
        } catch (SQLException e) {
            throw Sqlite.failure(e);
        }
    }

    public void cancelAdmission(int id) {
        try (Connection connection = Sqlite.open(file); PreparedStatement statement = connection.prepareStatement("UPDATE admissions SET status = 'CANCELLED' WHERE id = ? AND status = 'REGISTERED'")) {
            statement.setInt(1, id);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw Sqlite.failure(e);
        }
    }

    public void reset() {
        try (Connection connection = Sqlite.open(file); Statement statement = connection.createStatement()) {
            statement.executeUpdate("DELETE FROM admissions");
            statement.executeUpdate("DELETE FROM sqlite_sequence WHERE name = 'admissions'");
        } catch (SQLException e) {
            throw Sqlite.failure(e);
        }
    }

    private Admission read(ResultSet rows) throws SQLException {
        return new Admission(rows.getInt("id"), rows.getString("patient_name"), rows.getString("insurance_number"), Admission.Status.valueOf(rows.getString("status")));
    }
}
