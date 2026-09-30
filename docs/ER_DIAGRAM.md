# ER diagram (version 3.0)

```mermaid
erDiagram
    BRANDS ||--o{ CARS : "has"
    CATEGORIES ||--o{ CARS : "groups"
    CARS ||--o{ RENTALS : "is rented in"
    CUSTOMERS ||--o{ RENTALS : "makes"
    INSURANCE_PLANS ||--o{ RENTALS : "covers"

    BRANDS {
        bigint id PK
        varchar name UK
    }
    CATEGORIES {
        bigint id PK
        varchar name UK
    }
    CARS {
        bigint id PK
        bigint brand_id FK
        bigint category_id FK
        varchar model
        int model_year
        varchar plate_number UK
        decimal daily_price
        varchar status "AVAILABLE, RENTED, MAINTENANCE"
        varchar image_url
    }
    CUSTOMERS {
        bigint id PK
        varchar first_name
        varchar last_name
        varchar email UK
        varchar phone
        varchar license_number UK
    }
    INSURANCE_PLANS {
        bigint id PK
        varchar name UK
        varchar description
        decimal daily_price
        decimal deductible
    }
    RENTALS {
        bigint id PK
        bigint car_id FK
        bigint customer_id FK
        bigint insurance_plan_id FK
        date start_date
        date planned_end_date
        date return_date "null while active"
        decimal daily_price "car price saved at start"
        decimal insurance_daily_price "insurance price saved at start"
        decimal total_price "set when returned"
    }
```

## Notes

- A rental is **active** while `return_date` is empty; it is **overdue** when it is active and `planned_end_date` has passed.
- `total_price = days x (daily_price + insurance_daily_price)`, minimum 1 day.
- Prices are copied into the rental when it starts, so later price changes do not change old rentals.
- `model_year` is used as the column name because `YEAR` is a reserved word in H2.
- `image_url` is optional; the desktop app downloads the photo once and caches it in `~/CarRentalData/images`.
