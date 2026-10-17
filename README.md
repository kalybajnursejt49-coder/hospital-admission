# Hospital Admission

Консольное приложение для приёма пациента на Java 17, Maven и SQLite JDBC. Регистрация (A), назначение койки и оборудования (B), лабораторный и аптечный заказы (C) координируются через `AdmissionFacade`. Подсистемы A, B и C не импортируют друг друга.

## Запуск и проверка

Требуются Java 17+ и Maven. Первый запуск из чистой папки: `mvn compile exec:java`. После компиляции: `mvn exec:java`. Тесты: `mvn test`. Проверка покрытия: `mvn verify`. Отчёт JaCoCo: `target/site/jacoco/index.html`.

JaCoCo проверяет минимум 80% строк основного кода. `app.Main` исключён из подсчёта: это тонкая оболочка с `Scanner`, `System.out.println` и циклом меню, зависящая от ручного ввода. Сценарии и ветвления предметной логики проверяются независимыми JUnit-тестами на временных SQLite-БД. Исключение `Main` не означает, что приложение можно не запускать вручную.

## Работа приложения

Страховой номер должен начинаться с `INS-`, например `INS-100`. По умолчанию в `GENERAL` есть 3 свободные койки, в `ICU` — 1. Запас `paracetamol` — 5 единиц. База хранится в `data/hospital.db` относительно рабочей папки.

Меню: принять пациента, показать таблицы, изменить число свободных коек, изменить остаток препарата, компенсировать приём по id, сбросить БД, выйти. Для демонстрации сбоя B установите 0 коек в нужном отделении; для сбоя C установите 0 единиц препарата. Неверный номер, id или числовой ввод выводит сообщение об ошибке, после чего меню продолжает работу.

Пункт «Сброс БД» очищает записи пациентов, назначений, заказов и цепочек, сбрасывает счётчики id и восстанавливает исходные запасы коек, оборудования и `paracetamol`. Файл `data/hospital.db` остаётся на диске. Перед сбросом сохраните нужные записи отдельно.

## Цепочка и восстановление

`admission_chain` хранит `PENDING_B`, `PENDING_C`, `COMPLETED`, `COMPENSATED`. Проверка данных и страховки происходит до записи. Регистрация пациента и запись `PENDING_B` выполняются одной транзакцией SQLite. Поэтому сбой между этими действиями не оставит `REGISTERED` без цепочки.

После B статус становится `PENDING_C`, после C — `COMPLETED`. Если B не удался, отменяется A. Если C не удался, отменяются лабораторный заказ, B и A, а цепочка становится `COMPENSATED`. При старте `recoverUnfinished()` находит `PENDING_B` и `PENDING_C` и выполняет повторяемую компенсацию. Ручная компенсация доступна и для завершённой цепочки. Повторная компенсация не возвращает запасы второй раз.

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

`AdmissionFacade` скрывает от `Main` зависимости между A, B и C и детали компенсации. `AdminFacade` отдельно получает путь к БД и сам создаёт репозитории для административных действий. `AdmissionFacade` не предоставляет геттеров репозиториев. Тесты читают БД напрямую через отдельные репозитории, так как проверяют сохранённое состояние, а не публичный интерфейс приложения.

## Facade vs Adapter (3.10)

`AdmissionFacade` — **Facade**: он предоставляет одну операцию `admit(...)` для нескольких подсистем, управляет порядком A → B → C и компенсацией. Он не переводит несовместимый интерфейс в другой, поэтому это не Adapter. `AdminFacade` также упрощает административные действия для меню. Если бы сторонняя лаборатория требовала интерфейс с другим форматом заявки, отдельный Adapter преобразовывал бы нашу заявку в её формат; он не отвечал бы за всю цепочку приёма.

## UML

Файл-изображение диаграммы классов: [docs/class-diagram.svg](docs/class-diagram.svg). Исходная Mermaid-диаграмма, состояния и последовательность: [docs/uml.md](docs/uml.md).
