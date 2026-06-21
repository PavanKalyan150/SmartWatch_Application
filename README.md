# Smartwatch Leaderboard System

A secure, testable RESTful API for a smartwatch leaderboard gaming platform developed using Java 17 and Spring Boot 3.3.0.

## Core Features & Tech Stack
- **Java 17 & Spring Boot 3.x**
- **Spring Security (JWT)**: Role-based authorization (`ROLE_USER`, `ROLE_ADMIN`).
- **Spring Data JPA**: Relational MySQL schema mapping Users, Devices, Tasks, Challenges, Leaderboards, and Badges.
- **Embedded Kafka Broker**: Launches an in-memory Kafka broker programmatically on port `9092` at startup for telemetry ingestion without external dependencies.
- **Spring Batch 5.0**: Automated challenge ranking (Reader-Processor-Writer) and gamification badges issuance.
- **Swagger UI**: API discoverability at `http://localhost:8081/swagger-ui.html`.
- **JUnit 5 & MockMvc**: Integration testing with >70% coverage.
- **Jackson Masking Serializers**: Automatically masks phone numbers (`******7890`) and email addresses (`j***e@domain.com`) in JSON responses.

---

## Getting Started

### 1. Database Setup
Ensure you have MySQL running on port `3306`. The application connects using:
- **URL**: `jdbc:mysql://localhost:3306/watch_leaderboard`
- **Username**: `root`
- **Password**: `root123`

The database `watch_leaderboard` must exist. If not, create it:
```sql
CREATE DATABASE IF NOT EXISTS watch_leaderboard;
```

### 2. Run the Application
Start the application using Maven:
```bash
mvn spring-boot:run
```
Upon startup, the programmatic Embedded Kafka Broker will start listening on port `9092`, and Hibernate will auto-create/update the database tables.

### 3. Run the Tests & Coverage
Run the test suite and compile the JaCoCo code coverage report:
```bash
mvn clean test
```
View the coverage report at `target/site/jacoco/index.html`.

---

## Example API Requests

### 1. Public Health Check
```bash
curl -X GET http://localhost:8081/health
```

### 2. User Registration (supports ROLE_USER or ROLE_ADMIN)
```bash
curl -X POST http://localhost:8081/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "phone": "9876543210",
    "email": "john.doe@example.com",
    "fullName": "John Doe",
    "password": "password123",
    "role": "ROLE_USER"
  }'
```
*Response shows masked phone and email properties:*
```json
{
  "id": 1,
  "phone": "******3210",
  "email": "j***e@example.com",
  "fullName": "John Doe",
  "points": 0,
  "level": "Novice",
  "device": null
}
```

### 3. Login (returns Access Token)
```bash
curl -X POST http://localhost:8081/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "phone": "9876543210",
    "password": "password123"
  }'
```

### 4. Create a Device Model (Admin only)
```bash
curl -X POST http://localhost:8081/device \
  -H "Authorization: Bearer <TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Garmin Venu 3",
    "features": ["GPS", "HRM", "ACCELEROMETER"]
  }'
```

### 5. Pair User with Device
```bash
curl -X PUT "http://localhost:8080/user/1/device?deviceId=1" \
  -H "Authorization: Bearer <TOKEN>"
```

### 6. Create a Challenge (requires hardware GPS/HRM) (Admin/User)
```bash
curl -X POST http://localhost:8080/challenge \
  -H "Authorization: Bearer <TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Evening Run",
    "description": "Run 10k steps",
    "requiredSteps": 10000,
    "pointsReward": 250,
    "expiryDate": "2026-06-25T20:00:00",
    "requiredFeatures": ["GPS", "HRM"],
    "city": "Mumbai"
  }'
```

### 7. Telemetry Ingestion (POST /user/{userId})
Accepts and validates steps count value and active capability tags:
```bash
curl -X POST http://localhost:8080/user/1 \
  -H "Authorization: Bearer <TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{
    "Step Count Value": 5000,
    "date": "2026-06-20",
    "tags": ["GPS", "HRM"]
  }'
```

### 8. Trigger Ranking Job (Admin only)
Calculates and updates ranks for completed and expired challenges:
```bash
curl -X GET http://localhost:8080/rank \
  -H "Authorization: Bearer <TOKEN>"
```

### 9. Trigger Gamification Job (Admin only)
Evaluates points against thresholds and issues milestone badges:
```bash
curl -X GET http://localhost:8080/game \
  -H "Authorization: Bearer <TOKEN>"
```
