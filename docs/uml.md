# UML: Hospital Admission

## Классы и зависимости

```mermaid
classDiagram
    class Main
    class AdmissionFacade {
        +admit(name, insuranceNumber, department, medicine) int
        +recoverUnfinished() int
        +compensate(admissionId) void
    }
    class AdminFacade {
        +printTables() void
        +setFreeBeds(department, count) void
        +setMedicineStock(medicine, count) void
        +compensate(admissionId) void
        +reset() void
    }
    class RegistrationDesk
    class BedAllocator
    class EquipmentReserver
    class LabOrderSystem
    class PharmacyDispatcher
    class AdmissionRepository
    class BedRepository
    class OrderRepository
    class ChainRepository
    class Admission
    class BedAssignment
    class HospitalOrder
    Main --> AdmissionFacade
    Main --> AdminFacade
    AdminFacade --> AdmissionFacade : compensation
    AdminFacade --> AdmissionRepository
    AdminFacade --> BedRepository
    AdminFacade --> OrderRepository
    AdminFacade --> ChainRepository
    AdmissionFacade --> RegistrationDesk
    AdmissionFacade --> BedAllocator
    AdmissionFacade --> EquipmentReserver
    AdmissionFacade --> LabOrderSystem
    AdmissionFacade --> PharmacyDispatcher
    AdmissionFacade --> ChainRepository
    RegistrationDesk --> AdmissionRepository
    BedAllocator --> BedRepository
    EquipmentReserver --> BedRepository
    LabOrderSystem --> OrderRepository
    PharmacyDispatcher --> OrderRepository
    AdmissionRepository --> Admission
    BedRepository --> BedAssignment
    OrderRepository --> HospitalOrder
```

Подсистемы `registration`, `bed` и `orders` не зависят друг от друга. Их координация находится в `facade`.

## Состояния сущностей и цепочки

```mermaid
stateDiagram-v2
    [*] --> NEW
    NEW --> REGISTERED : register()
    REGISTERED --> CANCELLED : cancel()
```

```mermaid
stateDiagram-v2
    [*] --> RESERVED
    RESERVED --> ASSIGNED : assign()
    RESERVED --> RELEASED : release()
    ASSIGNED --> RELEASED : release()
```

```mermaid
stateDiagram-v2
    [*] --> CREATED
    CREATED --> SENT : send()
    CREATED --> CANCELLED : cancel()
    SENT --> CANCELLED : cancel()
```

```mermaid
stateDiagram-v2
    [*] --> PENDING_B : A и запись цепочки в одной транзакции
    PENDING_B --> PENDING_C : B выполнен
    PENDING_B --> COMPENSATED : сбой B или восстановление
    PENDING_C --> COMPLETED : C выполнен
    PENDING_C --> COMPENSATED : сбой C или восстановление
    COMPLETED --> COMPENSATED : ручная компенсация
```

## Последовательность приёма

```mermaid
sequenceDiagram
    actor User
    participant Main
    participant Facade as AdmissionFacade
    participant A as RegistrationDesk
    participant Chain as ChainRepository
    participant B as BedAllocator и EquipmentReserver
    participant C as LabOrderSystem и PharmacyDispatcher
    User->>Main: данные пациента
    Main->>Facade: admit(...)
    Facade->>A: validate, verify, register
    Facade->>Chain: savePendingB
    Note over Facade,Chain: A и PENDING_B фиксируются одной транзакцией
    Facade->>B: reserve bed and equipment
    alt сбой B
        Facade->>A: cancel admission
        Facade->>Chain: COMPENSATED
    else B успешно
        Facade->>Chain: PENDING_C
        Facade->>C: send lab and pharmacy orders
        alt сбой C
            Facade->>C: cancel lab order
            Facade->>B: release bed and equipment
            Facade->>A: cancel admission
            Facade->>Chain: COMPENSATED
        else C успешно
            Facade->>Chain: COMPLETED
        end
    end
```
