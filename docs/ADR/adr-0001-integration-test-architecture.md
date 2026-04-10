# ADR-0001: Integration Test Architecture for Liferay Portal CE

## Status

Accepted (partially implemented — PanelApp navigation on CE pending)

## Date

2026-04-09

## Context

The liferay-dummy-factory project needs to build E2E integration tests using Testcontainers for the calculator portlet (MVCPortlet + React).

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

**Decision**: Sequential trial of multiple passwords + `portal-ext.properties` placement.

- Place `portal-ext.properties` in `/opt/liferay/tomcat/webapps/ROOT/WEB-INF/classes/` (via `withCopyToContainer`)
  - However, `passwords.default.policy.change.required=false` does not take effect on the Docker image's pre-built database
- The test side tries both `test` and `Test12345` in sequence, also handling containers where the password change has been persisted via `withReuse(true)`
- If the password change page appears, it is handled automatically

**Rejected alternatives**:
- **Environment variable `LIFERAY_PASSWORDS_DEFAULT_POLICY_CHANGE_REQUIRED`**: Not recognized by Liferay
- **DB update via GoGo Shell**: Direct SQL execution is not possible from the OSGi console
- **Groovy script execution**: `groovy:exec` in GoGo Shell is not available in the CE Docker image

### 4. JAR Deployment Method: Copy via /tmp + chown

**Decision**: Place the file in `/tmp` using `copyFileToContainer`, then use `execInContainer` to `cp` + `chown liferay:liferay` and move it to `/opt/liferay/deploy/`.

**Rationale**: `copyFileToContainer` creates files owned by root, but Liferay's AutoDeployScanner runs as the liferay user (uid=1000). Copying directly to `/deploy/` causes an `Unable to write` error.

### 5. GoGo Shell Bundle Verification: Full Output Retrieval + Java-Side Filtering

**Decision**: Retrieve the full output of the `lb` command (approximately 1394 lines) and filter for lines containing `Liferay Dummy Factory` on the Java/Groovy side.

**Rationale**: GoGo Shell is an OSGi console and does not support Unix shell pipes (`|`) or the `grep` command. Running `lb | grep dummy.factory` simply causes the `grep` command to return `false`.

### 6. Container Configuration

```groovy
withReuse(true)                    // Reuse container since startup takes 2-3 minutes
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
- `withReuse(true)` enables fast test execution during development (no container startup required)
- Installing only Chromium reduces download time
- Testcontainers 2.0.4 provides compatibility with the latest Docker Engine 29.x

### Negative

- **No Global Menu in CE**: The DXP-only Global Menu (`Open Applications Menu`) does not exist in CE GA132. Browser navigation to the PanelApp (`CONTROL_PANEL_CONFIGURATION`) remains unresolved.
- Multiple password trial logic is required to handle password changes persisted by `withReuse(true)`
- Some properties in `portal-ext.properties` (e.g., `passwords.default.policy.change.required`) have no effect on the Docker image's pre-built database

### Open Questions

1. **PanelApp navigation on CE GA132**: The Control Panel section does not appear in the Product Menu sidebar. Direct URL access (`/group/control_panel/manage`) returns 404. The following options are under consideration:
   - Change the PanelApp's `panel.category.key` to place it in the Site Administration section
   - Change the portlet's `display-category` to make it deployable on widget pages and switch to page placement testing
   - Identify the correct access path to the Control Panel on CE (requires manual browser verification)

2. **Playwright version**: Currently using 1.59.0. Consider updating as needed while maintaining compatibility with the official Liferay tests.

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
