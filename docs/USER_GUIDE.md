# Car Rental System – User Guide

This guide explains how rental office employees use the Car Rental System.

**Team:** Denizhan Demir · Emek Mert Öçal · Erman Yazgan (BAI0168)

---

## 1. Introduction

The Car Rental System keeps a rental office's cars, customers and rentals in one place, so staff always know which car is rented out and when it comes back.

The application is used by **rental office employees** only. Customers do not log in; staff enter and manage all data for them.

| Module | What you do there |
| --- | --- |
| Rentals | Start a rental, return a car, see active, overdue and closed rentals |
| Cars | Add, edit and delete cars; filter by brand, category or availability |
| Customers | Add, edit and delete customers; search by name or email |
| Brands | Manage the brand list used by cars |
| Categories | Manage the category list used by cars (SUV, Sedan, ...) |

---

## 2. Getting started

1. Make sure **Docker Desktop** is running.
2. In a terminal, go to the project folder and start the database: `docker compose up -d`
3. Start the application: `./mvnw spring-boot:run` (on Windows: `mvnw.cmd spring-boot:run`)
4. Wait for the line `Started CarrentalApplication`, then open **http://localhost:8080**.
5. To stop the application, press `Ctrl + C` in the terminal.

On the first start with an empty database, the system loads sample data (7 brands, 5 categories, 10 cars, 6 customers, 6 rentals) so every screen can be tried right away.

### Home page and navigation

The dark bar at the top is on every page. Click **Car Rental** to return to the home page, or a menu item to open a module: **Rentals, Cars, Customers, Brands, Categories**. The Rentals card on the home page has shortcuts to **New rental** and **Active rentals**.

### Messages

- **Green message** – the action succeeded (for example "Car saved successfully.").
- **Red message** – the action was not allowed; the message says why.
- **Red text under a field** – that field is missing or invalid; correct it and click Save again.

---

## 3. Rentals

A rental connects one car with one customer for a period of time; returning the car closes the rental and calculates the price.

### 3.1 View rentals

Open **Rentals** in the menu. Use the **All / Active / Closed** buttons to switch between lists. Active rentals are sorted by planned return date, so the ones due soonest are at the top.

| Status | Colour | Meaning |
| --- | --- | --- |
| Active | Blue | The car is with the customer and the planned return date has not passed yet |
| Overdue | Red | The car is still with the customer, but the planned return date has passed |
| Closed | Grey | The car has been returned and the final price is saved |

For active and overdue rentals the **Price** column shows an estimate marked **(est.)**, based on the planned return date. Closed rentals show the final price.

### 3.2 Create a new rental

1. Click **+ New rental** (or the green **Rent** button next to a car on the Cars page – that car is then preselected).
2. Choose a **Car**. Only cars with the status *Available* are listed, with their daily price.
3. Choose a **Customer**.
4. Check the **Start date** (default: today) and the **Planned return date** (default: tomorrow).
5. Click **Create rental**.

The rental appears as *Active* and the car's status changes to **Rented** automatically.

### 3.3 Return a car

1. On the Rentals page, click the green **Return car** button of the rental.
2. Check the details (car, customer, dates, daily price).
3. Enter the **Return date** (default: today). It cannot be earlier than the start date.
4. Click **Confirm return**.

The rental becomes *Closed*, the message shows the total price, and the car becomes **Available** again.

### 3.4 How the price is calculated

**Total price = number of days × daily price**, with a minimum of 1 day.

Example: a car at 45.00 per day, rented on 29 September and returned on 2 October, costs 3 × 45.00 = **135.00**.

The daily price is saved when the rental is created. If the car's price is changed later, existing rentals keep their original price.

---

## 4. Cars

The Cars page lists the whole fleet with each car's brand, category, daily price and current status.

| Status | Colour | Meaning | Can be rented? |
| --- | --- | --- | --- |
| Available | Green | The car is in the office and ready | Yes – a green **Rent** button is shown |
| Rented | Yellow | The car has an active rental | No |
| Maintenance | Grey | The car is being serviced | No |

### 4.1 Filter cars

Choose a **Brand**, a **Category** and/or an **Availability** status above the table and click **Filter**. Click **Clear** to show all cars again. Example: *Availability = Available* shows only the cars that can be rented now.

### 4.2 Add a car

1. Click **+ New car**.
2. Choose the **Brand** and **Category** from the lists. (If a list is empty, add a brand or category first – see section 6.)
3. Enter the **Plate number**, e.g. `AA-BC-123`. It is saved in capital letters and must be unique.
4. Enter the **Daily price**, e.g. `45.00`.
5. Choose the **Status**: *Available* or *Maintenance*.
6. Click **Save**.

### 4.3 Edit a car

Click **Edit** next to the car, change the fields and click **Save**. You can move a car between *Available* and *Maintenance*. The status *Rented* is set only by the system: while a car has an active rental you cannot change its status, and you cannot choose *Rented* by hand.

### 4.4 Delete a car

Click **Delete** and confirm. A car that has rentals (active or past) cannot be deleted, because the rental history must stay complete. Set it to *Maintenance* instead if it should not be rented.

---

## 5. Customers

The Customers page holds everyone who can rent a car; a customer must exist before a rental can be created for them.

### 5.1 Search customers

Type part of a first name, last name or email into the search box and click **Search**. Click **Clear** to show all customers again.

### 5.2 Add a customer

Click **+ New customer**, fill in all fields and click **Save**.

| Field | Example | Rule |
| --- | --- | --- |
| First name | Anna | Required, max. 50 characters |
| Last name | Kovács | Required, max. 50 characters |
| Email | anna.kovacs@example.com | Required, valid email, unique; saved in lower case |
| Phone | +36 30 111 2233 | Required, 6–20 characters: digits, +, space, dash, brackets |
| License number | HU1234567 | Required, max. 20 characters, unique; saved in capital letters |

### 5.3 Edit or delete a customer

Click **Edit**, change the fields and click **Save**. Click **Delete** and confirm to remove a customer. A customer who has rentals cannot be deleted, so that the rental history stays complete.

---

## 6. Brands and Categories

Brands (Toyota, BMW, ...) and categories (Economy, SUV, Van, ...) are the lists you choose from when adding a car; both pages work the same way.

1. Open **Brands** or **Categories** in the menu. Entries are listed alphabetically.
2. Click **+ New brand** / **+ New category**, type the name (max. 50 characters) and click **Save**.
3. To rename an entry, click **Edit**, change the name and click **Save**. All cars using it show the new name.
4. To remove an entry, click **Delete** and confirm.

Names must be unique; upper and lower case count as the same ("toyota" and "Toyota" are duplicates). A brand or category that is still used by a car cannot be deleted – change or delete those cars first.

---

## 7. Rules and messages

The system blocks actions that would leave the data inconsistent. The table lists the messages you may see and what to do.

| Message | Why it appears | What to do |
| --- | --- | --- |
| ... is required. | A required field is empty | Fill in the field and save again |
| A ... with this name / plate number / email / license number already exists. | The value must be unique | Use a different value, or edit the existing record |
| Car ... is not available for rent. | The car is Rented or in Maintenance | Choose another car, or return / release this car first |
| Planned return date cannot be before the start date. | The dates are in the wrong order | Correct the planned return date |
| Return date cannot be before the start date. | The return date is too early | Enter the real return date |
| This car has an active rental. Close the rental to make it available again. | You tried to change the status of a rented car | Return the car on the Rentals page |
| The Rented status is set automatically when a rental is created. | You chose *Rented* by hand | Create a rental instead |
| This ... cannot be deleted because it is used by one or more cars / has rentals. | Other records still refer to it | Keep it, or change the related records first |

### Business rules at a glance

- Only **Available** cars can be rented; a car can have only one active rental at a time.
- Creating a rental sets the car to **Rented**; returning it sets the car back to **Available**.
- Total price = days × daily price (minimum 1 day), using the price saved when the rental was created.
- An active rental past its planned return date is shown as **Overdue**.
- Records that are part of the rental history (cars, customers, brands, categories in use) cannot be deleted.
