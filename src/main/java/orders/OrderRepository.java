package orders;

import database.Sqlite;
import exception.PharmacyOutOfStockException;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

public class OrderRepository {
    private final Path file;

    public OrderRepository(Path file) {
        this.file = file;
        try (Connection connection = Sqlite.open(file); Statement statement = connection.createStatement()) {
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS medicine_stock (medicine TEXT PRIMARY KEY, available_count INTEGER NOT NULL)");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS hospital_orders (id INTEGER PRIMARY KEY AUTOINCREMENT, admission_id INTEGER NOT NULL, order_type TEXT NOT NULL, medicine TEXT NOT NULL, status TEXT NOT NULL)");
            statement.executeUpdate("INSERT OR IGNORE INTO medicine_stock VALUES ('paracetamol', 5)");
        } catch (SQLException e) {
            throw Sqlite.failure(e);
        }
    }

    public HospitalOrder saveOrder(int admissionId, String type, String medicine) {
        String sql = "INSERT INTO hospital_orders(admission_id, order_type, medicine, status) VALUES (?, ?, ?, 'CREATED')";
        try (Connection connection = Sqlite.open(file); PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setInt(1, admissionId);
            statement.setString(2, type);
            statement.setString(3, medicine);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                keys.next();
                return new HospitalOrder(keys.getInt(1), admissionId, type, medicine, HospitalOrder.Status.CREATED);
            }
        } catch (SQLException e) {
            throw Sqlite.failure(e);
        }
    }

    public void markSent(int id) {
        try (Connection connection = Sqlite.open(file)) {
            HospitalOrder order;
            try (PreparedStatement find = connection.prepareStatement("SELECT * FROM hospital_orders WHERE id = ?")) {
                find.setInt(1, id);
                try (ResultSet rows = find.executeQuery()) {
                    if (!rows.next()) {
                        throw new IllegalArgumentException("Заказ не найден: " + id);
                    }
                    order = read(rows);
                }
            }
            order.send();
            try (PreparedStatement update = connection.prepareStatement("UPDATE hospital_orders SET status = ? WHERE id = ? AND status = 'CREATED'")) {
                update.setString(1, order.getStatus().name());
                update.setInt(2, id);
                update.executeUpdate();
            }
        } catch (SQLException e) {
            throw Sqlite.failure(e);
        }
    }

    public HospitalOrder sendPharmacyOrder(int admissionId, String medicine) {
        String name = medicine.trim().toLowerCase();
        try (Connection connection = Sqlite.open(file)) {
            connection.setAutoCommit(false);
            try (PreparedStatement update = connection.prepareStatement("UPDATE medicine_stock SET available_count = available_count - 1 WHERE medicine = ? AND available_count > 0")) {
                update.setString(1, name);
                if (update.executeUpdate() == 0) {
                    connection.rollback();
                    throw new PharmacyOutOfStockException("Препарат недоступен: " + name);
                }
            }
            int id;
            HospitalOrder order = new HospitalOrder(0, admissionId, "PHARMACY", name, HospitalOrder.Status.CREATED);
            order.send();
            try (PreparedStatement insert = connection.prepareStatement("INSERT INTO hospital_orders(admission_id, order_type, medicine, status) VALUES (?, 'PHARMACY', ?, ?)", Statement.RETURN_GENERATED_KEYS)) {
                insert.setInt(1, admissionId);
                insert.setString(2, name);
                insert.setString(3, order.getStatus().name());
                insert.executeUpdate();
                try (ResultSet keys = insert.getGeneratedKeys()) {
                    keys.next();
                    id = keys.getInt(1);
                }
            }
            connection.commit();
            order.setId(id);
            return order;
        } catch (SQLException e) {
            throw Sqlite.failure(e);
        }
    }

    public void cancelByAdmissionId(int admissionId) {
        try (Connection connection = Sqlite.open(file)) {
            connection.setAutoCommit(false);
            ArrayList<HospitalOrder> orders = new ArrayList<>();
            try (PreparedStatement find = connection.prepareStatement("SELECT * FROM hospital_orders WHERE admission_id = ? AND status <> 'CANCELLED'")) {
                find.setInt(1, admissionId);
                try (ResultSet rows = find.executeQuery()) {
                    while (rows.next()) {
                        orders.add(read(rows));
                    }
                }
            }
            for (HospitalOrder order : orders) {
                if (order.getType().equals("PHARMACY") && order.getStatus() == HospitalOrder.Status.SENT) {
                    try (PreparedStatement restore = connection.prepareStatement("UPDATE medicine_stock SET available_count = available_count + 1 WHERE medicine = ?")) {
                        restore.setString(1, order.getMedicine());
                        restore.executeUpdate();
                    }
                }
                order.cancel();
                try (PreparedStatement update = connection.prepareStatement("UPDATE hospital_orders SET status = ? WHERE id = ?")) {
                    update.setString(1, order.getStatus().name());
                    update.setInt(2, order.getId());
                    update.executeUpdate();
                }
            }
            connection.commit();
        } catch (SQLException e) {
            throw Sqlite.failure(e);
        }
    }

    public ArrayList<HospitalOrder> findByAdmissionId(int admissionId) {
        ArrayList<HospitalOrder> orders = new ArrayList<>();
        try (Connection connection = Sqlite.open(file); PreparedStatement statement = connection.prepareStatement("SELECT * FROM hospital_orders WHERE admission_id = ? ORDER BY id")) {
            statement.setInt(1, admissionId);
            try (ResultSet rows = statement.executeQuery()) {
                while (rows.next()) {
                    orders.add(read(rows));
                }
            }
            return orders;
        } catch (SQLException e) {
            throw Sqlite.failure(e);
        }
    }

    public ArrayList<HospitalOrder> findAll() {
        ArrayList<HospitalOrder> orders = new ArrayList<>();
        try (Connection connection = Sqlite.open(file); Statement statement = connection.createStatement(); ResultSet rows = statement.executeQuery("SELECT * FROM hospital_orders ORDER BY id")) {
            while (rows.next()) {
                orders.add(read(rows));
            }
            return orders;
        } catch (SQLException e) {
            throw Sqlite.failure(e);
        }
    }

    public int getMedicineStock(String medicine) {
        if (medicine == null || medicine.trim().isEmpty()) {
            return 0;
        }
        try (Connection connection = Sqlite.open(file); PreparedStatement statement = connection.prepareStatement("SELECT available_count FROM medicine_stock WHERE medicine = ?")) {
            statement.setString(1, medicine.trim().toLowerCase());
            try (ResultSet rows = statement.executeQuery()) {
                return rows.next() ? rows.getInt(1) : 0;
            }
        } catch (SQLException e) {
            throw Sqlite.failure(e);
        }
    }

    public void setMedicineStock(String medicine, int count) {
        if (medicine == null || medicine.trim().isEmpty() || count < 0) {
            throw new IllegalArgumentException("Неверный препарат или остаток");
        }
        try (Connection connection = Sqlite.open(file); PreparedStatement statement = connection.prepareStatement("INSERT INTO medicine_stock(medicine, available_count) VALUES (?, ?) ON CONFLICT(medicine) DO UPDATE SET available_count = excluded.available_count")) {
            statement.setString(1, medicine.trim().toLowerCase());
            statement.setInt(2, count);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw Sqlite.failure(e);
        }
    }

    public void reset() {
        try (Connection connection = Sqlite.open(file); Statement statement = connection.createStatement()) {
            statement.executeUpdate("DELETE FROM hospital_orders");
            statement.executeUpdate("DELETE FROM sqlite_sequence WHERE name = 'hospital_orders'");
            statement.executeUpdate("DELETE FROM medicine_stock");
            statement.executeUpdate("INSERT INTO medicine_stock VALUES ('paracetamol', 5)");
        } catch (SQLException e) {
            throw Sqlite.failure(e);
        }
    }

    private HospitalOrder read(ResultSet rows) throws SQLException {
        return new HospitalOrder(rows.getInt("id"), rows.getInt("admission_id"), rows.getString("order_type"), rows.getString("medicine"), HospitalOrder.Status.valueOf(rows.getString("status")));
    }
}
