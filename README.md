# NEXO

Nexo is a small management app for a device repair shop, built as a learning and practice project. It helps keep track of customers, repair tickets and their status, and employees. Monorepo with a Spring Boot backend and an Angular frontend.
> [!NOTE]
> Work in progress.

<!-- ## Screenshots -->

## Features

- Real login: email and password with JWT in an HttpOnly cookie (`SameSite=Strict`). Roles (`TECHNICIAN`, `RECEPTION`, `ADMIN`) enforced by the backend. The session survives page reloads.
- Customers: paginated list, shared create/edit dialog, and a detail page with contact info plus the customer ticket history.
- Tickets: paginated list (customer, device, status, technician, date), creation dialog linked to a customer, and a detail page with a progress stepper, technician assignment, status changes, internal notes and a full timeline.
- Employees (admin only): list and inline creation.

## Planned features

- Employee detail page.
- Search and filters for tickets, customers and employees.
- Dashboard with simple metrics.

## Stack and architecture

- `backend/`: Java 21, Spring Boot (Web MVC, Data JPA/Hibernate, Security, Validation), PostgreSQL. REST API under `/api` (`/auth`, `/customers`, `/tickets`, `/employees`). Auth is JWT in an HttpOnly cookie (jjwt, HS256) with role based authorization.
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

On the first start with an empty employee table, a seeder creates an initial admin so you can log in:

- Email `admin@nexo.local`, password `admin123` (development defaults).

For anything beyond local development, set these environment variables:

- `JWT_SECRET`: random value of at least 32 characters. Required in production.
- `NEXO_ADMIN_EMAIL`, `NEXO_ADMIN_PASSWORD`, `NEXO_ADMIN_NAME`: credentials of the seeded admin.

### Frontend

```bash
cd fronted
npm install
npm start # same as `ng serve`, but uses the project's local CLI
```

Runs at `http://localhost:4200`.

### Deployment

TBD.
