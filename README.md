# HpOtE Backend

HpOtE is a Spring Boot API for connecting people who need a service with local
professionals. The marketplace flow is request → professional quotes → customer
selects a quote → work starts → work completes.

## Requirements

- Java 17
- PostgreSQL
- Maven (or the included `./mvnw`)

Configure these environment variables before starting the application:

| Variable | Purpose |
| --- | --- |
| `DB_URL` | JDBC connection URL, for example `jdbc:postgresql://localhost:5432/hpote` |
| `DB_USERNAME` | Database username |
| `DB_PASSWORD` | Database password |
| `JWT_SECRET` | Random signing secret of at least 32 UTF-8 bytes |
| `MAIL_HOST` | SMTP server |
| `MAIL_PORT` | SMTP port |
| `MAIL_USERNAME` | SMTP account |
| `MAIL_PASSWORD` | SMTP account password |
| `PORT` | Optional HTTP port; defaults to `8080` |

Start locally with `./mvnw spring-boot:run` and run tests with `./mvnw test`.
The application initializes the default roles, professions, and service states
on startup. Hibernate currently updates the schema automatically.

For the Postman collection and request-by-request walkthrough, see the
[Postman API guide](./docs/POSTMAN-API-GUIDE.md) and import
[HpOtE.postman_collection.json](./postman/HpOtE.postman_collection.json).

## Authentication and onboarding

Register at `POST /api/users/register`, then authenticate at
`POST /api/users/login`. Send the returned JWT on authenticated requests as
`Authorization: Bearer <token>`.

A professional registers with role `PROFESSIONAL` and one or more `professionIds`
(public registration does not allow the `ADMIN` role). Then configure their
service location and availability using
`PUT /api/professionals/profile`:

```json
{
  "professionId": "primary-profession-uuid",
  "latitude": -6.8,
  "longitude": 39.2,
  "serviceRadiusKm": 25,
  "isAvailable": true
}
```

The profile's coordinates and radius are used for nearby matching; professionals
can receive requests for any profession attached to their account.

## Service marketplace API

All routes below require a JWT.

| Method | Route | Purpose |
| --- | --- | --- |
| `POST` | `/api/service-requests` | Customer creates a request |
| `GET` | `/api/service-requests/mine` | List the caller's requests |
| `GET` | `/api/service-requests/available` | Professional finds open matching requests within their radius |
| `GET` | `/api/service-requests/assigned` | Professional lists jobs assigned to them |
| `GET` | `/api/service-requests/my-responses` | Professional tracks submitted quotes |
| `POST` | `/api/service-requests/{id}/responses` | Professional submits a quote |
| `GET` | `/api/service-requests/{id}/responses` | Request owner views quotes |
| `POST` | `/api/service-requests/{id}/responses/{responseId}/accept` | Request owner chooses a professional |
| `POST` | `/api/service-requests/{id}/start` | Assigned professional starts the job |
| `POST` | `/api/service-requests/{id}/complete` | Assigned professional marks the job complete |
| `POST` | `/api/service-requests/{id}/cancel` | Request owner cancels an open request |

Create a service request with a profession ID and location:

```json
{
  "professionId": "profession-uuid",
  "description": "Repair a leaking kitchen tap",
  "pictureUrl": "https://example.com/tap.jpg",
  "latitude": -6.8,
  "longitude": 39.2
}
```

Professionals respond with a quote; currency is a three-letter ISO code:

```json
{
  "quoteAmount": 45000,
  "currencyCode": "TZS",
  "message": "I can inspect and repair the tap this afternoon.",
  "estimatedDays": 1
}
```

Requests move through `OPEN`, `ACCEPTED`, `IN_PROGRESS`, and `COMPLETED`, or
from `OPEN` to `CANCELLED`. Once an owner accepts a response, other responses
are closed and only that professional can advance the job.
