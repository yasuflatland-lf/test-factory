# liferay-dummy-factory

[![Build Status](https://travis-ci.org/yasuflatland-lf/liferay-dummy-factory.svg?branch=master)](https://travis-ci.org/yasuflatland-lf/liferay-dummy-factory)
[![Coverage Status](https://coveralls.io/repos/github/yasuflatland-lf/liferay-dummy-factory/badge.svg)](https://coveralls.io/github/yasuflatland-lf/liferay-dummy-factory)

Dummy Factory generates dummy data for debugging use. Please don't use this for a production use.

## What does Dummy Factory generate?

* Organizations
* Sites
* Pages
* Users
* Web Content Articles
* Documents
* Message Board (Threads / Categories)
* Category (Categories / Vocabularies)
* Wiki
* Blogs
* Company

## Required environment
* Java 21 or above
* Liferay 7.4 GA1 (Master / Develop branch)
* Liferay 7.3 GA1 (Master / Develop branch)
* Liferay 7.2 (Please see 7.2.x branch)
* Liferay 7.1 (Please see 7.1.x branch)
* Liferay 7.0 (Please see 7.0.x branch)

> For development rules and contracts, start with [`CLAUDE.md`](CLAUDE.md) and the task-based files under [`.claude/rules/`](.claude/rules/). Concrete details live under [`docs/details/`](docs/details/), and architectural decisions in [`docs/ADR/`](docs/ADR/).


## Tech Stack

| Layer | Technology |
|-------|------------|
| Portal | Liferay Portal 7.4.3.132-ga132 |
| Backend | MVCPortlet + MVCResourceCommand (layered) |
| Frontend | React + Clay CSS |
| Build | Gradle 8.5 + Liferay Workspace Plugin 10.1.9 |
| Testing | Spock 2.4 / Groovy 5.0 / Testcontainers 2.0.4 / Playwright 1.59.0 |
| Java | JDK 21 |

## Usage
| Version | Link                                                                                                                                                       | 
|---------|:-----------------------------------------------------------------------------------------------------------------------------------------------------------| 
| 7.4     | [https://github.com/yasuflatland-lf/liferay-dummy-factory/tree/master/latest](https://github.com/yasuflatland-lf/liferay-dummy-factory/tree/master/latest) |
| 7.3     | [https://github.com/yasuflatland-lf/liferay-dummy-factory/tree/7.3.x/latest](https://github.com/yasuflatland-lf/liferay-dummy-factory/tree/7.3.x/latest)   | 
| 7.2     | [https://github.com/yasuflatland-lf/liferay-dummy-factory/tree/7.2.x/latest](https://github.com/yasuflatland-lf/liferay-dummy-factory/tree/7.2.x/latest)   | 
| 7.1     | [https://github.com/yasuflatland-lf/liferay-dummy-factory/tree/7.1.x/latest](https://github.com/yasuflatland-lf/liferay-dummy-factory/tree/7.1.x/latest)   | 
| 7.0     | [https://github.com/yasuflatland-lf/liferay-dummy-factory/tree/7.0.x/latest](https://github.com/yasuflatland-lf/liferay-dummy-factory/tree/7.0.x/latest)   | 

1. Download jar file according to the version above and place it int `${liferay-home}/deploy ` 
1. Start Liferay bundle and login as an administrator.
1. After the jar is properly installed, navigate to `Control Panel -> System Settings -> Platform -> Thrid party` and enable JQuery.
1. Reboot the bundle.
1. Navigate to `Control Panel`, under `Marketplace`, `Dummy Factory` will be found.
1. Now you are ready to create dummy data! Enjoy!

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
./gradlew :integration-test:integrationTest --info

# Run a specific spec
./gradlew :integration-test:integrationTest --tests "com.liferay.support.tools.it.spec.DeploymentSpec"
```

Tests automatically start a Liferay container via Testcontainers and verify:

- **DeploymentSpec** -- Bundle deployment and activation (via GoGo Shell)
- **PortletRenderSpec** -- Login and portlet rendering through the browser (Playwright)
- **OrganizationFunctionalSpec** -- Organization batch creation via portlet UI with REST API verification (Playwright)

## CI

GitHub Actions (`.github/workflows/integration-test.yml`) runs automatically on push / PR to `master`.
