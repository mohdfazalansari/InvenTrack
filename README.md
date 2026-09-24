# InvenTrack – Inventory & Order Management System

InvenTrack is a role-based inventory and order management backend REST API built with Java 17, Spring Boot 3, Spring Security, JWT authentication, and PostgreSQL.

---

## 🛠 Tech Stack

* **Java**: 17
* **Framework**: Spring Boot 3.3.4 (Spring Web, Spring Data JPA, Spring Security)
* **Authentication**: JWT (JSON Web Token) with HMAC-SHA256
* **Database**: PostgreSQL (production), H2 in-memory (automated test profile)
* **Build Tool**: Apache Maven
* **Validation**: Jakarta Bean Validation (`@Valid`, `@NotNull`, `@Min`, etc.)
* **Boilerplate**: Project Lombok
* **Documentation**: OpenAPI 3 / Swagger UI (`springdoc-openapi`)
* **Testing**: JUnit 5, Mockito, Spring Security Test, MockMvc

---

## 👥 Roles & Permissions Matrix

| Feature | ADMIN | STAFF |
|---|:---:|:---:|
| Create Products | ✅ | ❌ (403 Forbidden) |
| Update Products | ✅ | ❌ (403 Forbidden) |
| Delete Products (Soft Delete) | ✅ | ❌ (403 Forbidden) |
| View Products Catalog | ✅ | ✅ |
| View Inventory Stock | ✅ | ✅ |
| View Low-Stock Alerts | ✅ | ✅ |
| Adjust Inventory Directly | ✅ | ❌ (403 Forbidden) |
| Place / Create Orders | ✅ | ✅ |
| View Orders | ✅ | ✅ |
| Update Order Status | ✅ | ❌ (403 Forbidden) |

---

## 📦 Core Entities & Business Rules

1. **User**:
   - `id`, `username` (unique), `password` (BCrypt encoded), `role` (`ADMIN`, `STAFF`), `createdAt`.
2. **Product**:
   - `id`, `name`, `description`, `category`, `price`, `quantity`, `lowStockThreshold`, `isActive`, `createdAt`, `updatedAt`.
   - **Non-negative stock**: `quantity >= 0` is strictly enforced.
   - **Soft Delete**: Setting `isActive = false` preserves referential integrity for historical orders.
3. **Order**:
   - `id`, `user`, `totalAmount`, `status`, `items`, `createdAt`, `updatedAt`.
   - **Statuses**: `PENDING`, `CONFIRMED`, `PROCESSING`, `SHIPPED`, `DELIVERED`, `CANCELLED`.
   - **State Machine**:
     - `PENDING` $\rightarrow$ `CONFIRMED` or `CANCELLED`
     - `CONFIRMED` $\rightarrow$ `PROCESSING` or `CANCELLED`
     - `PROCESSING` $\rightarrow$ `SHIPPED` or `CANCELLED`
     - `SHIPPED` $\rightarrow$ `DELIVERED`
     - `DELIVERED` orders cannot be moved back.
     - `CANCELLED` orders cannot be modified.
   - **Stock Restoration**: Transitioning an order to `CANCELLED` automatically replenishes product stock within the transaction.
4. **OrderItem**:
   - `id`, `order`, `product`, `quantity`, `price` (snapshot price at time of order).
   - **Total Calculation**: Order total is computed strictly by the backend (`SUM(quantity * price)`).
   - **Atomic Concurrency**: Product rows are locked using pessimistic write locks (`@Lock(LockModeType.PESSIMISTIC_WRITE)`) to eliminate race conditions and overselling.

---

## 🚀 Getting Started

### Prerequisites
* Java 17+ installed (`java -version`)
* Maven 3.8+ installed (`mvn -version`)
* PostgreSQL 14+ running on port 5432 (or modify `application.yml`)

### Default Admin Credentials
When the application starts, a default administrator is automatically seeded:
* **Username**: `admin`
* **Password**: `adminPassword123!`

### Build and Run

```bash
# Build the project
mvn clean package -DskipTests

# Run the application
mvn spring-boot:run
```

The application runs on `http://localhost:8080`.

### Running Tests

```bash
mvn test
```

---

## 📖 Swagger / OpenAPI Documentation

Interactive Swagger UI is accessible at:
```
http://localhost:8080/swagger-ui.html
```
OpenAPI JSON schema:
```
http://localhost:8080/v3/api-docs
```

To authenticate in Swagger:
1. Call `POST /api/auth/login` with your credentials.
2. Copy the `token` from the response.
3. Click the **Authorize** button at the top right of the Swagger UI and enter `Bearer <your_token>`.

---

## 📡 API Endpoints Overview

### Authentication (`/api/auth`)
* `POST /api/auth/register` – Register a new user (defaults to `STAFF`).
* `POST /api/auth/login` – Login and obtain JWT token.

### Products (`/api/products`)
* `GET /api/products` – List all active products (supports `?category=...&page=0&size=10`).
* `GET /api/products/{id}` – Get product by ID.
* `POST /api/products` – Create a new product (*ADMIN only*).
* `PUT /api/products/{id}` – Update an existing product (*ADMIN only*).
* `DELETE /api/products/{id}` – Soft-delete a product (*ADMIN only*).

### Inventory (`/api/inventory`)
* `GET /api/inventory` – View stock levels across all products.
* `GET /api/inventory/low-stock` – View products where `quantity <= lowStockThreshold`.
* `POST /api/inventory/{productId}/adjust` – Adjust stock (+/- delta) (*ADMIN only*).

### Orders (`/api/orders`)
* `POST /api/orders` – Create an order and atomically deduct stock.
* `GET /api/orders` – List orders (supports `?status=...&page=0&size=10`).
* `GET /api/orders/{id}` – Get detailed order breakdown with snapshot prices.
* `PATCH /api/orders/{id}/status` – Advance order status according to state machine rules (*ADMIN only*).
