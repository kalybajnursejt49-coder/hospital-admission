# Hospital Admission

Консольное приложение на Java 17, Maven и SQLite JDBC. `AdmissionFacade` объединяет регистрацию (A), койку и оборудование (B), лабораторию и аптеку (C). Подсистемы не импортируют друг друга.

## Запуск и проверка

Требуются Java 17+ и Maven. Первый запуск: `mvn compile exec:java`; далее: `mvn exec:java`. Тесты: `mvn test`. Полная проверка: `mvn clean verify`.

JaCoCo в `pom.xml`: `prepare-agent` собирает покрытие, `report` создаёт `target/site/jacoco/index.html`, `check` требует не менее 80% строк. `app.Main` исключён из подсчёта, потому что это интерактивный цикл со `Scanner`; логика проверяется JUnit-тестами. Меню проверяется вручную.

## Работа приложения

Страховой номер начинается с `INS-`, например `INS-100`. По умолчанию: `GENERAL` — 3 койки, `ICU` — 1, `paracetamol` — 5 единиц. База: `data/hospital.db`.

Меню позволяет принять пациента, посмотреть таблицы, изменить запасы, выполнить компенсацию по id и сбросить БД. Для сбоя B задайте 0 коек, для сбоя C — 0 единиц препарата. Ошибка ввода выводится без завершения программы.

Для изменения запасов `INSERT OR REPLACE` добавляет строку, если её нет, или заменяет значение по ключу.

«Сброс БД» удаляет записи и цепочки, обнуляет счётчики id и возвращает начальные запасы. Сам файл SQLite остаётся; перед сбросом сохраните нужные данные.

## Цепочка и восстановление

`admission_chain` хранит `PENDING_B`, `PENDING_C`, `COMPLETED`, `COMPENSATED`. Регистрация и `PENDING_B` записываются через одно соединение: `commit` сохраняет обе записи, `rollback` при ошибке отменяет обе. `catch (SQLException | RuntimeException e)` обрабатывает как ошибку SQL, так и ошибку проверки данных одним откатом. Это не оставляет `REGISTERED` без цепочки.

После B ставится `PENDING_C`, после C — `COMPLETED`. Сбой B отменяет A; сбой C отменяет заказ, B и A. `recoverUnfinished()` при старте компенсирует незавершённые цепочки. Повторная компенсация не возвращает запасы дважды.

Тест отката временно удаляет таблицу цепочек: вставка `PENDING_B` падает, и проверяется, что регистрация тоже не сохранилась.

## Принцип наименьшего знания (3.5)

Без фасада клиенту пришлось бы знать классы всех трёх подсистем, порядок вызовов, состояния цепочки и обратные действия при сбое:

```java
AdmissionRepository registrations = new AdmissionRepository(databaseFile);
BedRepository beds = new BedRepository(databaseFile);
OrderRepository orders = new OrderRepository(databaseFile);
ChainRepository chains = new ChainRepository(databaseFile);
RegistrationDesk desk = new RegistrationDesk(
        new AdmissionValidator(), new InsuranceVerifier(), registrations);
BedAllocator allocator = new BedAllocator(beds, new EquipmentPolicy());
EquipmentReserver equipment = new EquipmentReserver(beds);
LabOrderSystem lab = new LabOrderSystem(orders);
PharmacyDispatcher pharmacy = new PharmacyDispatcher(orders, new StockChecker());

int id;
try (Connection connection = Sqlite.open(databaseFile)) {
    connection.setAutoCommit(false);
    try {
        id = desk.register(connection, name, insuranceNumber).getId();
        chains.savePendingB(connection, id);
        connection.commit();
    } catch (SQLException | RuntimeException e) {
        connection.rollback();
        throw e;
    }
} catch (SQLException e) {
    throw Sqlite.failure(e);
}

try {
    allocator.reserve(id, department);
    equipment.reserve(id);
    chains.setStatus(id, ChainStatus.PENDING_C);
} catch (BedUnavailableException e) {
    beds.release(id);
    registrations.cancelAdmission(id);
    chains.setStatus(id, ChainStatus.COMPENSATED);
    throw e;
}

try {
    lab.createAndSend(id);
    pharmacy.dispatch(id, medicine);
    chains.setStatus(id, ChainStatus.COMPLETED);
} catch (PharmacyOutOfStockException e) {
    orders.cancelByAdmissionId(id);
    beds.release(id);
    registrations.cancelAdmission(id);
    chains.setStatus(id, ChainStatus.COMPENSATED);
    throw e;
}
```

С фасадом клиент делает один вызов:

```java
int id = admissionFacade.admit(name, insuranceNumber, department, medicine);
```

`AdmissionFacade` скрывает порядок A → B → C и откат от `Main`. `AdminFacade` сам создаёт репозитории по пути к БД. Тесты читают БД напрямую для проверки сохранённого состояния.

## Facade vs Adapter (3.10)

`AdmissionFacade` — **Facade**: один вызов управляет несколькими подсистемами и компенсацией. **Adapter** нужен, когда требуется перевести один интерфейс в другой, например преобразовать нашу заявку в формат внешней лаборатории. Здесь такого преобразования нет.

## UML

Диаграмма классов: [docs/class-diagram.svg](docs/class-diagram.svg).
