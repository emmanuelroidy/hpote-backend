# HpOtE API: Postman guide

This guide works with the included Postman collection:
[`postman/HpOtE.postman_collection.json`](../postman/HpOtE.postman_collection.json).
Import the JSON file in Postman using **Import → Files**.

## Start the backend

The API listens on port `8080` by default. It requires a PostgreSQL database,
JWT signing secret, and SMTP settings. Set these environment variables before
starting it:

```text
DB_URL=jdbc:postgresql://localhost:5432/hpote
DB_USERNAME=your_database_user
DB_PASSWORD=your_database_password
JWT_SECRET=use-a-random-secret-of-at-least-32-utf8-bytes
MAIL_HOST=your-smtp-host
MAIL_PORT=587
MAIL_USERNAME=your-smtp-user
MAIL_PASSWORD=your-smtp-password
```

Then run `./mvnw spring-boot:run`. The API base URL is `http://localhost:8080`.
Configure these variables in your deployment platform for a deployed API.

## Postman variables and authorization

The collection defines `baseUrl`, `customerToken`, `professionalToken`,
`professionId`, `requestId`, and `responseId`. Set `baseUrl` to the running
backend URL. The login requests save their JWTs automatically; choose the
appropriate registered account for the two login requests.

Authenticated requests use the collection's bearer-token authorization and
select `customerToken` or `professionalToken` as appropriate. Registration,
login, forgot-password, and reset-password are public. Other requests require a
valid JWT.

## Suggested end-to-end run order

1. Call **Users → Register customer**, then **Login customer**.
2. Call **Professions → List professions** with the customer token and copy an
   `id` into `professionId`. The collection also saves the first returned
   profession ID automatically.
3. Call **Users → Register professional**, then **Login professional**. The
   professional registration body refers to `professionId`.
4. Call **Professional → Create or update my profile** as the professional.
5. Call **Customer → Create request** as the customer. The collection saves
   the returned request ID automatically.
6. Call **Professional → Find nearby requests**, then **Submit quote**. The
   quote response ID is saved automatically.
7. Call **Customer → View request quotes**, then **Accept quote**.
8. Call **Professional → List assigned jobs**, **Start job**, and **Complete
   job** in order.

Use different phone numbers and email addresses for each registration. Public
registration accepts `NORMAL` and `PROFESSIONAL` roles; it does not permit
creating an administrator.

## Request reference

All successful API responses use an envelope containing `success`,
`statusCode`, `message`, and `data`.

### Users and professions

| Method | Path | Access |
| --- | --- | --- |
| `POST` | `/api/users/register` | Public |
| `POST` | `/api/users/login` | Public |
| `POST` | `/api/users/forgot-password` | Public |
| `POST` | `/api/users/reset-password` | Public |
| `GET` | `/api/users/{id}` | Own account or admin |
| `GET` | `/api/users/all` | Admin |
| `GET` | `/api/professions` | Authenticated |

Customer registration body:

```json
{
  "name": "Test Customer",
  "phone": "255700000001",
  "email": "customer@example.com",
  "password": "StrongPass123",
  "role": "NORMAL"
}
```

Professional registration body:

```json
{
  "name": "Test Professional",
  "phone": "255700000002",
  "email": "professional@example.com",
  "password": "StrongPass123",
  "role": "PROFESSIONAL",
  "professionIds": ["{{professionId}}"]
}
```

Login body (username can be email or phone):

```json
{
  "username": "customer@example.com",
  "password": "StrongPass123"
}
```

Request a password-reset code with `POST /api/users/forgot-password`:

```json
{
  "email": "customer@example.com"
}
```

Then submit the emailed six-digit code to `POST /api/users/reset-password`:

```json
{
  "email": "customer@example.com",
  "otp": "123456",
  "newPassword": "NewStrongPass123"
}
```

`GET /api/users/{id}` is limited to the account owner or an administrator.
`GET /api/users/all` is administrator-only.

### Professional profile

`PUT /api/professionals/profile` creates or updates the authenticated
professional's profile. A professional can only select a profession assigned
to their account.

```json
{
  "professionId": "{{professionId}}",
  "latitude": -6.8,
  "longitude": 39.2,
  "serviceRadiusKm": 25,
  "isAvailable": true
}
```

`GET /api/professionals/profile` returns the authenticated professional's
profile.

### Service marketplace

| Method | Path | Access |
| --- | --- | --- |
| `POST` | `/api/service-requests` | Authenticated customer |
| `GET` | `/api/service-requests/mine` | Authenticated customer |
| `GET` | `/api/service-requests/available` | Available professional |
| `GET` | `/api/service-requests/assigned` | Professional |
| `GET` | `/api/service-requests/my-responses` | Professional |
| `GET` | `/api/service-requests/{requestId}/navigation` | Customer or assigned professional after acceptance |
| `POST` | `/api/service-requests/{requestId}/responses` | Matching professional |
| `GET` | `/api/service-requests/{requestId}/responses` | Request owner |
| `POST` | `/api/service-requests/{requestId}/responses/{responseId}/accept` | Request owner |
| `POST` | `/api/service-requests/{requestId}/start` | Assigned professional |
| `POST` | `/api/service-requests/{requestId}/complete` | Assigned professional |
| `POST` | `/api/service-requests/{requestId}/cancel` | Request owner; open requests only |

Create a request:

```json
{
  "professionId": "{{professionId}}",
  "description": "Repair a leaking kitchen tap",
  "pictureUrl": "https://example.com/tap.jpg",
  "latitude": -6.8,
  "longitude": 39.2
}
```

Submit a quote (currency must be a three-letter uppercase code):

```json
{
  "quoteAmount": 45000,
  "currencyCode": "TZS",
  "message": "I can inspect and repair the tap this afternoon.",
  "estimatedDays": 1
}
```

Requests transition from `OPEN` to `ACCEPTED`, `IN_PROGRESS`, and `COMPLETED`,
or from `OPEN` to `CANCELLED`. Accepting one quote declines the competing
quotes. Only the assigned professional can start or complete the request.

After a quote is accepted, the navigation response returns
`startLatitude`/`startLongitude` for the authenticated customer or
professional and `endLatitude`/`endLongitude` for the other party's saved
location. For a customer, the request location is the start and the
professional's profile location is the end; for the assigned professional,
the start and end are reversed. The legacy `destinationLatitude` and
`destinationLongitude` fields remain aliases for the end coordinates.
