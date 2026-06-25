# WiFi Admin

Spring Boot REST wrapper around a SOAP WiFi management platform. Exposes `GET /wifi-parameter/{cpeId}` and `PUT /wifi-parameter`, backed by the SOAP service defined in `wsdl/wifi-platform.wsdl`. Responses are cached in H2 and a nightly scheduler keeps the DB in sync. Includes a React frontend.

## Technologies

- Java 21, Spring Boot 3.5, Gradle
- Spring Web Services (`WebServiceTemplate`) for the SOAP client
- Spring Data JPA + H2 + Flyway
- Spring Security (stateless, no auth enforced — see security note below)
- SpringDoc OpenAPI (Swagger UI at `/swagger-ui.html`)
- JUnit 5, Mockito, WireMock, JaCoCo
- React 18, Vite, TypeScript, Axios

## Project Structure

```
backend/src/main/java/.../wifiadmin/
    api/            REST controller, DTOs, exception handler
    application/    WifiParameterService, validator, scheduler
    domain/         WifiConfiguration, WifiBand, EncryptionType
    infrastructure/ SOAP client, persistence, security, observability

frontend/src/
    api/            wifiApi.ts
    components/     WifiLookupForm, WifiConfigurationForm, HealthStatus, ErrorAlert
    types/          wifi.ts
```

## Running

Start the SOAP platform mock:

```bash
docker compose up -d
```

Run the backend (local profile — H2 in-memory, debug logging):

```bash
cd backend
gradlew.bat bootRun --args="--spring.profiles.active=local"   # Windows
./gradlew bootRun --args='--spring.profiles.active=local'     # Linux/macOS
```

Run the frontend:

```bash
cd frontend
npm install
npm run dev
```

Backend runs on `http://localhost:8081`, frontend on `http://localhost:5173`.

H2 console (local only): `http://localhost:8081/h2-console` — `jdbc:h2:mem:wifiadmin` / `sa` / no password.

## Docker

To run the full stack (mock + backend + frontend):

```bash
docker compose -f docker-compose.app.yml up --build
```

## REST API

**GET** `/wifi-parameter/{cpeId}` — returns WiFi configuration. Served from DB if cached, otherwise fetched from the SOAP platform and stored.

```bash
curl http://localhost:8081/wifi-parameter/CPE_001
```

**PUT** `/wifi-parameter` — updates configuration via the SOAP platform and persists the confirmed response.

```bash
curl -X PUT http://localhost:8081/wifi-parameter \
  -H "Content-Type: application/json" \
  -d '{"cpeId":"CPE_001","wifiBand":"BAND_2_4_GHZ","ssid":"Office-2G-Updated","encryptionType":"WPA2_PSK","password":"newpassword123"}'
```

Password is required when `encryptionType` is anything other than `OPEN`. Errors are returned as `{"message": "...", "code": "..."}` with appropriate HTTP status codes (400, 404, 502).

## Testing

```bash
cd backend
gradlew.bat clean test    # Windows
./gradlew clean test      # Linux/macOS
```

Covers unit tests (validator, service, scheduler), `@WebMvcTest` controller tests, WireMock-based SOAP client tests, `@DataJpaTest` persistence tests, and a `@SpringBootTest` integration test. JaCoCo report is generated at `backend/build/reports/jacoco/test/html/index.html`.

## Security note

Spring Security is wired (stateless, CSRF disabled, CORS for `http://localhost:5173`, `X-Frame-Options: DENY`) but authentication is not enforced. The endpoints are public to keep the assignment easy to evaluate. OAuth2/JWT can be added later by changing `permitAll` to `.authenticated()` and adding a resource server dependency.
