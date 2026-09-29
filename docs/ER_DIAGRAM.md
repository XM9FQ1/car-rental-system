# Car Rental System – ER Diagram

The database has five tables. Hibernate creates them automatically from the entity classes in `src/main/java/hu/nye/carrental/model`.

```mermaid
erDiagram
    BRANDS ||--o{ CARS : "brand of"
    CATEGORIES ||--o{ CARS : "category of"
    CARS ||--o{ RENTALS : "rented in"
    CUSTOMERS ||--o{ RENTALS : "makes"

    BRANDS {
        bigint id PK
        varchar name UK "max 50"
    }
    CATEGORIES {
        bigint id PK
        varchar name UK "max 50"
    }
    CARS {
        bigint id PK
        bigint brand_id FK
        bigint category_id FK
        varchar plate_number UK "max 15"
        numeric daily_price "10,2"
        varchar status "AVAILABLE, RENTED, MAINTENANCE"
    }
    CUSTOMERS {
        bigint id PK
        varchar first_name "max 50"
        varchar last_name "max 50"
        varchar email UK "max 100"
        varchar phone "max 20"
        varchar license_number UK "max 20"
    }
    RENTALS {
        bigint id PK
        bigint car_id FK
        bigint customer_id FK
        date start_date
        date planned_end_date
        date return_date "NULL while the rental is active"
        numeric daily_price "price saved at rental time"
        numeric total_price "calculated on return"
    }
```

## Relationships

| Relationship | Type | Meaning |
| --- | --- | --- |
| Brand – Car | 1 : N | A brand can have many cars; every car has exactly one brand |
| Category – Car | 1 : N | A category can have many cars; every car has exactly one category |
| Car – Rental | 1 : N | A car can be rented many times over time, but has at most one active rental (`return_date` is NULL) |
| Customer – Rental | 1 : N | A customer can have many rentals; every rental belongs to exactly one customer |

## Constraints

- **Primary keys:** every table has an auto-generated `id`.
- **Foreign keys:** `cars.brand_id`, `cars.category_id`, `rentals.car_id`, `rentals.customer_id`. Because of them, a brand, category, car or customer that is still referenced cannot be deleted.
- **Unique:** `brands.name`, `categories.name`, `cars.plate_number`, `customers.email`, `customers.license_number`.
- **Not null:** every column except `rentals.return_date` and `rentals.total_price`, which are filled when the car is returned.
