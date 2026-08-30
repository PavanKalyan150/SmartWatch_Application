# Smartwatch Leaderboard API

## 1 Overview
REST + Kafka micro-service that tracks smartwatch activity, assigns scores/levels, and publishes leaderboards for time-bound challenges.

---

## 2 Technology Stack
| Layer | Tech |
|-------|------|
| Runtime | Spring Boot 3, Spring Web |
| Security | Spring Security (JWT) |
| Data | Spring Data JPA → MySQL/H2 |
| Batch | Spring Batch + `BatchScheduler` |
| Messaging | Apache Kafka (activity events) |
| Docs | springdoc-openapi (Swagger UI) |

---

## 3 API Endpoints

| Domain | Verb | Full Path | Use Case |
|--------|------|-----------|----------|
| **AuthController** (`/auth`) |
|  | POST | `/auth/register` | Create user & return JWT |
|  | POST | `/auth/login` | Authenticate & return JWT |
|  | POST | `/auth/logout` | Client-side token disposal acknowledgment |
| **UserController** (`/user`) |
|  | POST | `/user/{userId}/activity` | Push activity event if device is offline |
|  | GET  | `/user/{userId}/activity` | Retrieve all events for user |
| **DeviceController** (`/devices`) |
|  | GET  | `/devices/` | List registered devices |
|  | PUT  | `/devices/{id}` | Admin – update device metadata |
| **ChallengeController** (`/challenges`) |
|  | GET  | `/challenges/{id}` | Fetch challenge detail & leaderboard link |
|  | PUT  | `/challenges/{id}` | Admin – edit challenge |
|  | POST | `/challenges/{challengeId}/enroll/{userId}` | Enroll user into challenge |
|  | GET  | `/challenges/{challengeId}/user` | Current user’s rank & progress in challenge |
| **TaskController** (`/tasks`) |
|  | GET  | `/tasks/{id}` | Task definition & reward scheme |
|  | PUT  | `/tasks/{id}` | Admin – update task parameters |
|  | GET  | `/tasks/user/{userId}` | User-specific task progress snapshot |
| **BatchController** (`/batch`) – *admin-only* |
|  | GET  | `/batch/rank` | Manually trigger RankingJob |
|  | GET  | `/batch/game` | Manually trigger GamificationJob |
| **HealthController** |
|  | GET  | `/health` | Liveness/Readiness probe (no auth) |

> Security: All endpoints except `/auth/**` and `/health` require `Authorization: Bearer <JWT>`.  
> Role matrix: Users (`ROLE_USER`) vs Admins (`ROLE_ADMIN`) as enforced by `@PreAuthorize` on methods.

## 4 Batch Processing (Nightly or Manual Trigger)

### 5.1 GamificationJob (`GamificationJobConfig`) and RankingJob (`RankingJobConfig`)
| Step | Component | Action |
|------|-----------|--------|
| 1 | `ScoreComputationProcessor` | Aggregate `UserActivityEvent` → XP & challenge points |
| 2 | `ScoreUpdateWriter` | Update **User** total score & **Challenge** cumulative points |
| 3 | `LevelUpProcessor` | Determine if XP crosses threshold |
| 4 | `LevelUpWriter` | Persist new **Level** row & increment **User.level** |
| 5 | `ChallengeExpiryTasklet` | Mark finished challenges `EXPIRED`, lock scores |
| 6 | `RankPublishTasklet` | Calculate rank order per challenge, write **Leaderboard** snapshot |

Batch entry point: `BatchScheduler` (@Scheduled cron: `0 0 2 * * *`) or `/batch/game` & `/batch/rank` endpoints.

---

## 5 Domain Entities (JPA)

| Entity | Key Fields | Purpose |
|--------|-----------|---------|
| User | id, email, level, totalScore | Auth + profile |
| Device | id, model, user_id | Registered smartwatch |
| Challenge | id, name, start, end, rewardScheme | Group competition |
| ChallengeTask | id, challenge_id, task_id | Link tasks to challenge |
| Task | id, metric, target | e.g., steps ≥ 10 000 |
| UserTask | id, user_id, task_id, status, progress | User’s task completion |
| UserChallenge | id, user_id, challenge_id, status | Enrollment & badge |
| UserActivityEvent | id, user_id, device_id, metric, value, processedStatus | Raw activity |
| Level | id, thresholdXP, label | XP tiers |
| Leaderboard | id, challenge_id, user_id, rank, scoreSnap | Daily snapshot |

---

Swagger UI → `http://localhost:8080/swagger-ui.html`

