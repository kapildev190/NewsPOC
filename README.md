# Newspaper Workflow POC

A Spring Boot + Hibernate proof of concept for a newspaper publishing workflow.
Journalists write articles, editors review them, and only approved articles are visible to end users.

## Requirement

- There are three user types: **Admin**, **Editor** and **Journalist**.
- **Admin** creates and deletes editor and journalist accounts.
- **Journalist** creates, edits and submits news articles.
- **Editor** approves or rejects submitted articles.
- **End users** (no login) can only read **approved** articles.

### Additions to the original requirement

- Authentication and role-based authorization (Spring Security, HTTP Basic, BCrypt).
- Article lifecycle with a draft step, so editors only see finished work.
- Mandatory rejection comment; rejected articles can be edited and resubmitted.
- Approved articles are locked and cannot be edited.
- A journalist can only see and edit their own articles.
- Editor review queue (pending articles).
- Seeded first admin account (someone must exist to create other users).
- Soft delete for users, so their articles keep an author.
- Audit fields: reviewer, review time, publish time.
- Public API with pagination, newest-first ordering and category filter.

## Article lifecycle

```
DRAFT --submit--> PENDING_REVIEW --approve--> APPROVED (public)
                       |
                       +--reject (comment)--> REJECTED --edit + submit--> PENDING_REVIEW
```

| From | Action | To | Who |
|---|---|---|---|
| DRAFT / REJECTED | submit | PENDING_REVIEW | Journalist (owner) |
| PENDING_REVIEW | approve | APPROVED | Editor |
| PENDING_REVIEW | reject | REJECTED | Editor |
| DRAFT / REJECTED | edit | same | Journalist (owner) |

Any other transition returns `409 Conflict`.

## Tech stack

- Java 17, Spring Boot 3.3.5
- Spring Web, Spring Data JPA (Hibernate), Bean Validation
- Spring Security (HTTP Basic, stateless)
- H2 in-memory database
- JUnit 5 + MockMvc

## Running

```bash
mvn spring-boot:run     # starts on http://localhost:8080
mvn test                # runs the end-to-end workflow test
```

- H2 console: http://localhost:8080/h2-console
  (JDBC URL `jdbc:h2:mem:newsdb`, user `sa`, empty password)
- Seeded admin: `admin` / `admin123` (**change before sharing the app**)

## API

### Admin (`ROLE_ADMIN`)

| Method | Path | Description |
|---|---|---|
| POST | `/api/admin/users` | Create an editor or journalist |
| GET | `/api/admin/users` | List users |
| DELETE | `/api/admin/users/{id}` | Disable a user (soft delete) |

### Journalist (`ROLE_JOURNALIST`)

| Method | Path | Description |
|---|---|---|
| POST | `/api/journalist/articles` | Create a draft |
| PUT | `/api/journalist/articles/{id}` | Edit own DRAFT/REJECTED article |
| POST | `/api/journalist/articles/{id}/submit` | Submit for review |
| GET | `/api/journalist/articles` | List own articles (paged) |

### Editor (`ROLE_EDITOR`)

| Method | Path | Description |
|---|---|---|
| GET | `/api/editor/articles/pending` | Review queue (oldest first) |
| POST | `/api/editor/articles/{id}/approve` | Approve |
| POST | `/api/editor/articles/{id}/reject` | Reject, body: `{"comment": "..."}` |

### Public (no login)

| Method | Path | Description |
|---|---|---|
| GET | `/api/public/articles?category=&page=&size=` | Approved articles, newest first |
| GET | `/api/public/articles/{id}` | One approved article (404 otherwise) |

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

# 3. Public sees nothing yet
curl localhost:8080/api/public/articles

# 4. Editor reviews
curl -u ed:secret1 localhost:8080/api/editor/articles/pending
curl -u ed:secret1 -X POST localhost:8080/api/editor/articles/1/approve
# or reject:
# curl -u ed:secret1 -X POST localhost:8080/api/editor/articles/1/reject \
#   -H 'Content-Type: application/json' -d '{"comment":"Needs sources"}'

# 5. Public now sees the article
curl "localhost:8080/api/public/articles?category=world"
```

## Project structure

```
com.example.news
├── NewsApplication              entry point, enables JPA auditing
├── entity/
│   ├── BaseEntity               id, @Version, createdAt, updatedAt
│   ├── AppUser, Role            users and their role
│   └── Article, ArticleStatus   article + workflow rules (state machine)
├── repo/                        Spring Data repositories (derived queries, @EntityGraph)
├── dto/Dtos                     request/response records with validation
├── service/
│   ├── UserService              user creation and soft delete
│   └── ArticleService           workflow, ownership checks, public queries
├── security/
│   ├── SecurityConfig           URL-to-role rules, BCrypt, HTTP Basic
│   └── DbUserDetailsService     loads users from the database
├── web/                         controllers per audience + GlobalExceptionHandler
└── config/AdminSeeder           creates the first admin
```
