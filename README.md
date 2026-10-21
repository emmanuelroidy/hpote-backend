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
| `APP_CORS_ALLOWED_ORIGINS` | Optional comma-separated browser origins; defaults to local portal and Flutter web URLs |
| `GRAPHHOPPER_API_KEY` | Optional GraphHopper Directions API key; required for in-app routes and must stay on the backend |
| `LIBRETRANSLATE_URL` | Optional local LibreTranslate `/translate` endpoint; defaults to `http://localhost:5001/translate` |
| `LIBRETRANSLATE_API_KEY` | Optional key if the local LibreTranslate instance is configured to require one |

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
| `GET` | `/api/service-requests/{id}/navigation` | Customer or assigned professional gets the other party's saved destination after acceptance |
| `GET` | `/api/service-requests/{id}/route` | Get a GraphHopper route for an accepted request; query parameters: `fromLatitude`, `fromLongitude`, `profile` (`car`, `bike`, `foot`), and optional `weighting` (`recommended`, `fastest`, `shortest`) |
| `POST` | `/api/service-requests/{id}/start` | Assigned professional starts the job |
| `POST` | `/api/service-requests/{id}/complete` | Assigned professional marks the job complete |
| `POST` | `/api/service-requests/{id}/cancel` | Request owner cancels an open request |

Requests accept either the legacy `pictureUrl` string or a `pictureUrls` array
with up to five authenticated image URLs. Responses include both fields so
older clients continue to work; `pictureUrl` remains the first photo.

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

GraphHopper route requests are proxied by the authenticated backend so the API
key is never shipped to the app. Set `GRAPHHOPPER_API_KEY` in the backend
environment to enable in-app route geometry and turn instructions. The free
GraphHopper Directions API tier still requires an API key. Without one, the app
keeps the map and external map-app directions available and explains how to
enable in-app routing.

## Administration API

All `/api/admin/**` routes require a JWT for a user with role `ADMIN`. Responses
use the standard envelope (`success`, `statusCode`, `message`, `data`). User
objects are safe DTOs and include `enabled`, `professionIds`, and profession
names, plus `createdAt` as the account's join date (never the password or reset
credentials). IDs must be UUIDs.

| Method | Route | Purpose |
| --- | --- | --- |
| `GET` | `/api/admin/overview` | Exact user, role, enabled/blocked, profession, and service-request counts |
| `GET` | `/api/admin/service-requests` | List all marketplace requests, newest first, with requester, status, and photo URLs |
| `POST` | `/api/admin/service-requests/{id}/cancel` | Cancel an open request during moderation |
| `GET` | `/api/admin/users` | List users |
| `GET` | `/api/admin/users/{id}` | Get a user |
| `PATCH` | `/api/admin/users/{id}/status` | Enable or block with `{"enabled":false}` |
| `PUT` | `/api/admin/users/{id}/role` | Assign `NORMAL`, `PROFESSIONAL`, or `ADMIN` with `{"role":"PROFESSIONAL","professionIds":["<uuid>"]}` |
| `PUT` | `/api/admin/users/{id}/professions` | Set a professional's professions with `{"professionIds":["<uuid>"]}` |
| `GET` | `/api/admin/professions` | List all professions, including inactive ones |
| `POST` | `/api/admin/professions` | Create with `{"name":"Painter","description":"...","isActive":true}` |
| `PUT` | `/api/admin/professions/{id}` | Replace name/description, optionally set `isActive` |
| `PATCH` | `/api/admin/professions/{id}/status` | Set activation with `{"isActive":false}` |
| `DELETE` | `/api/admin/professions/{id}` | Deactivate (soft delete; preserves historical references) |

Duplicate profession assignments and duplicate profession names are rejected;
only active professions can be assigned. A professional must retain at least
one profession. Supply `professionIds` when changing a user to `PROFESSIONAL`
if they do not already have an assignment. The API prevents self-blocking/self-demotion and blocking or
demoting the last administrator. Changing a user away from `PROFESSIONAL`
clears their profession assignments. The public `GET /api/professions` only
returns active professions.

## App translations

The app supports English and Swahili and can select other language tags as
translation dictionaries are added. Admins manage dictionary entries in the
operations portal under **Translations**. Dictionary entries take precedence;
only missing text is sent to the configured LibreTranslate service. Generated
translations are saved as editable automatic entries. The app and backend do
not send text to a public translation provider.

To run the free, open-source translator locally, start LibreTranslate on host
port `5001` with English and Swahili models enabled:

```sh
docker run --rm -p 5001:5000 libretranslate/libretranslate --load-only en,sw
```

Set `LIBRETRANSLATE_URL` if the service uses a different address. App dictionary
and translation-resolution APIs are public under `/api/public/translations`;
dictionary editing endpoints under `/api/admin/translations` require an
administrator JWT.

## Request-photo upload

`POST /api/media/images` accepts authenticated multipart form data with the
field name `file`. JPEG and PNG images up to 5 MB are accepted; file content
and image dimensions are validated. The response is an API envelope with
`data.url`, ready to store as the service request's `pictureUrl`:

```json
{"success":true,"statusCode":200,"message":"Image uploaded","data":{"url":"/api/media/images/<uuid>"}}
```

By default this URL is relative to the API origin, so clients should resolve
it against their configured backend base URL. Set `APP_MEDIA_PUBLIC_BASE_URL`
to the externally reachable API origin to receive an absolute URL instead.
Files are stored under `APP_MEDIA_UPLOAD_DIR` (default
`uploads/customer-requests`); mount/persist that directory in deployments.
`GET /api/media/images/{uuid}` serves the image and requires a valid bearer
token as request photos may contain personal information. Configure
`APP_MEDIA_PUBLIC_BASE_URL` when clients need an absolute image URL.
