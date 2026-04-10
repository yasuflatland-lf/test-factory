# liferay-dummy-factory

A Liferay Portal 7.4 (CE GA132) workspace featuring a portlet built with MVCPortlet + React, along with Testcontainers-based integration tests.

## Project Structure

```
liferay-dummy-factory/
├── modules/
│   └── liferay-dummy-factory/   # OSGi bundle (Portlet + Web)
│       └── src/main/
│           ├── java/              # MVCPortlet, MVCResourceCommand, services, constants
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
| Backend | MVCPortlet + MVCResourceCommand (layered) |
| Frontend | React + Clay CSS |
| Build | Gradle 8.5 + Liferay Workspace Plugin 10.1.9 |
| Testing | Spock 2.4 / Groovy 5.0 / Testcontainers 2.0.4 / Playwright 1.59.0 |
| Java | JDK 21 |

## Quick Start (Docker)

Steps to start Liferay with Docker and deploy the portlet.

### 1. Start the Liferay Container

```bash
# Foreground mode (view logs directly, Ctrl+C to stop)
docker run -it -m 8g -p 8080:8080 -p 11311:11311 \
  -e LIFERAY_SETUP_PERIOD_WIZARD_PERIOD_ENABLED=false \
  -e LIFERAY_TERMS_PERIOD_OF_PERIOD_USE_PERIOD_REQUIRED=false \
  -e LIFERAY_USERS_PERIOD_REMINDER_PERIOD_QUERIES_PERIOD_ENABLED=false \
  -e LIFERAY_PASSWORDS_PERIOD_DEFAULT_PERIOD_POLICY_PERIOD_CHANGE_PERIOD_REQUIRED=false \
  liferay/portal:7.4.3.132-ga132

# Background mode (with a named container)
docker run -d --name liferay -m 8g -p 8080:8080 -p 11311:11311 \
  -e LIFERAY_SETUP_PERIOD_WIZARD_PERIOD_ENABLED=false \
  -e LIFERAY_TERMS_PERIOD_OF_PERIOD_USE_PERIOD_REQUIRED=false \
  -e LIFERAY_USERS_PERIOD_REMINDER_PERIOD_QUERIES_PERIOD_ENABLED=false \
  -e LIFERAY_PASSWORDS_PERIOD_DEFAULT_PERIOD_POLICY_PERIOD_CHANGE_PERIOD_REQUIRED=false \
  liferay/portal:7.4.3.132-ga132
```

| Option | Description |
|-----------|------|
| `-m 8g` | Memory limit 8GB |
| `-p 8080:8080` | HTTP port |
| `-p 11311:11311` | GoGo Shell port (for checking bundle status) |
| Environment variables | Disable setup wizard, terms of use, reminder queries, and forced password change |

Startup takes approximately 5-8 minutes. If running in background mode, check with:

```bash
docker logs -f liferay   # When the "Server startup" message appears, startup is complete. Ctrl+C to exit.
```

To restart an existing container:

```bash
docker start liferay && docker logs -f liferay
```

### 2. Build and Deploy the Portlet

```bash
# Build the module JAR
./gradlew :modules:liferay-dummy-factory:jar

# Deploy the JAR to the container
docker cp modules/liferay-dummy-factory/build/libs/liferay.dummy.factory-1.0.0.jar liferay:/opt/liferay/deploy/

# Verify deployment (if STARTED appears, deployment is successful)
docker logs -f liferay 2>&1 | grep -i "dummy.factory"
```

### 3. Verify Operation

Navigate to http://localhost:8080 and log in as admin (`test@liferay.com` / `test`).
**Liferay Dummy Factory** will appear under Control Panel > Configuration.

Check bundle status via GoGo Shell:

```bash
docker exec liferay bash -c "(echo 'lb dummy.factory'; sleep 2) | telnet localhost 11311"
```

## Build

```bash
# Module build
./gradlew :modules:liferay-dummy-factory:build
```

## Testing

Requires Docker to be running.

```bash
# Run all integration tests
./gradlew :integration-test:integrationTest

# Run a specific spec
./gradlew :integration-test:integrationTest --tests "com.liferay.support.tools.it.spec.DeploymentSpec"
```

Tests automatically start a Liferay container via Testcontainers and verify:

- **DeploymentSpec** -- Bundle deployment and activation (via GoGo Shell)
- **PortletRenderSpec** -- Login and portlet rendering through the browser (Playwright)
- **OrganizationFunctionalSpec** -- Organization batch creation via portlet UI with REST API verification (Playwright)

## CI

GitHub Actions (`.github/workflows/integration-test.yml`) runs automatically on push / PR to `master`.
