# Testing

## Framework Versions

| Dependency      | Version          |
|-----------------|------------------|
| Spock           | 2.4-groovy-5.0   |
| Groovy          | 5.0.4            |
| Testcontainers  | 2.0.4            |
| Playwright      | 1.59.0           |

## Container Setup

- Docker image: `liferay/portal:7.4.3.132-ga132` (CE GA132).
- Singleton pattern: `LiferayContainer.getInstance()` starts one container per test run and shares it across all specs. Never create a second container instance inside a single run.
- Startup timeout: **8 minutes** (log-based wait strategy matching Catalina startup message).
- Exposed ports: **8080** (HTTP) and **11311** (GoGo Shell). Access via `liferay.httpPort` / `liferay.gogoPort` (mapped ports).
- Environment variables disable the setup wizard, terms-of-use prompt, and reminder queries so tests run unattended.
- **Container reuse is disabled** (`withReuse(false)`). Every Gradle run starts a fresh container so state from a previous run (created users, roles, sites, password changes) cannot leak across runs and mask regressions. Do not flip this to `true` for local speedups — state drift will cause false passes.

## Verification Strategy: Prefer JSONWS

- **Prefer Liferay JSONWS (`/api/jsonws/...`) over Playwright/UI navigation for verifying test outcomes.** JSONWS calls are faster, deterministic, and do not depend on Control Panel rendering or portlet UI state.
- Use Playwright when the assertion is about DOM/rendering, client-side validation, or navigation flows. For database-state assertions ("did the entity actually get created / updated / deleted?"), query JSONWS.
- Authenticate JSONWS calls with Basic Auth using the default admin credentials (`test@liferay.com` / `test`).
- See `BaseLiferaySpec` for `jsonwsGet` / `jsonwsPost` helpers, and any `*FunctionalSpec` under `integration-test/.../spec/` for usage.

## Deploy Verification

1. The module JAR is copied into the container at `/opt/liferay/deploy/` using `liferay.deployJar(path)`.
2. The JAR must be pre-built: run `./gradlew :modules:liferay-dummy-factory:jar` before running tests.
3. Bundle activation is verified via GoGo Shell: `lb | grep dummy.factory` must show `Active` or `ACTIVE`.
4. The `ensureBundleActive()` method in `BaseLiferaySpec` polls GoGo Shell every 5 seconds for up to 5 minutes until the bundle is active. It is synchronized and runs only once per test suite.

## PortletTracker and jakarta.portlet Compatibility (CE GA132)

The PortletTracker in CE 7.4 GA132 tracks `javax.portlet.Portlet` services, **not** `jakarta.portlet.Portlet`. If the portlet's `@Component` declares `service = jakarta.portlet.Portlet.class`, the PortletTracker will not detect it and the corresponding `com.liferay.portal.kernel.model.Portlet` OSGi service will never be registered. This causes any PanelApp with a `@Reference` to that portlet model to stay in an **UNSATISFIED** state.

**Debugging steps:**

1. Run `scr:info <component-name>` in GoGo Shell and look for `UNSATISFIED REFERENCE` entries. If the portlet model reference is unsatisfied, the PortletTracker is not registering the portlet.
2. Run `headers <bundle-id>` in GoGo Shell and inspect the `Import-Package` header to verify which portlet API package the bundle actually imports (`javax.portlet` vs `jakarta.portlet`).
3. If the bundle imports `jakarta.portlet`, switch the portlet `@Component` to declare `service = javax.portlet.Portlet.class` (and use `javax.portlet` imports) so the PortletTracker can detect it on CE GA132.

## Playwright (Browser Tests)

- Runs **headless Chromium** via `PlaywrightLifecycle`.
- Login credentials: `test@liferay.com` / `test` (Liferay default admin).
- Navigate to portlets via **direct URL** with the portlet ID in the query string (`p_p_id=...&p_p_lifecycle=0`). Do not click through menus -- direct navigation is faster and more reliable.
- Use CSS selectors for locators (`#count`, `.alert-success`, `button.btn-primary`, `[type=submit]`).
- Set explicit timeouts on waits: `waitForURL(..., new Page.WaitForURLOptions().setTimeout(30_000))`, `waitFor(new Locator.WaitForOptions().setTimeout(15_000))`.
- Close the `PlaywrightLifecycle` instance in `cleanupSpec()` using safe-navigation: `pw?.close()`.

## Running Tests

```bash
# Build the module JAR first (required)
./gradlew :modules:liferay-dummy-factory:jar

# Run integration tests (requires Docker)
./gradlew :integration-test:integrationTest
```

- The module build depends on `release.portal.api` (Portal CE), **not** `release.dxp.api`. Using the wrong dependency artifact will cause build failures or runtime class-loading issues against the CE Docker image.
- The default `test` task is **disabled** (`enabled = false`). All integration tests run exclusively via the `integrationTest` task.
- The `integrationTest` task automatically depends on `:modules:liferay-dummy-factory:jar`, so a standalone `./gradlew :integration-test:integrationTest` will build the JAR first.
- JVM args: `-Xms4g -Xmx4g`.
- Test logging outputs `passed`, `skipped`, `failed`, `standardOut`, and `standardError`.

## Coverage (JaCoCo)

Host-JVM unit tests under `modules/liferay-dummy-factory/src/test/java` are measured by JaCoCo. The report is generated automatically after `test` via `finalizedBy`.

```bash
./gradlew :modules:liferay-dummy-factory:test
```

Report locations:
- HTML: `modules/liferay-dummy-factory/build/reports/jacoco/test/html/index.html`
- XML:  `modules/liferay-dummy-factory/build/reports/jacoco/test/jacocoTestReport.xml`

**Scope limitation**: Only the host-JVM unit tests (JUnit 5) are covered. Integration tests run against a containerized Liferay JVM (Testcontainers) and are **not** instrumented in Step 1. Coverage for the containerized JVM requires injecting the JaCoCo agent into Liferay's `CATALINA_OPTS` and will be handled in a later step.

## Adding New Tests

1. Create a new Groovy class under `integration-test/src/test/groovy/com/liferay/support/tools/it/spec/`.
2. **Extend `BaseLiferaySpec`** -- this gives you the shared `liferay` container instance and `ensureBundleActive()`.
3. Call `ensureBundleActive()` in `setupSpec()` (or in the first test) to guarantee the bundle is deployed and active before your tests run.
4. Use `@Stepwise` if your tests must execute in declaration order (e.g., login then interact).
5. For browser tests, instantiate `PlaywrightLifecycle` as a `@Shared` field in `setupSpec()` and close it in `cleanupSpec()`.
