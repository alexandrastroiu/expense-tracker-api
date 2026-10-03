# Expense Tracker API

A REST API for tracking personal expenses, managing recurring payments, and estimating monthly spending against a budget.

Built with Java 21, Spring Boot, and PostgreSQL. The project includes JWT-based authentication, database migrations, automated tests, and a GitHub Actions CI workflow.

## Features

* User registration and login with JWT authentication
* Password hashing with BCrypt
* Create, view, update, and delete expenses, recurring expenses, and budgets
* User-specific data access: users can only access and manage their own records
* Search and filter expenses and recurring expenses
* Pagination and sorting with validated sort fields
* Daily, weekly, monthly, and yearly recurring payments
* Monthly spending estimates, remaining budget balances, and budget usage percentages
* Request validation and centralized error handling
* Versioned database migrations with Flyway to track and apply schema changes consistently
* Docker Compose configuration to run the backend and database together with a reproducible setup
* Automated unit, controller, integration, and Postman API tests
* GitHub Actions CI pipeline to automatically build and test changes on pushes and pull requests to `main` and `develop` branches

## Technology Stack

* Language: Java 21
* Framework: Spring Boot 4.1
* Security: Spring Security, JWT, BCrypt
* Persistence: Spring Data JPA (Hibernate), PostgreSQL 17
* Database migrations: Flyway
* Testing: JUnit, Mockito, MockMvc, Testcontainers, Postman
* API documentation: OpenAPI
* Containers: Docker, Docker Compose
* CI: GitHub Actions, Newman

## Architecture

Requests pass through the security filters before reaching the application layers:

```text
HTTP request
    ↓
Spring Security / JWT authentication
    ↓
Controller → Service → Repository → PostgreSQL
```

- **Controllers** handle HTTP requests and responses
- **DTOs** define the data accepted and returned by the API, validate incoming requests, and keep internal entity details out of responses
- **Services** implement business rules and transactional operations
- **Repositories** provide database access
- **Mappers** convert between entities and DTOs
- **Entities** represent the data stored in the database, mapping Java objects to tables and defining relationships between users, expenses, categories, recurring expenses, and budgets
- **Exception handling** translates application errors into HTTP responses

Authenticated users access their own expenses, recurring expenses, and budgets.

## Database Design

### Entity-Relationship Diagram:
![Database diagram](database/database_diagram.png)

The main entities are:

- `users`
- `categories`
- `expenses`
- `recurring_expenses`
- `budgets`

Each user can have one budget per month.

Flyway migrations are stored in:

```text
backend/src/main/resources/db/migration/
```

The initial migrations create the schema and insert the default categories. Hibernate validates entity mappings against the database schema.

## Getting Started

### Prerequisites

**Run the application with Docker:**
- Git
- Docker with Docker Compose to run the backend and database together. Make sure Docker is running before starting the application
- Java and Maven do not need to be installed locally for this option

**Run Java tests locally:**
- Git
- JDK 21 to build and run the Java application
- Docker to run PostgreSQL and the temporary databases used by integration tests

The project includes the **Maven Wrapper**, so a separate Maven installation is not required. Use `./mvnw` on macOS/Linux or `mvnw.cmd` on Windows.

**Test the API manually:**

Choose either:
- **Postman** to import and run the provided collection.
- **curl** to send requests from a terminal.

To run the Postman collection from the command line, install **Node.js and Newman**.

### Clone the repository

```bash
git clone https://github.com/alexandrastroiu/expense-manager.git
cd expense-manager
```

### Configure environment variables

Copy the example configuration:

```bash
cp .env.example .env
```

Edit `.env`:

```dotenv
POSTGRES_DB=expense_manager
POSTGRES_USER=expense_user
POSTGRES_PASSWORD=replace_with_a_password
JWT_SECRET=replace_with_a_random_secret
```

Use a randomly generated JWT secret of at least 32 bytes. If OpenSSL is available, generate one with:

```bash
openssl rand -hex 32
```

Copy the generated value into `JWT_SECRET`.

### Start the application

From the repository root:

```bash
docker compose up --build -d --wait
```

On a fresh database, Flyway creates the schema and inserts the default categories automatically.

The API runs at:

```text
http://localhost:8080
```

Check application health:

```bash
curl http://localhost:8080/actuator/health
```

Inspect containers and logs:

```bash
docker compose ps
docker compose logs -f backend
```

Stop the containers while keeping database data:

```bash
docker compose down
```

(Optional) The following command deletes the project's database volume:

```bash
docker compose down -v
```

This permanently deletes all stored users, expenses, recurring expenses, and budgets.

(Optional) Then start the application again:

```bash
docker compose up --build -d --wait
```

## API Documentation

- [OpenAPI specification](api-docs/api-docs.yaml)
- [Postman collection](postman/Expense%20Manager%20API.postman_collection.json)
- [Postman environment](postman/Expense%20Manager%20Environment.postman_environment.json)

When the application is running, the generated specification is available at:

- [OpenAPI JSON](http://localhost:8080/v3/api-docs)
- [OpenAPI YAML](http://localhost:8080/v3/api-docs.yaml)

## API Usage

### Main endpoints

All endpoints below require authentication except registration and login.

| Method | Endpoint | Purpose |
|---|---|---|
| POST | `/api/auth/register` | Register |
| POST | `/api/auth/login` | Log in |
| GET | `/api/categories` | List categories |
| POST | `/api/expenses` | Create an expense |
| GET | `/api/expenses` | List expenses |
| GET | `/api/expenses/{id}` | Retrieve an expense |
| PUT | `/api/expenses/{id}` | Update an expense |
| DELETE | `/api/expenses/{id}` | Delete an expense |
| GET | `/api/expenses/search` | Search expenses |
| GET | `/api/expenses/filter` | Filter expenses |
| POST | `/api/recurring-expenses` | Create a recurring expense |
| GET | `/api/recurring-expenses` | List recurring expenses |
| GET | `/api/recurring-expenses/{id}` | Retrieve a recurring expense |
| PUT | `/api/recurring-expenses/{id}` | Update a recurring expense |
| DELETE | `/api/recurring-expenses/{id}` | Delete a recurring expense |
| GET | `/api/recurring-expenses/search` | Search recurring expenses |
| GET | `/api/recurring-expenses/filter` | Filter recurring expenses by amount |
| POST | `/api/budgets` | Create a monthly budget |
| GET | `/api/budgets?period=YYYY-MM-DD` | Retrieve a monthly budget |
| GET | `/api/budgets/{id}` | Retrieve a budget |
| PUT | `/api/budgets/{id}` | Update a budget |
| DELETE | `/api/budgets/{id}` | Delete a budget |
| GET | `/api/budgets/summary?period=YYYY-MM-DD` | Retrieve a monthly summary |

### Error responses

Application errors use a structured response:

```json
{
  "timestamp": "2026-10-03T12:00:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Unsupported sort field: unknownField"
}
```

| Status | Meaning |
|---|---|
| `400` | Invalid request or unsupported sort field |
| `401` | Missing/invalid authentication or incorrect credentials |
| `404` | Resource not found or not owned by the authenticated user |
| `409` | Duplicate username, email, or monthly budget |
| `500` | Unexpected server error |

Authentication failures handled by the security filter may return an empty `401` response body.

### Pagination and sorting

Expense and recurring-expense list, search, and filter endpoints support pagination:

- `page`: zero-based page number; default `0`
- `size`: page size; default `20`
- `sort`: field name and direction, such as `amount,asc`

Repeat `sort` to order by multiple fields:

```text
/api/expenses?page=0&size=10&sort=amount,asc&sort=id,desc
```

| Resource | Allowed sort fields | Default order |
|---|---|---|
| Expenses | `id`, `title`, `amount`, `expenseDate` | `expenseDate,desc`, then `id,desc` |
| Recurring expenses | `id`, `title`, `amount`, `startDate`, `endDate`, `frequency` | `startDate,desc`, then `id,desc` |

Unsupported sort fields return `400 Bad Request`.

### Authentication

Register a user:

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "alice",
    "firstName": "Alice",
    "lastName": "Example",
    "password": "ExamplePassword123!",
    "email": "alice@example.com"
  }'
```

Successful registration returns `201 Created`.

Log in:

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "alice",
    "password": "ExamplePassword123!"
  }'
```

The response contains a token:

```json
{
  "token": "<JWT>"
}
```

Protected requests use:

```http
Authorization: Bearer <JWT>
```

Tokens expire after one hour under the current configuration.

## Budget Calculation Rules

A summary represents the **entire requested calendar month**, even when the supplied date is in the middle of that month. Budget periods are normalized to the first day of the month.

- **Current expenses:** all recorded ordinary expenses dated within the month
- **Estimated monthly expenses:** current expenses plus recurring payments scheduled within that month
- **Remaining current budget:** budget minus current expenses
- **Remaining monthly budget:** budget minus estimated monthly expenses
- **Budget percentage:** estimated monthly expenses divided by the budget, multiplied by 100 and rounded to two decimal places

Remaining balances may be negative, and usage may exceed 100%.

### Recurring payments

- Daily payments count each active day, including start and end dates
- Weekly payments repeat every seven days from the original start date
- Monthly payments retain their original day where possible and use the month's last day when necessary
- Yearly payments recur in the original month, with February 29 adjusted to February 28 in non-leap years
- An omitted end date means the recurring expense continues indefinitely

### Example

| Item | Amount |
|---|---:|
| Monthly budget | 1,000.00 |
| Recorded expenses | 300.00 |
| Scheduled recurring payments | 150.00 |
| Estimated monthly expenses | 450.00 |
| Remaining current budget | 700.00 |
| Remaining monthly budget | 550.00 |
| Budget usage | 45.00% |

The application does not store currency codes or perform currency conversion.

## Testing

### Java tests

From the repository root:

```bash
cd backend
./mvnw clean test
```

Docker must be running for tests that use PostgreSQL Testcontainers. These tests use temporary databases rather than the application's Compose database.

The tests include:

- **Unit tests:** test service behavior and budget calculations
- **Web-layer tests:** test validation, HTTP responses, authentication requirements, and sorting validation
- **Integration tests:** test real authentication, persisted CRUD, user isolation, search/filter behavior, and budget summaries
- **Application startup test:** test Spring context and database initialization

Run the integration test class only:

```bash
./mvnw -Dtest=ExpenseManagerIntegrationTest test
```

Run controller tests only:

```bash
./mvnw -Dtest=ControllerTest test
```

### Postman tests

Import the provided collection and environment into Postman. Select the imported environment, confirm that baseUrl is set to http://localhost:8080, and start the application before running the collection.

Alternatively, with Node.js and Newman available, run from the repository root:

```bash
newman run "postman/Expense Manager API.postman_collection.json" \
  --env-var "baseUrl=http://localhost:8080"
```

The collection creates test data and assumes a fresh test environment. Repeated runs against the same database may encounter existing test users. Use a disposable environment rather than a database containing data you want to preserve.

## Continuous Integration

[View GitHub Actions runs](https://github.com/alexandrastroiu/expense-manager/actions)

The workflow runs on pushes and pull requests targeting `main` or `develop` branches:

1. Sets up Java 21
2. Runs Maven tests
3. Builds and starts the application with Docker Compose
4. Runs the Postman collection through Newman
5. Removes the CI containers and volumes

The current workflow requires these repository settings:

| Type | Name |
|---|---|
| Variable | `POSTGRES_DB` |
| Variable | `POSTGRES_USER` |
| Secret | `CI_POSTGRES_PASSWORD` |
| Secret | `CI_JWT_SECRET` |

These settings are used for the temporary test environment in GitHub Actions. The CI pipeline builds and tests the application. Continuous deployment is not configured.

## Design Decisions

- **Layered architecture:** Controllers handle HTTP requests, services implement business rules, and repositories manage database access. This separates responsibilities and makes the code easier to test and maintain.

- **User data isolation:** The application identifies users through JWT authentication and restricts database queries to their own expenses, recurring expenses, and budgets.

- **Data Transfer Objects:** Request and response DTOs keep the API independent of database entities, validate incoming data, and control which fields are exposed.

- **Precise monetary calculations:** Java `BigDecimal` and PostgreSQL `NUMERIC` columns avoid floating-point rounding errors when storing and calculating amounts.

- **Database-enforced integrity:** Foreign keys, uniqueness constraints, and check constraints protect relationships and enforce rules alongside application validation.

- **Recurring expense estimates:** The application calculates scheduled recurring payments for each month and includes them in the budget estimate without creating individual expense records.

- **Database migrations:** Flyway tracks and applies versioned database changes, keeping the schema consistent across environments.

- **Testing at multiple levels:** Unit tests cover business logic, controller tests verify HTTP behavior, and PostgreSQL integration tests check that authentication, services, and persistence work together.

## Future Improvements

- CSV export
- Spending totals grouped by category
- Linking recurring payments to recorded expenses to prevent double-counting
- A frontend for expense management and budget visualization
- Deployment
