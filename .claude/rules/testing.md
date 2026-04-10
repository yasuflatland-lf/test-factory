# Testing

## Framework Versions

| Dependency      | Version          |
|-----------------|------------------|
| Spock           | 2.4-groovy-5.0   |
| Groovy          | 5.0.4            |
| Testcontainers  | 2.0.4            |
| Playwright      | 1.42.0           |

## Container Setup

- Docker image: `liferay/portal:7.4.3.132-ga132` (CE GA132).
- Singleton pattern: `LiferayContainer.getInstance()` starts the container once and reuses it across all specs. Never create a second container instance.
- Startup timeout: **8 minutes** (log-based wait strategy matching Catalina startup message).
- Exposed ports: **8080** (HTTP) and **11311** (GoGo Shell). Access via `liferay.httpPort` / `liferay.gogoPort` (mapped ports).
- Environment variables disable the setup wizard, terms-of-use prompt, and reminder queries so tests run unattended.
- Container reuse is enabled (`withReuse(true)`) to speed up repeated local runs.

## Deploy Verification

1. The calculator JAR is copied into the container at `/opt/liferay/deploy/` using `liferay.deployJar(path)`.
2. The JAR must be pre-built: run `./gradlew :modules:test-factory-calculator:jar` before running tests.
3. Bundle activation is verified via GoGo Shell: `lb | grep test.factory` must show `Active` or `ACTIVE`.
4. The `ensureDeployed()` method in `BaseLiferaySpec` polls GoGo Shell every 5 seconds for up to 5 minutes until the bundle is active. It is synchronized and runs only once per test suite.

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
- Use CSS selectors for locators (`#num1`, `.alert-success`, `button.btn-primary`, `[type=submit]`).
- Set explicit timeouts on waits: `waitForURL(..., new Page.WaitForURLOptions().setTimeout(30_000))`, `waitFor(new Locator.WaitForOptions().setTimeout(15_000))`.
- Close the `PlaywrightLifecycle` instance in `cleanupSpec()` using safe-navigation: `pw?.close()`.

## Running Tests

```bash
# Build the module JAR first (required)
./gradlew :modules:test-factory-calculator:jar

# Run integration tests (requires Docker)
./gradlew :integration-test:integrationTest
```

- The module build depends on `release.portal.api` (Portal CE), **not** `release.dxp.api`. Using the wrong dependency artifact will cause build failures or runtime class-loading issues against the CE Docker image.
- The default `test` task is **disabled** (`enabled = false`). All integration tests run exclusively via the `integrationTest` task.
- The `integrationTest` task automatically depends on `:modules:test-factory-calculator:jar`, so a standalone `./gradlew :integration-test:integrationTest` will build the JAR first.
- JVM args: `-Xmx1g`.
- Test logging outputs `passed`, `skipped`, `failed`, `standardOut`, and `standardError`.

## Adding New Tests

1. Create a new Groovy class under `integration-test/src/test/groovy/com/liferay/test/factory/it/spec/`.
2. **Extend `BaseLiferaySpec`** -- this gives you the shared `liferay` container instance and `ensureDeployed()`.
3. Call `ensureDeployed()` in `setupSpec()` (or in the first test) to guarantee the bundle is deployed and active before your tests run.
4. Use `@Stepwise` if your tests must execute in declaration order (e.g., login then interact).
5. For browser tests, instantiate `PlaywrightLifecycle` as a `@Shared` field in `setupSpec()` and close it in `cleanupSpec()`.
