# Test Factory Calculator -- Architecture

## 1. Project Structure

```
test-factory/
  settings.gradle            # Liferay Workspace plugin + includes integration-test
  gradle.properties          # liferay.workspace.product=portal-7.4-ga132
  build.gradle
  modules/
    test-factory-calculator/ # Single OSGi bundle (api + service + web)
      bnd.bnd
      service.xml
      src/main/java/         # Java: portlet, service builder, constants
      src/main/resources/
        META-INF/resources/
          js/Calculator.js   # React frontend component
  integration-test/          # Separate Gradle subproject at the repo root
    src/test/groovy/         # Spock/Groovy tests (Testcontainers + Playwright)
```

## 2. Calculator Module

**Single-JAR design** -- API, service, and web layers live in one bundle (`com.liferay.test.factory`). The `bnd.bnd` exports `model`, `service`, `service.persistence`, and `exception` packages so other bundles can consume the API, while the portlet and React UI ship in the same JAR.

**Service Builder entity -- `CalcEntry`**

| Column     | Type   | Purpose                        |
|------------|--------|--------------------------------|
| calcEntryId| long   | Primary key                    |
| num1       | double | First operand                  |
| num2       | double | Second operand                 |
| operator   | String | `+`, `-`, `*`, `/`             |
| result     | double | Computed result                |

Standard audit columns (`companyId`, `userId`, `userName`, `createDate`, `modifiedDate`) are included. Results are ordered by `createDate` descending, with a finder on `userId`.

**MVCPortlet + PanelApp** -- The portlet (`com_liferay_test_factory_TestFactoryPortlet`) is registered in the Control Panel under Configuration. The view JSP renders the React component via the `<react:component>` tag.

**React frontend** -- `Calculator.js` is a functional React component using `useState`. It renders a form with two number inputs, an operator dropdown (`+`, `-`, `*`, `/`), and a calculate button. Results and errors are shown inline.

## 3. Data Flow

```
React UI
  |  FormData: num1, num2, operator, serviceContext
  |  Header: x-csrf-token = Liferay.authToken
  v
fetch POST /api/jsonws/TestFactory.CalcEntry/calculate
  |
  v
CalcEntryServiceImpl          (remote service -- permission checks)
  |
  v
CalcEntryLocalServiceImpl     (local service -- business logic + persistence)
  |
  v
Database (CalcEntry table)
  |
  v
JSON response  -->  React sets result state or displays error
```

The frontend uses `credentials: 'include'` so Liferay session cookies are sent automatically. The CSRF token comes from the global `Liferay.authToken` object.

## 4. Integration Test Module

The `integration-test` subproject is included via `settings.gradle` at the repository root. It is intentionally **not** placed under `modules/` to avoid the Liferay Workspace Gradle plugin applying Service Builder and BundleSupport tasks to it.

Key components:

- **LiferayContainer** (`LiferayContainer.groovy`) -- A Testcontainers `GenericContainer` subclass wrapping `liferay/portal:7.4.3.132-ga132`. Singleton pattern (`getInstance()`) ensures one container per test run. Exposes HTTP (8080) and GogoShell (11311) ports. Waits for the Catalina startup log message (up to 8 minutes). Pre-configures setup wizard, terms-of-use, and reminder query to be disabled.
- **JAR deployment** -- `deployJar(Path)` copies the built module JAR into `/opt/liferay/deploy/` inside the container.
- **GogoShell verification** -- Tests connect to the mapped GogoShell port (11311) to verify bundle state.
- **Playwright UI tests** -- Browser-based tests hit the container's mapped HTTP port to exercise the calculator through the real UI.

## 5. Key Design Decisions

| Decision | Rationale |
|----------|-----------|
| **Single JAR** (`apiDir = src/main/java` in Service Builder) | Avoids splitting into three modules (`-api`, `-service`, `-web`) for a small, self-contained calculator. Exported packages in `bnd.bnd` still allow external consumption of the API. |
| **integration-test at repo root** | The Liferay Workspace plugin auto-applies to everything under `modules/`. Placing the test project at the root and including it explicitly in `settings.gradle` keeps it as a plain Gradle project with no Liferay plugin interference. |
| **`liferay/portal` CE image for tests** | Uses the free Community Edition Docker image (`liferay/portal:7.4.3.132-ga132`) so integration tests run without a DXP license. The workspace targets `portal-7.4-ga132` (Portal CE) for development. |
| **Container reuse (`withReuse(true)`)** | Liferay startup is slow (~8 min). Reusing the container across test runs drastically shortens feedback loops during development. |
| **Singleton container** | `getInstance()` with `synchronized` guarantees all test classes share a single Liferay instance, preventing resource waste and port conflicts. |
