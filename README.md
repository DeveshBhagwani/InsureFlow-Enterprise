# InsureFlow Enterprise

InsureFlow Enterprise is a production-grade, secure, and high-performance **Insurance Policy Lifecycle & Claims Management System** built using Spring Boot, Spring Security, Spring Data JPA, and Docker.

It provides a robust platform for managing customers, dynamic premium pricing, multi-stage claim workflows, secure transactions, and enterprise audit logging.

---

## 🛠 Tech Stack
- **Backend Core**: Java 21, Spring Boot 3.3, Maven
- **Security**: Spring Security, JWT (JSON Web Tokens), BCrypt encryption, Refresh Token rotation
- **Persistence**: Spring Data JPA, Hibernate, MySQL 8.0, H2 Database (for testing)
- **Design Patterns**: State Pattern (for claim lifecycles), Strategy Pattern (for pricing calculations)
- **Instrumentation & QA**: JaCoCo (code coverage), JUnit 5, Mockito
- **DevOps**: Docker, Docker Compose, Swagger/OpenAPI (Springdoc)

---

## 🌟 Key Features

### 1. Authentication & Role-Based Access (RBAC)
- Secure registration and login flows.
- Stateless JWT authentication with database-backed Refresh Token rotation to prevent session-jacking.
- Secure password recovery using expiring verification tokens.
- Granular method-level API authorization using `@PreAuthorize` based on roles:
  - `CUSTOMER`: Exclusively manages own profile, policies, claims, and premium payments.
  - `AGENT`: Issues new policies, conducts renewals, and searches customer metrics.
  - `CLAIM_OFFICER`: Manages claim investigations, reviews, and decisions.
  - `ADMIN`: Has full read/write privileges, including processing transaction refunds and system audit log retrieval.

### 2. State-Delegated Claims Workflow
Utilizes the **State Design Pattern** to enforce strict transitions across a claim's lifecycle:
`SUBMITTED` ➔ `UNDER_REVIEW` ➔ `INVESTIGATION` ➔ `APPROVED` / `REJECTED`

Each state encapsulates transition constraints, preventing invalid actions (e.g., investigating a claim before it has been reviewed).

### 3. Dynamic Premium Strategy Engine
Implements the **Strategy Design Pattern** to decouple pricing calculations. Features specialized strategies for:
- **Health Insurance**: Evaluates age, risk scores, occupation, and existing claims.
- **Vehicle Insurance**: Evaluates driver age, vehicle make/model, license history, and vehicle value.
- **Life Insurance**: Evaluates term years, smoking status, and medical history.

### 4. Aspect-Oriented Audit Trails
Maintains system integrity using a **Spring AOP Aspect** that intercepts persistence calls on `@Auditable` methods. It captures and stores before/after states (serialized as flat JSON) along with operator identities and timestamps.

---

## 🚀 Getting Started

### Prerequisites
- JDK 21
- Docker & Docker Compose
- Maven (or IntelliJ bundled version)

### Running Locally with Docker
1. Clone the repository and navigate to the project directory.
2. Build the application and start the containers:
   ```bash
   docker-compose up --build
   ```
3. The API will be accessible at `http://localhost:8080/api/v1`.
4. The interactive OpenAPI Documentation (Swagger UI) is available at:
   `http://localhost:8080/api/v1/swagger-ui.html`

---

## 🧪 Testing & Verification
The project features a suite of 68 integration and unit tests covering all edge cases.

To execute tests locally:
```bash
mvn test
```
*Note: Jacoco is configured to automatically fail builds if overall instruction coverage drops below the required quality gate.*
