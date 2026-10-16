package facade;

import database.Sqlite;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

public class ChainRepository {
    private final Path file;

    public ChainRepository(Path file) {
        this.file = file;
        try (Connection connection = Sqlite.open(file); Statement statement = connection.createStatement()) {
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS admission_chain (admission_id INTEGER PRIMARY KEY, status TEXT NOT NULL)");
        } catch (SQLException e) {
            throw Sqlite.failure(e);
        }
    }

    public void savePendingB(int admissionId) {
        try (Connection connection = Sqlite.open(file); PreparedStatement statement = connection.prepareStatement("INSERT INTO admission_chain VALUES (?, 'PENDING_B')")) {
            statement.setInt(1, admissionId);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw Sqlite.failure(e);
        }
    }

    public void setStatus(int admissionId, ChainStatus status) {
        try (Connection connection = Sqlite.open(file); PreparedStatement statement = connection.prepareStatement("UPDATE admission_chain SET status = ? WHERE admission_id = ?")) {
            statement.setString(1, status.name());
            statement.setInt(2, admissionId);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw Sqlite.failure(e);
        }
    }

    public ChainStatus getStatus(int admissionId) {
        try (Connection connection = Sqlite.open(file); PreparedStatement statement = connection.prepareStatement("SELECT status FROM admission_chain WHERE admission_id = ?")) {
            statement.setInt(1, admissionId);
            try (ResultSet rows = statement.executeQuery()) {
                return rows.next() ? ChainStatus.valueOf(rows.getString(1)) : null;
            }
        } catch (SQLException e) {
            throw Sqlite.failure(e);
        }
    }

    public ArrayList<ChainRecord> findUnfinished() {
        return find(" WHERE status IN ('PENDING_B', 'PENDING_C')");
    }

    public ArrayList<ChainRecord> findAll() {
        return find("");
    }

    public void reset() {
        try (Connection connection = Sqlite.open(file); Statement statement = connection.createStatement()) {
            statement.executeUpdate("DELETE FROM admission_chain");
        } catch (SQLException e) {
            throw Sqlite.failure(e);
        }
    }

    private ArrayList<ChainRecord> find(String condition) {
        ArrayList<ChainRecord> chains = new ArrayList<>();
        try (Connection connection = Sqlite.open(file); Statement statement = connection.createStatement(); ResultSet rows = statement.executeQuery("SELECT * FROM admission_chain" + condition + " ORDER BY admission_id")) {
            while (rows.next()) {
                chains.add(new ChainRecord(rows.getInt("admission_id"), ChainStatus.valueOf(rows.getString("status"))));
            }
            return chains;
        } catch (SQLException e) {
            throw Sqlite.failure(e);
        }
    }
}
