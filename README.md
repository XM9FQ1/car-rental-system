# Car Rental System

**BAI0168 – The Technology and Methodology of System Development**

A simple web application for small car rental offices to manage their cars, customers and rentals in one place, instead of on paper or in Excel.

**Team:** Denizhan Demir · Emek Mert Öçal · Erman Yazgan

---

## Features

| Module | What it does |
|---|---|
| **Rentals** | Start a rental for a customer, close it when the car is returned. The total price is calculated automatically. Overdue rentals are highlighted. |
| **Cars** | Add, list, edit and delete cars (brand, category, plate number, daily price, status). Filter by brand, category or availability. |
| **Customers** | Add, list, edit and delete customers. Search by name or email. |
| **Brands** | Manage the list of car brands. |
| **Categories** | Manage the list of car categories (SUV, Sedan, ...). |

### Business rules

- Only **available** cars can be rented.
- When a rental is created, the car's status changes to **Rented** automatically.
- When the car is returned, the rental is closed, the **total price** is calculated and the car becomes **Available** again.
- **Total price = number of days × daily price** (minimum 1 day).
- The daily price is **saved at the moment of renting**, so later price changes do not affect existing rentals.
- An active rental whose planned return date has passed is shown as **Overdue**.
- The **Rented** status cannot be set or removed by hand – only through rentals.
- Plate numbers, customer emails and license numbers must be **unique**.
- Brands, categories, cars and customers that are still in use **cannot be deleted**.

---

## Technologies

| Layer | Technology |
|---|---|
| Language | Java 21+ |
| Framework | Spring Boot (Spring Web MVC, Spring Data JPA, Validation) |
| User interface | Thymeleaf + Bootstrap 5 |
| Database | PostgreSQL 16 (running in Docker) |
| Build tool | Maven (wrapper included) |
| Version control | Git + GitHub |

---

## How to run the project

### Requirements

- **JDK 21** or newer (check with `java -version`)
- **Docker Desktop** (must be running)
- **Git**

### Steps

1. Clone the repository:

        git clone https://github.com/XM9FQ1/car-rental-system.git
        cd car-rental-system

2. Start the PostgreSQL database (it runs on port 5433):

        docker compose up -d

3. Start the application:

        ./mvnw spring-boot:run          # macOS / Linux
        mvnw.cmd spring-boot:run        # Windows

4. Open **http://localhost:8080** in your browser.

The database tables are created automatically on the first start.

To stop the application press `Ctrl + C`. To stop the database run `docker compose down`
(your data is kept; `docker compose down -v` deletes it).

### Database connection (for DataGrip / pgAdmin)

| Setting | Value |
|---|---|
| Host | `localhost` |
| Port | `5433` |
| Database | `carrental` |
| User | `carrental` |
| Password | `carrental` |

---

## Project structure

    src/main/java/hu/nye/carrental/
    ├── model/        Entities (Brand, Category, Car, CarStatus, Customer, Rental)
    ├── repository/   Spring Data JPA repositories (database access)
    ├── service/      Business logic for rentals (RentalService)
    ├── controller/   Web controllers (one per module)
    ├── dto/          Form objects (RentalForm)
    └── config/       Converters for the drop-down lists

    src/main/resources/
    ├── application.properties   Database and JPA settings
    └── templates/               Thymeleaf HTML pages

### Data model

    Brand 1 ────< Car >──── 1 Category
                   │
                   1
                   │
                   ^
    Customer 1 ──< Rental

- A **brand** and a **category** can have many cars.
- A **car** can have many rentals over time, but only one active rental at a time.
- A **customer** can have many rentals.

---

## Git workflow

- `main` always contains a working version.
- Each feature is developed in its own branch (e.g. `feature/customer-search`) and merged with a pull request.

---

## Sample data

When the application starts with an **empty database**, it automatically loads sample data
(7 brands, 5 categories, 10 cars, 6 customers and 6 rentals – closed, active and overdue),
so the system can be tried out right away. If the database already contains data, nothing is changed.

- To turn it off, set `app.seed-data=false` in `src/main/resources/application.properties`.
- To start again from a clean database with fresh sample data:

        docker compose down -v
        docker compose up -d

---

## Documentation

- [User guide](docs/USER_GUIDE.md) – how to use every screen of the application
- [ER diagram](docs/ER_DIAGRAM.md) – database tables, relationships and constraints

---

## Desktop application

The system can also run as a normal desktop application: double-click to open, no browser, terminal or Docker needed.
In desktop mode it uses an embedded **H2** database stored in `~/CarRentalData` (the web mode keeps using PostgreSQL).

- Try it without packaging: `./mvnw -Pdesktop spring-boot:run`
- Build the installer (macOS, JDK 21+ with `jpackage`): `./build-desktop.sh`
  → `target/dist/Car Rental-1.0.0.dmg`. Open it and drag **Car Rental** into **Applications**.
- On Windows, run the same `jpackage` command with `--type exe` to get an installer.
- To start again with fresh sample data, delete the `~/CarRentalData` folder.

### Native JavaFX interface (version 2.0)

The desktop application now has its own native JavaFX screens (Rentals, Cars, Customers, Brands, Categories)
instead of showing the web pages. It uses the same entities, repositories, validation rules and `RentalService`
as the web version.

- Run it without packaging: `./mvnw -Pfx spring-boot:run`
- Build the installer: `./build-desktop.sh` → `target/dist/Car Rental-2.0.0.dmg`

## Version 3.0 - redesigned desktop app

Version 3.0 adds a new look and new features to the native JavaFX application:

- **Dashboard** start screen: available cars, active and overdue rentals, revenue, current rentals, fleet status and insurance mix.
- **Modern design**: dark sidebar with icons, cards, badges and a new theme for all windows and dialogs.
- **Cars with photos**: every car has brand, **model**, **year** and a **photo** (image URL). Photos are downloaded once and cached in `~/CarRentalData/images`. The car editor has a **Find image online** button and a live preview. The sample cars use free photos from Wikimedia Commons.
- **Insurance plans** like at a real rental company: Basic (included, deductible 1,500), Medium (11.00 / day, deductible 500) and Premium (24.00 / day, no deductible). Plans can be added, edited and deleted (not when used by a rental).
- **Rentals with insurance**: the new rental window shows the car photo, insurance cards and a live price breakdown. Price = days x (car daily price + insurance daily price), minimum 1 day.

Build the macOS installer:

    ./build-desktop.sh

The installer is created at `target/dist/Car Rental-3.0.0.dmg`.

See also: [ER diagram](docs/ER_DIAGRAM.md) and [User guide](docs/USER_GUIDE.md).
