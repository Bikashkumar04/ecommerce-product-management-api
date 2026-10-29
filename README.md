# E-commerce Product Management REST API

A backend REST API for managing e-commerce products with user registration, JWT authentication, role-based authorization, request validation, and consistent JSON error responses.

## Implemented features

- Product CRUD operations
- PostgreSQL persistence with Flyway schema migration
- User registration with BCrypt password hashing
- Username/password login with signed JWT tokens
- `USER` and `ADMIN` roles
- Product read access for authenticated users
- Product create, update, and delete access for administrators
- Request validation for product fields
- Global error responses for validation, authentication, authorization, conflicts, missing resources, malformed JSON, and unexpected failures

## Technology stack

- Java 21
- Spring Boot 4.0.9
- Spring Security
- JSON Web Token (JJWT 0.12.6)
- Spring Data JPA / Hibernate
- PostgreSQL
- Flyway
- Maven
- JUnit and MockMvc

## Prerequisites

- JDK 21
- PostgreSQL 14 or newer
- Maven (or the included Maven Wrapper)
- A Base64-encoded JWT signing key

Verify Java:

```bash
java -version
```

## Project structure

```text
src/
├── main/
│   ├── java/com/bikash/ecommerce/
│   │   ├── config/          # JSON and Spring Security configuration
│   │   ├── controller/      # Authentication and product endpoints
│   │   ├── dto/             # Request and response DTOs
│   │   ├── entity/          # JPA entities and roles
│   │   ├── exception/       # Error response and exception handling
│   │   ├── repository/      # Spring Data repositories
│   │   ├── security/        # JWT request filter
│   │   └── service/         # Business and authentication services
│   └── resources/
│       ├── db/migration/    # Flyway SQL migrations
│       └── application.properties
└── test/
    ├── java/                # Integration and MockMvc tests
    └── resources/           # Isolated H2 test configuration
```

## Clone the repository

Replace `<repository-url>` with the URL of the submitted GitHub repository:

```bash
git clone <repository-url>
cd ecommerce-product-management-api
```

## PostgreSQL setup

Create the database before starting the application:

```sql
CREATE DATABASE ecommerce_db;
```

The application does not create PostgreSQL users or databases. Use a PostgreSQL account with permission to create tables and indexes in `ecommerce_db`.

The application reads these environment variables:

| Variable | Purpose | Example |
|---|---|---|
| `DATASOURCE_URL` | PostgreSQL JDBC URL | `jdbc:postgresql://localhost:5432/ecommerce_db` |
| `DATASOURCE_USERNAME` | PostgreSQL username | `postgres` |
| `DATASOURCE_PASSWORD` | PostgreSQL password | `use-a-local-secret` |
| `JWT_SECRET` | Base64-encoded HMAC signing key | `base64-key-value` |
| `JWT_EXPIRATION_MS` | Token lifetime in milliseconds | `3600000` |

Copy [.env.example](.env.example) to a local `.env` file and replace every placeholder. The application does not load `.env` automatically; export the variables in the shell, or configure them through the IDE/run configuration.

PowerShell example:

```powershell
$env:DATASOURCE_URL = "jdbc:postgresql://localhost:5432/ecommerce_db"
$env:DATASOURCE_USERNAME = "postgres"
$env:DATASOURCE_PASSWORD = "<your-postgres-password>"
$env:JWT_SECRET = "<base64-encoded-256-bit-key>"
$env:JWT_EXPIRATION_MS = "3600000"
```

Generate a suitable random key with PowerShell:

```powershell
$bytes = New-Object byte[] 32
[System.Security.Cryptography.RandomNumberGenerator]::Fill($bytes)
[Convert]::ToBase64String($bytes)
```

Do not commit `.env`, database passwords, JWT secrets, or other credentials.

## Database migrations

Flyway is enabled for the application. On startup it runs:

```text
src/main/resources/db/migration/V1__create_users_and_products.sql
```

The migration creates the `users` and `products` tables, unique username/email constraints, role constraints, product price precision, and non-negative stock constraints. Hibernate is configured with `ddl-auto=validate`, so it checks the migrated schema instead of changing it.

For an existing database created by an earlier development version using Hibernate `ddl-auto=update`, inspect and back up the database before adopting Flyway. Do not delete existing product data. If the schema already contains the equivalent tables, baseline or reconcile it using your team’s database migration policy before running the application.

## Run the application

With the required environment variables configured:

Windows:

```powershell
.\mvnw.cmd spring-boot:run
```

macOS/Linux:

```bash
./mvnw spring-boot:run
```

The API starts on the default port `8080`.

## Run tests

Windows:

```powershell
.\mvnw.cmd clean test
```

macOS/Linux:

```bash
./mvnw clean test
```

Tests use an isolated in-memory H2 database and do not require PostgreSQL credentials.

## API documentation

### Authentication endpoints

#### Register

```http
POST /api/auth/register
Content-Type: application/json
```

Authentication: none.

Request:

```json
{
  "username": "alice",
  "email": "alice@example.com",
  "password": "StrongPassword123!"
}
```

Success: `201 Created`.

The public request cannot assign a role. New users always receive `USER`.

Common errors: `400 Bad Request` for invalid fields, `409 Conflict` for duplicate username/email.

#### Login

```http
POST /api/auth/login
Content-Type: application/json
```

Authentication: none.

Request:

```json
{
  "username": "alice",
  "password": "StrongPassword123!"
}
```

Success: `200 OK`.

The response contains a signed JWT and basic user information. Password hashes are never returned.

Common errors: `400 Bad Request` for invalid fields, `401 Unauthorized` for invalid credentials.

### Product endpoints

Product request fields:

```json
{
  "name": "Wireless Keyboard",
  "description": "Bluetooth mechanical keyboard",
  "price": 2499.99,
  "category": "Electronics",
  "stockQuantity": 50
}
```

`name` is required and limited to 150 characters. `description` is optional and limited to 1000 characters. `price` must be at least `0.01` with up to 10 integer digits and 2 decimal places. `category` is required and limited to 100 characters. `stockQuantity` is required and cannot be negative.

| Method | URL | Authentication | Required role | Success | Common errors |
|---|---|---|---|---|---|
| `GET` | `/api/products` | Bearer JWT | `USER` or `ADMIN` | `200 OK` | `401` |
| `GET` | `/api/products/{id}` | Bearer JWT | `USER` or `ADMIN` | `200 OK` | `401`, `404` |
| `POST` | `/api/products` | Bearer JWT | `ADMIN` | `201 Created` | `400`, `401`, `403`, `409` |
| `PUT` | `/api/products/{id}` | Bearer JWT | `ADMIN` | `200 OK` | `400`, `401`, `403`, `404` |
| `DELETE` | `/api/products/{id}` | Bearer JWT | `ADMIN` | `204 No Content` | `401`, `403`, `404` |

Create:

```http
POST /api/products
Authorization: Bearer <admin-jwt>
Content-Type: application/json
```

```json
{
  "name": "Wireless Keyboard",
  "description": "Bluetooth mechanical keyboard",
  "price": 2499.99,
  "category": "Electronics",
  "stockQuantity": 50
}
```

Update:

```http
PUT /api/products/1
Authorization: Bearer <admin-jwt>
Content-Type: application/json
```

```json
{
  "name": "Updated Wireless Keyboard",
  "description": "Updated description",
  "price": 2699.99,
  "category": "Electronics",
  "stockQuantity": 45
}
```

Read:

```http
GET /api/products/1
Authorization: Bearer <user-or-admin-jwt>
```

Delete:

```http
DELETE /api/products/1
Authorization: Bearer <admin-jwt>
```

## Using JWT in Postman

1. Register a user and log in using the authentication endpoints.
2. Copy the `token` value from the login response.
3. Open a protected request in Postman.
4. Select **Authorization** → **Bearer Token**.
5. Paste the token and send the request.

Equivalent header:

```http
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```

## Roles and first administrator

- `USER` can retrieve products.
- `ADMIN` can retrieve, create, update, and delete products.
- Public registration never accepts an admin role.

Provision the first administrator through trusted database administration, not a public API:

```sql
UPDATE users
SET role = 'ADMIN'
WHERE username = 'your-admin-username';
```

Keep database administration credentials private.

## Error response format

Handled errors use this structure:

```json
{
  "timestamp": "2026-10-29T15:00:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/api/products",
  "fieldErrors": {
    "name": "Name must not be blank"
  }
}
```

Authentication failures return `401`, authorization failures return `403`, missing products return `404`, duplicate user/data conflicts return `409`, and unexpected failures return a generic `500` message.

## Troubleshooting

- **Application fails because `JWT_SECRET` is missing:** export a valid Base64-encoded key before starting the application.
- **Database connection fails:** verify PostgreSQL is running, the database exists, and all `DATASOURCE_*` variables are correct.
- **Flyway validation fails:** compare the database schema with `V1__create_users_and_products.sql`; do not delete data to bypass the error.
- **HTTP 401:** obtain a new token and send it as `Authorization: Bearer <token>`.
- **HTTP 403:** use an `ADMIN` token for product create/update/delete operations.
- **HTTP 400:** inspect `fieldErrors` for the exact validation issue.
- **HTTP 404:** verify the product ID exists.

## Known limitations

- No pagination or sorting.
- No frontend.
- No Docker configuration.
- No public role-management endpoint.
- No refresh-token flow.
- No email verification or password reset flow.
- The application currently uses the default HTTP server port `8080`.

## Submission checklist

- [ ] Configure PostgreSQL and verify the database exists.
- [ ] Configure `DATASOURCE_*` and `JWT_*` environment variables.
- [ ] Run `.\mvnw.cmd clean test`.
- [ ] Confirm `README.md`, source files, and Flyway migration are included.
- [ ] Confirm `.env`, `target/`, logs, and IDE files are not committed.
- [ ] Review the final API behavior with Postman.
- [ ] If submitting through GitHub, invite `iks-hiring` to view the repository.

Suggested commit message:

```text
Finalize e-commerce product management API submission
```

Commit and push after reviewing the changes:

```bash
git status
git add README.md .env.example .gitignore pom.xml src
git commit -m "Finalize e-commerce product management API submission"
git push origin <branch-name>
```

Do not push secrets or generated build output.
