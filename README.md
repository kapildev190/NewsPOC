# Newspaper Workflow POC

A Spring Boot + Hibernate proof of concept for a newspaper publishing workflow.
Journalists write articles, editors review them, and only approved articles are visible to end users.

## Requirement

- Three user types: **Admin**, **Editor** and **Journalist**.
- **Admin** creates, lists, deletes (soft) and restores editor and journalist accounts.
- **Journalist** creates, edits and submits news articles, and sees only their own.
- **Editor** approves, rejects or archives articles.
- **End users** (no login) can read **approved** articles only.

## Features

**Core workflow**
- Role-based access with Spring Security (HTTP Basic, BCrypt, stateless).
- Article lifecycle with draft, review, rejection comment, resubmit and archive.
- Approved articles are locked; journalists can only touch their own articles.
- Seeded first admin account; soft delete and restore for users.
- Audit fields: reviewer, review time, publish time, created and updated timestamps.

**Added through the exercises**
1. Public title search.
2. Per-status article counts for a journalist.
3. List disabled users and restore them.
4. `ARCHIVED` status (editor action; hidden from the public API).
5. Reviewer name in responses, with the N+1 problem fixed via entity graph.
6. Clean `409` on optimistic-lock conflicts.
7. Bulk approve with partial success (approved and skipped lists).
8. Keyset-paginated public feed.
9. Plain unit tests for the article state machine.
10. Async author notification after an approval commits.

## Article lifecycle

```
DRAFT --submit--> PENDING_REVIEW --approve--> APPROVED --archive--> ARCHIVED
                       |
                       +--reject (comment)--> REJECTED --edit + submit--> PENDING_REVIEW
```

| From | Action | To | Who |
|---|---|---|---|
| DRAFT / REJECTED | edit | same | Journalist (owner) |
| DRAFT / REJECTED | submit | PENDING_REVIEW | Journalist (owner) |
| PENDING_REVIEW | approve | APPROVED | Editor |
| PENDING_REVIEW | reject | REJECTED | Editor |
| APPROVED | archive | ARCHIVED | Editor |

Any other transition returns `409 Conflict`. The rules live inside the `Article` entity.

## Tech stack

- Java 21 (17+ works), Spring Boot 3.x
- Spring Web, Spring Data JPA (Hibernate 6), Bean Validation, Spring Security
- PostgreSQL (H2 for tests only)
- JUnit 5, MockMvc, spring-security-test

## Configuration

`application.properties` (use **one** config file, not both `.properties` and `.yml`):

```properties
spring.datasource.url=${DB_URL:jdbc:postgresql://localhost:5432/newsdb}
spring.datasource.username=${DB_USER:postgres}
spring.datasource.password=${DB_PASSWORD:postgres}

spring.jpa.hibernate.ddl-auto=update
spring.jpa.open-in-view=false
spring.jpa.show-sql=true
```

- Create the database first: `CREATE DATABASE newsdb;`
- Pass real credentials through the `DB_USER` and `DB_PASSWORD` environment variables. Never commit passwords.
- In `pom.xml`, the PostgreSQL driver has `runtime` scope and H2 has `test` scope. If the log shows `jdbc:h2:mem`, the app is not using PostgreSQL.
- `ddl-auto=create-drop` wipes data on every restart, so use `update` (or Flyway with `validate` for production).

## Running

```bash
mvn clean spring-boot:run     # http://localhost:8080
mvn test
```

Seeded admin: `admin` / `admin123` (**change before sharing the app**).

## API

### Admin (`ROLE_ADMIN`)

| Method | Path | Description |
|---|---|---|
| POST | `/api/admin/users` | Create an editor or journalist (admins cannot be created) |
| GET | `/api/admin/users` | List active users (admins and disabled users hidden) |
| GET | `/api/admin/users/disabled` | List soft-deleted users |
| PATCH | `/api/admin/users/{id}/restore` | Re-enable a disabled user |
| DELETE | `/api/admin/users/{id}` | Soft delete (disable) a user |

### Journalist (`ROLE_JOURNALIST`)

| Method | Path | Description |
|---|---|---|
| POST | `/api/journalist/articles` | Create a draft |
| PUT | `/api/journalist/articles/{id}` | Edit own DRAFT/REJECTED article |
| POST | `/api/journalist/articles/{id}/submit` | Submit for review |
| GET | `/api/journalist/articles` | Own articles, paged (newest first) |
| GET | `/api/journalist/articles/stats` | Count per status, e.g. `{"DRAFT":2,"APPROVED":5,...}` (zeros included) |

### Editor (`ROLE_EDITOR`)

| Method | Path | Description |
|---|---|---|
| GET | `/api/editor/articles/pending` | Review queue (oldest first) |
| POST | `/api/editor/articles/{id}/approve` | Approve (triggers async author notification) |
| POST | `/api/editor/articles/{id}/reject` | Reject; body `{"comment": "..."}` |
| POST | `/api/editor/articles/{id}/archive` | Archive an APPROVED article |
| POST | `/api/editor/articles/bulk-approve` | Body `{"ids":[1,2,3]}` (max 100); returns `{"approved":[...],"skipped":[...]}` |

### Public (no login)

| Method | Path | Description |
|---|---|---|
| GET | `/api/public/articles?category=&page=&size=` | Approved articles, newest first |
| GET | `/api/public/articles/search?title=` | Approved articles whose title contains the text (case-insensitive, paged) |
| GET | `/api/public/articles/feed?cursor=&size=` | Keyset-paginated feed; returns `items` and `nextCursor` |
| GET | `/api/public/articles/{id}` | One approved article (404 otherwise) |

**Feed cursor:** `nextCursor` has the form `<publishedAt>_<id>`. Send it back as `cursor` to get the next page. It is `null` when a page is not full. If the last page is exactly `size` items, the following call returns an empty list. Page size is clamped to 1-50.

## Example walkthrough

```bash
# 1. Admin creates users
curl -u admin:admin123 -X POST localhost:8080/api/admin/users -H 'Content-Type: application/json' \
  -d '{"username":"jane","password":"secret1","fullName":"Jane","role":"JOURNALIST"}'
curl -u admin:admin123 -X POST localhost:8080/api/admin/users -H 'Content-Type: application/json' \
  -d '{"username":"ed","password":"secret1","fullName":"Ed","role":"EDITOR"}'

# 2. Journalist writes and submits
curl -u jane:secret1 -X POST localhost:8080/api/journalist/articles -H 'Content-Type: application/json' \
  -d '{"title":"Big News","summary":"Short","content":"Full text","category":"world"}'
curl -u jane:secret1 -X POST localhost:8080/api/journalist/articles/1/submit
curl -u jane:secret1 localhost:8080/api/journalist/articles/stats

# 3. Editor reviews
curl -u ed:secret1 localhost:8080/api/editor/articles/pending
curl -u ed:secret1 -X POST localhost:8080/api/editor/articles/1/approve
curl -u ed:secret1 -X POST localhost:8080/api/editor/articles/bulk-approve \
  -H 'Content-Type: application/json' -d '{"ids":[2,3,4]}'
curl -u ed:secret1 -X POST localhost:8080/api/editor/articles/1/archive

# 4. End users (no login)
curl "localhost:8080/api/public/articles?category=world"
curl "localhost:8080/api/public/articles/search?title=big"
curl "localhost:8080/api/public/articles/feed?size=5"
```

A Postman collection (`news-poc.postman_collection.json`) covers the original endpoints; add the new ones to the matching role folder so they inherit the right credentials.

## Project structure

```
com.example.news
├── NewsApplication              entry point (@EnableJpaAuditing, @EnableAsync)
├── entity/
│   ├── BaseEntity               id, @Version, createdAt, updatedAt
│   ├── AppUser, Role            users and roles (enable()/disable() for soft delete)
│   └── Article, ArticleStatus   article + workflow rules (state machine)
├── repo/
│   ├── UserRepository           incl. findByRoleNotAndEnabledTrue, findByEnabledFalse
│   ├── ArticleRepository        derived queries, @EntityGraph, stats and feed queries
│   └── StatusCount              projection for per-status counts
├── dto/Dtos                     request/response records with validation
├── service/
│   ├── UserService              create, list, soft delete, restore
│   ├── ArticleService           workflow, ownership checks, search, stats, bulk approve, feed
│   ├── ArticleApprovedEvent     event published on approval
│   ├── ArticleNotificationListener  @Async + AFTER_COMMIT notification
│   └── NotFoundException
├── security/
│   ├── SecurityConfig           URL-to-role rules, HTTP Basic, stateless
│   └── DbUserDetailsService     loads users from the database
├── web/                         controllers per audience + GlobalExceptionHandler
└── config/
    ├── PasswordConfig           BCrypt PasswordEncoder bean
    └── AdminSeeder              creates the first admin
```

## HTTP status codes

| Code | Meaning |
|---|---|
| 400 | Validation failed (blank title, short password, bad bulk size, ...) |
| 401 | Missing or wrong credentials, or disabled user |
| 403 | Authenticated but wrong role |
| 404 | Not found, not owned by the journalist, or not approved (public) |
| 409 | Invalid workflow transition, duplicate username, admin protection, optimistic-lock conflict, data integrity error |

## Design notes

- **Workflow rules live in `Article`**, so no code path can bypass them and they can be unit-tested without Spring.
- **Ownership in the query** (`findByIdAndAuthorUsername`): other journalists' articles look like they don't exist.
- **Separate DTOs for public and internal views**: end users never see status, reviewer or rejection comments.
- **`open-in-view=false`**: entities are mapped to DTOs inside service transactions.
- **N+1 avoided** with `@EntityGraph(attributePaths = {"author", "reviewer"})` on article lists. The reviewer is a left join because unreviewed articles have none.
- **Optimistic locking** with `@Version`; a lost update returns `409` instead of silently overwriting.
- **Bulk approve** loads all articles in one query and skips (not fails) articles that are missing or not pending.
- **Keyset pagination** orders by `(publishedAt desc, id desc)` so rows with equal timestamps are never skipped or repeated. Recommended index:
  ```sql
  create index idx_article_feed on article (status, published_at desc, id desc);
  ```
- **Notifications** use `@TransactionalEventListener(AFTER_COMMIT)` plus `@Async`: nothing is sent if the approval rolls back, and the HTTP response is not delayed. For guaranteed delivery, use an outbox table.
- **Soft delete** disables login but keeps article authorship.

## Testing

| Test | Type | Covers |
|---|---|---|
| `ArticleWorkflowTest` | Integration (MockMvc) | Admin creates users, journalist submits, public list is empty, journalist cannot approve, editor approves, article becomes public, approved article cannot be edited |
| `ArticleWorkflowUnitTest` | Plain unit test | Every transition rule in `Article`, no Spring or database |
| `OptimisticLockTest` | Integration | Second stale update throws `ObjectOptimisticLockingFailureException` |

Tests must not run against your real PostgreSQL data. Add `src/test/resources/application.properties` so tests use in-memory H2:

```properties
spring.datasource.url=jdbc:h2:mem:testdb
spring.jpa.hibernate.ddl-auto=create-drop
```

Integration tests that create fixed usernames fail on a second run against a persistent database, which is another reason to keep tests on H2.

## Running with Docker (optional)

A multi-stage `Dockerfile` (Maven + Temurin 21 build, JRE runtime) and a `docker-compose.yml` with `db` (PostgreSQL) and `app` services start everything with:

```bash
docker compose up --build
```

Inside Compose the app reaches the database at `jdbc:postgresql://db:5432/newsdb` through the `DB_URL` variable.

## Known limitations / next steps

- **Existing database after adding `ARCHIVED`:** `ddl-auto=update` does not change Hibernate's check constraint on `article.status`. Update it manually (or recreate the table in development), otherwise archiving fails with a `409 Duplicate or invalid data` response:
  ```sql
  alter table article drop constraint article_status_check;
  alter table article add constraint article_status_check
    check (status in ('DRAFT','PENDING_REVIEW','APPROVED','REJECTED','ARCHIVED'));
  ```
- HTTP Basic auth: replace with JWT or OAuth2 for production, and add rate limiting.
- Replace `ddl-auto` with Flyway migrations (`ddl-auto=validate`).
- `GenerationType.IDENTITY` ids prevent JDBC batch inserts; switch to `SEQUENCE` if bulk creation is needed.
- Any editor can review any article (no desk or category assignment).
- No password change/reset, article images, tags, or email delivery (the notification is a log line).
- The default admin password is hard-coded for the POC only.