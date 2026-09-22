# NEXO

Nexo is a small management app for a device repair shop. It keeps track of customers, repair tickets with their status, and employees. Monorepo with a Spring Boot backend and an Angular frontend.
> [!NOTE]
> Work in progress.

## Screenshots

## Features

- Employee session: pick the active employee on entry. The current employee is shown in the sidebar and can be switched.
- Customers: paginated list and creation dialog.
- Tickets: paginated list (customer, device, status, technician, date) and creation dialog linked to a customer.
- Employees (admin only): list and inline creation.
- The backend already supports the ticket workflow (assign technician, change status, notes, timeline), but the UI for it is still pending.

## Planned features

- Detail views: ticket, customer and employee pages (status changes, assignment, notes and history).
- Search and filters for tickets, customers and employees.
- Real login/authentication with roles instead of the current employee picker.

## Stack and architecture

- `backend/`: Java 21, Spring Boot (Web MVC, Data JPA/Hibernate, Security), PostgreSQL. REST API under `/api` (`/customers`, `/tickets`, `/employees`).
- `fronted/`: Angular 21 (standalone components, signals, reactive forms) with PrimeNG 21. Code is split by feature (`customers`, `tickets`, `employees`), plus shared `core/` (services, models, guards) and `layout/` (app shell with sidebar).
- The frontend calls the backend directly at `http://localhost:8080/api`.

## How to run

### Requirements

- Java 21
- Docker (for the database)
- Node.js 20.19+ with npm

### Database

```bash
docker run --name nexo-db -e POSTGRES_DB=nexo -e POSTGRES_USER=nexo -e POSTGRES_PASSWORD=nexo -p 5432:5432 -d postgres:17
```

The backend connects by default to `jdbc:postgresql://localhost:5432/nexo` (user `nexo`, password `nexo`). To use different values, set these environment variables before starting it (see `backend/src/main/resources/application.yaml`):

- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`

### Backend

```bash
cd backend
./mvnw spring-boot:run
```

Runs at `http://localhost:8080`. Tables are created automatically (`ddl-auto: update`), but the `nexo` database must already exist.

### Frontend

```bash
cd fronted
npm install
npm start # same as `ng serve`, but uses the project's local CLI
```

Runs at `http://localhost:4200`.

### Deployment

TBD.
