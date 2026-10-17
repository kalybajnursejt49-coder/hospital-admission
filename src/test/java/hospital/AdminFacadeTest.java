package hospital;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AdminFacadeTest {
    @TempDir Path folder;

    @Test
    void printTablesShowsStoredRowsAndResetRestoresDefaults() {
        HospitalTestKit kit = new HospitalTestKit(folder).givenPatient("Aibek");
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        PrintStream before = System.out;
        try {
            System.setOut(new PrintStream(output));
            kit.admin().printTables();
        } finally {
            System.setOut(before);
        }
        String text = output.toString();
        assertTrue(text.contains("Aibek"));
        assertTrue(text.contains("COMPLETED"));
        assertTrue(text.contains("PHARMACY"));
        kit.admin().reset();
        assertTrue(kit.registrations().findAll().isEmpty());
        assertTrue(kit.beds().findAll().isEmpty());
        assertTrue(kit.orders().findAll().isEmpty());
        assertTrue(kit.chains().findAll().isEmpty());
        assertEquals(3, kit.beds().getFreeBeds("GENERAL"));
        assertEquals(5, kit.orders().getMedicineStock("paracetamol"));
    }

    @Test
    void compensationRequiresExistingChain() {
        HospitalTestKit kit = new HospitalTestKit(folder);
        assertThrows(IllegalArgumentException.class, () -> kit.admin().compensate(99));
    }
}
