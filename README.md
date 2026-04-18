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
* Liferay DXP 2026.q1.3-lts (this branch)
* A valid DXP activation key at `configs/local/deploy/activation-key.xml`

Older CE / DXP versions are maintained on legacy branches (`7.3.x`, `7.2.x`, `7.1.x`, `7.0.x`) and are not covered by this README.

> For development rules and contracts, start with [`CLAUDE.md`](CLAUDE.md) and the task-based files under [`.claude/rules/`](.claude/rules/). Concrete details live under [`docs/details/`](docs/details/), and architectural decisions in [`docs/ADR/`](docs/ADR/).


## Tech Stack

| Layer | Technology |
|-------|------------|
| Portal | Liferay DXP 2026.q1.3-lts (`liferay/dxp:2026.q1.3-lts`) |
| Portlet API | `jakarta.portlet` 4.0 |
| Backend | MVCPortlet + MVCResourceCommand (layered) |
| Frontend | React + Clay CSS |
| Build | Gradle 8.5 + Liferay Workspace Plugin 16.0.5 |
| Testing | Spock 2.4 / Groovy 5.0 / Workspace-native Docker flow / Playwright 1.59.0 |
| Java | JDK 21 |

## Usage

| Version | Link                                                                                                                                                       |
|---------|:-----------------------------------------------------------------------------------------------------------------------------------------------------------|
| DXP 2026.q1.3-lts | [https://github.com/yasuflatland-lf/liferay-dummy-factory/tree/master/latest](https://github.com/yasuflatland-lf/liferay-dummy-factory/tree/master/latest) |
| 7.3 (legacy) | [https://github.com/yasuflatland-lf/liferay-dummy-factory/tree/7.3.x/latest](https://github.com/yasuflatland-lf/liferay-dummy-factory/tree/7.3.x/latest)   |
| 7.2 (legacy) | [https://github.com/yasuflatland-lf/liferay-dummy-factory/tree/7.2.x/latest](https://github.com/yasuflatland-lf/liferay-dummy-factory/tree/7.2.x/latest)   |
| 7.1 (legacy) | [https://github.com/yasuflatland-lf/liferay-dummy-factory/tree/7.1.x/latest](https://github.com/yasuflatland-lf/liferay-dummy-factory/tree/7.1.x/latest)   |
| 7.0 (legacy) | [https://github.com/yasuflatland-lf/liferay-dummy-factory/tree/7.0.x/latest](https://github.com/yasuflatland-lf/liferay-dummy-factory/tree/7.0.x/latest)   |

1. Download the jar from the `latest/` directory of the appropriate branch and place it into `${liferay-home}/deploy`.
1. Start the Liferay bundle and login as an administrator.
1. Once the jar is active, navigate to `Control Panel` → `Apps` → **Dummy Factory**.
1. Create dummy data. Enjoy.

## Quick Start (Workspace-native Docker flow)

The integration-test harness and the recommended local workflow both rely on the Liferay workspace plugin's Docker tasks. You do NOT need to invoke `docker run` by hand — the workspace plugin builds the image, copies the activation key from `configs/local/deploy/`, and starts / stops the container.

### 1. Place the DXP activation key

```bash
cp /path/to/activation-key.xml configs/local/deploy/activation-key.xml
```

The file is `.gitignore`-d. CI stores it as a base64-encoded secret and decodes it into the same path before invoking Gradle.

### 2. Build the bundle JAR

```bash
./gradlew :modules:liferay-dummy-factory:jar
```

The artifact lands at `modules/liferay-dummy-factory/build/libs/liferay.dummy.factory-1.0.0.jar`.

### 3. Start the DXP container and deploy

```bash
./gradlew createDockerContainer startDockerContainer
./gradlew dockerDeploy
```

`dockerDeploy` runs the full task graph (`createDockerfile` → `buildDockerImage` → `createDockerContainer` → `startDockerContainer`) and copies the generated JAR into the running container's `/opt/liferay/deploy/` directory via `docker cp`.

| Port | Purpose |
|------|---------|
| 8080 | HTTP |
| 11311 | GoGo Shell (telnet for bundle status) |

Startup takes approximately 5–8 minutes on first run. Watch the log:

```bash
docker logs -f test-factory-liferay
```

### 4. Verify

Navigate to `http://localhost:8080` and log in as `test@liferay.com` / `test`. **Dummy Factory** appears under Control Panel → Apps.

Check bundle status via GoGo Shell:

```bash
docker exec test-factory-liferay bash -c "(echo 'lb dummy.factory'; sleep 2) | telnet localhost 11311"
```

`Active` / `ACTIVE` means the bundle is deployed.

### 5. Stop / remove the container

```bash
./gradlew stopDockerContainer
```

The workspace plugin configures `autoRemove`, so the container is removed on stop.

## Testing

Requires Docker to be running and the activation key in place.

```bash
# Run all integration tests
./gradlew :integration-test:integrationTest --info

# Run a specific spec
./gradlew :integration-test:integrationTest --tests "com.liferay.support.tools.it.spec.DeploymentSpec"
```

The `:integration-test:integrationTest` task depends on `createDockerContainer` → `startDockerContainer` → `awaitLiferayReady` and finalizes with `stopDockerContainer`. No separate Testcontainers runtime is involved; the workspace plugin owns the container lifecycle end-to-end.

Spec catalog:

- **DeploymentSpec** — bundle deployment and activation (via GoGo Shell)
- **PortletRenderSpec** — login and portlet rendering through the browser (Playwright)
- **OrganizationFunctionalSpec** — organization batch creation via portlet UI with REST API verification (Playwright)
- Additional `*Spec.groovy` under `integration-test/src/test/groovy/` per entity type.

## CI

GitHub Actions (`.github/workflows/integration-test.yml`) runs automatically on push / PR to `master`. The workflow decodes `LIFERAY_DXP_LICENSE_BASE64` into `configs/local/deploy/activation-key.xml` before invoking Gradle.
