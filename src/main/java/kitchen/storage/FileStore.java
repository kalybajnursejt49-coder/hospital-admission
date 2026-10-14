package kitchen.storage;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class FileStore {
    private final Path file;

    public FileStore(Path file) {
        this.file = file;
        try {
            Files.createDirectories(file.toAbsolutePath().getParent());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public boolean exists() {
        return Files.exists(file);
    }

    public List<String> readLines() {
        if (!Files.exists(file)) {
            return new ArrayList<>();
        }
        try {
            return new ArrayList<>(Files.readAllLines(file));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void writeLines(List<String> lines) {
        try {
            Files.write(file, lines);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
