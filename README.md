# test-factory

A Liferay Portal 7.4 (CE GA132) workspace featuring a Calculator portlet built with Service Builder + React, along with Testcontainers-based integration tests.

## Project Structure

```
test-factory/
├── modules/
│   └── test-factory-calculator/   # OSGi bundle (API + Service + Web)
│       ├── service.xml            # CalcEntry entity definition
│       └── src/main/
│           ├── java/              # MVCPortlet, Service Builder
│           └── resources/
│               └── META-INF/resources/
│                   └── js/        # React frontend
├── integration-test/              # Spock + Testcontainers + Playwright
│   └── src/test/groovy/
└── .github/workflows/             # CI (GitHub Actions)
```

## Tech Stack

| Layer | Technology |
|-------|------------|
| Portal | Liferay Portal 7.4.3.132-ga132 |
| Backend | Service Builder (CalcEntry entity) |
| Frontend | React + Clay CSS |
| Build | Gradle 8.5 + Liferay Workspace Plugin 10.1.9 |
| Testing | Spock 2.4 / Groovy 5.0 / Testcontainers 2.0.4 / Playwright 1.42.0 |
| Java | JDK 21 |

## Build

```bash
# Service Builder code generation
./gradlew :modules:test-factory-calculator:buildService

# Module build
./gradlew :modules:test-factory-calculator:build
```

## Testing

Requires Docker to be running.

```bash
# Run all integration tests
./gradlew :integration-test:integrationTest

# Run a specific spec
./gradlew :integration-test:integrationTest --tests "com.liferay.test.factory.it.spec.DeploymentSpec"
```

Tests automatically start a Liferay container via Testcontainers and verify:

- **DeploymentSpec** -- Bundle deployment and activation (via GoGo Shell)
- **CalculatorHappyPathSpec** -- Login and calculation through the browser (Playwright)

## CI

GitHub Actions (`.github/workflows/integration-test.yml`) runs automatically on push / PR to `master`.
