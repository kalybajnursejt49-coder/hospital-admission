package database;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class Sqlite {
    public static Connection open(Path file) throws SQLException {
        try {
            Path parent = file.toAbsolutePath().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
        } catch (java.io.IOException e) {
            throw new IllegalStateException("Не удалось создать папку БД", e);
        }
        return DriverManager.getConnection("jdbc:sqlite:" + file.toAbsolutePath());
    }

    public static IllegalStateException failure(SQLException e) {
        return new IllegalStateException("Ошибка SQLite: " + e.getMessage(), e);
    }
}
