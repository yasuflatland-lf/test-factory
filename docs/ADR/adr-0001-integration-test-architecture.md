# ADR-0001: Integration Test Architecture

> **Partially superseded by ADR-0008** — This ADR originally specified a Testcontainers-driven harness on Liferay Portal CE 7.4 GA132. The project has since migrated to Liferay DXP 2026.q1.3-lts with the Liferay workspace plugin's Docker tasks owning the container lifecycle (see `adr-0008-dxp-2026-migration.md`). The decisions below are preserved for historical context. The parts still in force today are explicitly called out in the "Current status" section at the bottom.

## Status

Accepted (2026-04-09). Partially superseded by ADR-0008 (2026-04-15).

## Context (historical)

The liferay-dummy-factory project needed end-to-end integration tests for the portlet (MVCPortlet + React) against a running Liferay instance.

### Constraints at the time

- **Target**: Liferay Portal CE 7.4 GA132 (Community Edition, not DXP)
- **Runtime environment**: WSL2 + Docker Desktop 29.x
- **Build**: Gradle 8.5 + Liferay Workspace Plugin
- **Test scope**: OSGi bundle deployment verification, bundle state validation via GoGo Shell, and browser E2E tests via Playwright

## Decisions (historical)

### 1. Test framework configuration

| Component | Choice | Rationale |
|-----------|--------|-----------|
| Test framework | Spock 2.4 + Groovy 5.0.4 | Groovy's concise syntax, test ordering via `@Stepwise`, Power Assert |
| Container management | Testcontainers 2.0.4 | Docker Engine 29.x support (bundles docker-java 3.7.1) |
| Browser tests | Playwright Java 1.59.0 (Chromium only) | Same technology stack as the official Liferay tests |
| GoGo Shell communication | Apache Commons Net (Telnet) | Used to connect to the Liferay OSGi console |

### 2. Login method: API POST with CSRF token

Adopted the same approach as the official Liferay Playwright tests (`performLoginViaApi`):

```
1. page.navigate("/") to establish a session
2. page.evaluate("() => Liferay.authToken") to obtain the CSRF token
3. page.request().post("/c/portal/login") with the CSRF token
4. page.navigate("/") to reload
```

**Rejected alternatives**:
- **Form-based login**: the redirect to `/web/guest` did not carry the session, and the password-change page needed conditional handling
- **Basic authentication**: disabled by default in the DXP/CE image at the time

### 3. Password policy handling

`LIFERAY_PASSWORDS_DEFAULT_POLICY_CHANGE_REQUIRED=false` env var to patch the pre-built DB at container startup, plus a fallback password trial and an interactive handler. `portal-ext.properties` was used for the non-password settings that do take effect from properties (setup wizard, terms-of-use, reminder queries).

### 4. JAR deployment via /tmp + chown

`copyFileToContainer` creates files owned by root, but Liferay's AutoDeployScanner runs as the liferay user (uid=1000). Workaround: place the file in `/tmp`, then `docker exec` to `cp` + `chown liferay:liferay` into `/opt/liferay/deploy/`.

### 5. GoGo Shell bundle verification: full output + Java-side filtering

`lb` outputs ~1394 lines. GoGo Shell is an OSGi console and does not support Unix pipes — `lb | grep dummy.factory` simply returns `false`. The harness retrieves the full output and filters for `Liferay Dummy Factory` on the Java/Groovy side.

### 6. Verification strategy: JSONWS first, Playwright only for UI

Post-condition assertions (did the entity actually get created / updated / deleted?) go through Liferay JSONWS (`/api/jsonws/...`) with Basic Auth. Playwright is reserved for behavior that is genuinely UI-specific (rendering, client-side validation, navigation flows).

JSONWS is faster and deterministic, and does not depend on Control Panel rendering or portlet UI state. The Elasticsearch-backed headless REST endpoints (`/o/headless-admin-user/...`) also have observable indexing lag that makes post-create reads non-deterministic; JSONWS goes directly through the service layer and avoids both issues.

**This decision is still in force on DXP 2026.**

### 7. Creator services declare `throws Throwable`

The public `create(...)` method on every `*Creator` service in `com.liferay.support.tools.service` declares `throws Throwable`, and the `*ResourceCommand` callers use `catch (Throwable t)`. This preserves the distinction between `PortalException` subtypes and avoids re-wrapping the original throwable.

The trade-off — `catch (Throwable)` also catches `Error` subtypes — is accepted: the JVM error state surfaces as a failed action rather than silently killing the worker thread, and the portlet container continues serving other requests.

**This decision is still in force on DXP 2026.**

### 8. Container configuration (historical)

```groovy
withReuse(false)
withCopyToContainer(...)           // Place portal-ext.properties
withEnv([
    'LIFERAY_SETUP_WIZARD_ENABLED': 'false',
    'LIFERAY_TERMS_OF_USE_REQUIRED': 'false',
    'LIFERAY_USERS_REMINDER_QUERY_ENABLED': 'false',
])
```

The `withReuse(false)` posture is still in force; the wiring is now done by the workspace plugin, not Testcontainers.

## Consequences (historical)

- Login method follows the official Liferay Playwright test patterns.
- `withReuse(false)` guarantees a clean Liferay state for every test run, so entities (users, roles, sites) or password changes from a previous run cannot leak into the next and hide regressions.
- Installing only Chromium reduces download time.
- Testcontainers 2.0.4 provided compatibility with Docker Engine 29.x.

### Observed limitations

- **No Global Menu in CE 7.4**: resolved by using direct URL access with `p_p_state=maximized` (e.g., `/group/control_panel/manage?p_p_id=...&p_p_lifecycle=0&p_p_state=maximized`). This workaround is still used on DXP 2026, where the Global Menu exists but direct URL access is faster and less flaky for tests.
- Each test run pays the full ~8 minute container startup cost, since container reuse is disabled — an intentional trade-off to preserve test isolation.
- Some properties in `portal-ext.properties` (e.g., `passwords.default.policy.change.required`) had no effect on the Docker image's pre-built database; these were handled by the equivalent `LIFERAY_*` environment variables instead.

## Current status (DXP 2026)

ADR-0008 migrated the harness to the Liferay workspace plugin's Docker tasks. The impact on this ADR:

- **§1 container management — replaced.** Testcontainers is no longer used. `LiferayContainer` is a POJO holding constants (`host=localhost`, `httpPort=8080`, `gogoPort=11311`, `container=test-factory-liferay`); the workspace plugin owns start/stop via `createDockerContainer` → `startDockerContainer` → `stopDockerContainer`. The `deployJar(Path)` helper uses `docker cp` via `ProcessBuilder` against the running container.
- **§2 login via API POST — still in force, but with a bootstrap step.** DXP 2026 ships the admin user with `USER_.PASSWORDRESET=1` baked into HSQL. `BaseLiferaySpec.bootstrapAdminCredentials()` walks the official `/sign-in` → `/c/portal/login` → `/c/portal/update_password` flow once per container lifetime to clear the flag. See ADR-0008 "DXP 2026 Baked-In Admin Password Reset Trap".
- **§3 password policy handling — replaced** by the admin bootstrap in §2 above.
- **§4 JAR deployment via /tmp + chown — simplified.** `LiferayContainer.deployJar` now runs `docker cp modules/.../build/libs/liferay.dummy.factory-1.0.0.jar test-factory-liferay:/opt/liferay/deploy/`; the workspace plugin's generated image already runs Liferay as uid=1000 and the `docker cp` target inherits liferay:liferay ownership.
- **§5 GoGo Shell verification — unchanged.**
- **§6 JSONWS-first verification — unchanged**, with the caveat that the JSONWS base path on DXP 2026 is `/portal/api/jsonws/` (portal-context-mounted). See `docs/details/dxp-2026-gotchas.md`.
- **§7 Creator `throws Throwable` — unchanged.**
- **§8 container configuration — replaced.** Config overrides now live in `configs/common/` (shared) and `configs/local/` (env-specific). The workspace plugin mounts them via `/mnt/liferay/files/` at container start.

For the full DXP 2026 harness contract, read ADR-0008.

## References

- Liferay Portal source: `/home/yasuflatland/tmp/liferay-portal`
- Official Liferay Playwright tests: `modules/test/playwright/`
  - `utils/performLogin.ts` — API login pattern
  - `helpers/ApiHelpers.ts` — CSRF token retrieval
  - `utils/productMenu.ts` — Product Menu
- ADR-0008 — Complete Migration to Liferay DXP 2026.q1.3-lts (the current harness contract)

## Release cadence caveat (Playwright Java vs Node)

Playwright Java (`com.microsoft.playwright:playwright` on Maven Central) and Playwright Node/CLI (`playwright` on npm) follow independent release cadences. A given `1.x.y` tag does not necessarily exist on both channels. For example, as of 2026-04 the latest stable on npm is `1.59.1`, while the latest on Maven Central is `1.59.0`; `1.59.1` and `1.60.0` have not yet been published to Maven Central.

The Playwright project recommends keeping the client library and the driver pinned to the same version because the wire protocol is only guaranteed to be compatible within a single release. Mixing a Java client with a different Node driver version can fail at runtime with opaque protocol errors.

When bumping the Playwright version, always confirm the target POM actually exists on Maven Central before changing any build file:

```
curl -s -o /dev/null -w "%{http_code}" https://repo.maven.apache.org/maven2/com/microsoft/playwright/playwright/<version>/playwright-<version>.pom
```

A `200` response means the artifact is available; anything else (typically `404`) means the version is npm-only and must not be adopted on the Java side yet. The `test.playwright.version` property in `gradle.properties` and the `npx playwright@<version>` invocation in the CI workflow must always reference the same version so the Java client and the Node driver stay in lockstep.
