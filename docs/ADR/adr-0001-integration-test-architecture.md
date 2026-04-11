# ADR-0001: Integration Test Architecture for Liferay Portal CE

## Status

Accepted

## Date

2026-04-09

## Context

The liferay-dummy-factory project needs to build E2E integration tests using Testcontainers for the portlet (MVCPortlet + React).

### Constraints

- **Target**: Liferay Portal CE 7.4 GA132 (Community Edition, not DXP)
- **Runtime environment**: WSL2 + Docker Desktop 29.x
- **Build**: Gradle 8.5 + Liferay Workspace Plugin
- **Test scope**: OSGi bundle deployment verification, bundle state validation via GoGo Shell, and browser E2E tests via Playwright

## Decision

### 1. Test Framework Configuration

| Component | Choice | Rationale |
|-----------|--------|-----------|
| Test framework | Spock 2.4 + Groovy 5.0.4 | Groovy's concise syntax, test ordering via `@Stepwise`, Power Assert |
| Container management | Testcontainers 2.0.4 | Docker Engine 29.x support (bundles docker-java 3.7.1). Version 1.21.x ships a shaded docker-java supporting up to API v1.44, which is incompatible with Docker 29.x's minimum API v1.40 |
| Browser tests | Playwright Java 1.59.0 (Chromium only) | Same technology stack as the official Liferay tests. Installing only Chromium reduces download time |
| GoGo Shell communication | Apache Commons Net (Telnet) | Used to connect to the Liferay OSGi console |

### 2. Login Method: API POST (with CSRF Token)

**Decision**: Adopt the same approach as the official Liferay Playwright tests (`performLoginViaApi`).

```
1. page.navigate("/") to establish a session
2. page.evaluate("() => Liferay.authToken") to obtain the CSRF token
3. page.request().post("/c/portal/login") with the CSRF token
4. page.navigate("/") to reload
```

**Rejected alternatives**:
- **Form-based login**: After login, redirecting to `/web/guest` causes the session to not carry over. Conditional handling for the password change page is complex.
- **Basic authentication**: Disabled by default in Liferay CE.

### 3. Password Policy Handling

**Decision**: `LIFERAY_PASSWORDS_DEFAULT_POLICY_CHANGE_REQUIRED=false` env var + fallback password trial + interactive handler.

- Set `LIFERAY_PASSWORDS_DEFAULT_POLICY_CHANGE_REQUIRED=false` so the pre-built DB is patched at container startup and the default admin password is accepted as-is.
- `portal-ext.properties` is still placed in `/opt/liferay/tomcat/webapps/ROOT/WEB-INF/classes/` for the non-password settings that do take effect from properties (setup wizard, terms-of-use, reminder queries).
- The test side still tries both `test` and `Test12345` in sequence to tolerate both fresh containers and any future image change that re-enables the policy. If the password change page still appears, it is handled automatically.

**Rejected alternatives**:
- **DB update via GoGo Shell**: Direct SQL execution is not possible from the OSGi console.
- **Groovy script execution**: `groovy:exec` in GoGo Shell is not available in the CE Docker image.

### 4. JAR Deployment Method: Copy via /tmp + chown

**Decision**: Place the file in `/tmp` using `copyFileToContainer`, then use `execInContainer` to `cp` + `chown liferay:liferay` and move it to `/opt/liferay/deploy/`.

**Rationale**: `copyFileToContainer` creates files owned by root, but Liferay's AutoDeployScanner runs as the liferay user (uid=1000). Copying directly to `/deploy/` causes an `Unable to write` error.

### 5. GoGo Shell Bundle Verification: Full Output Retrieval + Java-Side Filtering

**Decision**: Retrieve the full output of the `lb` command (approximately 1394 lines) and filter for lines containing `Liferay Dummy Factory` on the Java/Groovy side.

**Rationale**: GoGo Shell is an OSGi console and does not support Unix shell pipes (`|`) or the `grep` command. Running `lb | grep dummy.factory` simply causes the `grep` command to return `false`.

### 6. Verification Strategy: JSONWS First, Playwright Only for UI

**Decision**: Post-condition assertions (did the entity actually get created / updated / deleted?) go through Liferay JSONWS (`/api/jsonws/...`) with Basic Auth. Playwright is reserved for behavior that is genuinely UI-specific (rendering, client-side validation, navigation flows).

**Rationale**: JSONWS is faster and deterministic, and it does not depend on Control Panel rendering or portlet UI state. Relying on Playwright for data assertions couples the test outcome to transient UI layout, and the Elasticsearch-backed headless REST endpoints (`/o/headless-admin-user/...`) have observable indexing lag that makes post-create reads non-deterministic; JSONWS goes directly through the service layer and avoids both issues.

### 7. Creator Services Declare `throws Throwable`

**Decision**: The public `create(...)` method on every `*Creator` service in `com.liferay.support.tools.service` declares `throws Throwable`, and the `*ResourceCommand` callers use `catch (Throwable t)`.

**Rationale**: `TransactionInvokerUtil.invoke(TransactionConfig, Callable)` is declared `throws Throwable` — its `Callable`-shaped lambda lets the transaction machinery surface both checked exceptions and `Error`s. Adding a `catch (Throwable) { throw new Exception(t); }` bridge inside each Creator would let the public signature return to `throws Exception`, but it would also (a) flatten the distinction between `PortalException` subtypes that the ResourceCommand can decide to render as user errors, and (b) re-wrap the original throwable, obscuring the root cause in logs.

**Trade-off accepted**: `catch (Throwable)` at the ResourceCommand layer will also catch `Error` subtypes (`OutOfMemoryError`, `StackOverflowError`, `LinkageError`). In the portlet request path this is acceptable: the JVM's error state is reported to the user as a failed action rather than silently killing the worker thread, and the portlet container will continue to serve other requests. If this ever becomes a problem, the bridge-and-rethrow pattern can be added in the Creators without changing the ResourceCommand contract.

### 8. Container Configuration

```groovy
withReuse(false)                   // Always start a fresh container to prevent state leakage
withCopyToContainer(...)           // Place portal-ext.properties
withEnv([                          // Environment variables
    'LIFERAY_SETUP_WIZARD_ENABLED': 'false',
    'LIFERAY_TERMS_OF_USE_REQUIRED': 'false',
    'LIFERAY_USERS_REMINDER_QUERY_ENABLED': 'false',
])
```

## Consequences

### Positive

- Login method follows the official Liferay Playwright test patterns
- `withReuse(false)` guarantees a clean Liferay state for every test run, so entities (users, roles, sites) or password changes from a previous run cannot leak into the next run and hide regressions
- Installing only Chromium reduces download time
- Testcontainers 2.0.4 provides compatibility with the latest Docker Engine 29.x

### Negative

- **No Global Menu in CE**: The DXP-only Global Menu (`Open Applications Menu`) does not exist in CE GA132. Resolved by using direct URL access with `p_p_state=maximized` (e.g., `/group/control_panel/manage?p_p_id=...&p_p_lifecycle=0&p_p_state=maximized`).
- Each test run pays the full ~8 minute container startup cost, since container reuse is disabled. This is an intentional trade-off to preserve test isolation.
- Some properties in `portal-ext.properties` (e.g., `passwords.default.policy.change.required`) have no effect on the Docker image's pre-built database. These are handled by the equivalent `LIFERAY_*` environment variables instead.

### Resolved Questions

1. **PanelApp navigation on CE GA132**: Resolved by using direct URL access with `p_p_state=maximized` query parameter. The URL pattern `/group/control_panel/manage?p_p_id=<portlet_id>&p_p_lifecycle=0&p_p_state=maximized` renders the portlet directly without needing the Product Menu sidebar or Global Menu.

### Open Questions

1. **Playwright version**: Currently using 1.59.0. Consider updating as needed while maintaining compatibility with the official Liferay tests.

## References

- Liferay Portal source: `/home/yasuflatland/tmp/liferay-portal`
- Official Liferay Playwright tests: `modules/test/playwright/`
  - `utils/performLogin.ts` -- API login pattern
  - `helpers/ApiHelpers.ts` -- CSRF token retrieval
  - `pages/product-navigation-applications-menu/GlobalMenuPage.ts` -- Global Menu (DXP)
  - `utils/productMenu.ts` -- Product Menu
  - `env/portal-ext.properties` -- Test properties
- Testcontainers source: `/home/yasuflatland/tmp/testcontainers-java`
- Detailed implementation plan: `.claude/plan/integrationtest.md`
