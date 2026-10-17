package app;

import facade.AdminFacade;
import facade.AdmissionFacade;

import java.nio.file.Path;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Path databaseFile = Path.of("data", "hospital.db");
        AdmissionFacade admissions = new AdmissionFacade(databaseFile);
        AdminFacade admin = new AdminFacade(databaseFile, admissions);
        int recovered = admissions.recoverUnfinished();
        System.out.println("Восстановлено незавершённых цепочек: " + recovered);
        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.println("\n1. Принять пациента");
            System.out.println("2. Показать все таблицы");
            System.out.println("3. Изменить число коек");
            System.out.println("4. Изменить остаток препарата");
            System.out.println("5. Ручная компенсация по id");
            System.out.println("6. Сброс БД");
            System.out.println("7. Выход");
            System.out.print("Выбор: ");
            String choice = scanner.nextLine();
            if (choice.equals("7")) {
                break;
            }
            try {
                if (choice.equals("1")) {
                    System.out.print("Имя пациента: ");
                    String name = scanner.nextLine();
                    System.out.print("Страховой номер (INS-...): ");
                    String insurance = scanner.nextLine();
                    System.out.print("Отделение (GENERAL или ICU): ");
                    String department = scanner.nextLine();
                    System.out.print("Препарат: ");
                    String medicine = scanner.nextLine();
                    int id = admissions.admit(name, insurance, department, medicine);
                    System.out.println("Пациент принят, id = " + id);
                } else if (choice.equals("2")) {
                    admin.printTables();
                } else if (choice.equals("3")) {
                    System.out.print("Отделение: ");
                    String department = scanner.nextLine();
                    System.out.print("Свободных коек: ");
                    int count = Integer.parseInt(scanner.nextLine());
                    admin.setFreeBeds(department, count);
                    System.out.println("Число коек изменено");
                } else if (choice.equals("4")) {
                    System.out.print("Препарат: ");
                    String medicine = scanner.nextLine();
                    System.out.print("Остаток: ");
                    int count = Integer.parseInt(scanner.nextLine());
                    admin.setMedicineStock(medicine, count);
                    System.out.println("Остаток изменён");
                } else if (choice.equals("5")) {
                    System.out.print("id пациента: ");
                    int id = Integer.parseInt(scanner.nextLine());
                    admin.compensate(id);
                    System.out.println("Компенсация выполнена");
                } else if (choice.equals("6")) {
                    admin.reset();
                    System.out.println("БД сброшена");
                } else {
                    System.out.println("Неизвестный пункт");
                }
            } catch (RuntimeException e) {
                System.out.println("Ошибка: " + e.getMessage());
            }
        }
    }
}
