# DXP 2026.Q1.3-LTS — Runtime and Environment Gotchas

L3 detail. Concrete pitfalls discovered during DXP 2026 migration (PR #42 learnings + expert input). Read on demand from `.claude/rules/debugging.md`.

## 1. `portal-liferay-online-config.properties` baked into the DXP base image

**Symptom**: Every JSONWS call returns HTTP 403 or "Access denied" immediately after the container starts. No `configs/` change seems to help.

**Root cause**: `liferay/dxp:2026.q1.3-lts` bakes `/home/liferay/portal-liferay-online-config.properties` into the image. This file contains:

```
json.servlet.hosts.allowed=N/A
```

This value blocks all JSONWS access from any host, including `localhost`.

**Fix**: Place an empty file at `configs/common/portal-liferay-online-config.properties`. The workspace plugin merges `configs/common/` into `/home/liferay/` at image build time, and an empty file shadows the baked-in one.

```
# configs/common/portal-liferay-online-config.properties
# Intentionally empty — shadows the baked-in DXP file that sets
# json.servlet.hosts.allowed=N/A and blocks all JSONWS access.
```

**Verification**: After container start, run:

```bash
docker exec <container-name> cat /home/liferay/portal-liferay-online-config.properties
```

The output should be empty (or show only the comment). If it shows `json.servlet.hosts.allowed=N/A`, the shadow file was not deployed correctly.

## 2. BasicAuth URL glob must end with `/*`

**Symptom**: JSONWS calls using Basic Auth credentials return 401 or redirect to the login page, even though Basic Auth is enabled in `portal-ext.properties`.

**Root cause**: The `auth.verifier.BasicAuthHeaderAuthVerifier.urls.includes` property uses Liferay URL glob patterns. The terminator `/*` is required for path-prefix matching. Without it, only the exact path matches.

**Correct entry**:

```properties
auth.verifier.BasicAuthHeaderAuthVerifier.urls.includes=/api/*,/portal/api/*,/o/*,/xmlrpc/*
```

Note that both `/api/*` (legacy path, some endpoints still respond there) and `/portal/api/*` (DXP 2026 JSONWS base) must be listed.

## 3. AuthVerifierPipeline cache delay (~3 minutes)

**Symptom**: After updating `auth.verifier.*` properties in `portal-ext.properties` and restarting Tomcat (without rebuilding the image), Basic Auth requests still fail for up to 3 minutes.

**Root cause**: The `AuthVerifierPipeline` caches its configuration. The cache clears after approximately 3 minutes.

**Note**: Not typically observed during test runs because the container is disposable per run; a fresh AuthVerifierPipeline starts cold each time. It is noted here for cases where `portal-ext.properties` is hot-updated during debugging.

## 4. License activation: readiness polling watch-point

License activation races the first HTTP request. Current implementation uses a simple HTTP 200/302 readiness poll (see `awaitLiferayReady` in `integration-test/build.gradle`). If DXP 2026's license-activation sequence ever redirects to `/portal/update_language` or similar, extend the polling to inspect the `Location` header.

## 5. `Accept-Encoding: identity` must be forced on HTTP test requests

**Symptom**: `BaseLiferaySpec` helper methods occasionally return garbled JSON or trigger `JsonParseException` when the Liferay server returns gzip-compressed responses.

**Root cause**: Java's `HttpURLConnection` does not automatically decompress `Content-Encoding: gzip` responses unless the `java.net.http.HttpClient` API is used. The Groovy `URL.openConnection()` pattern used in test helpers receives the compressed bytes and tries to parse them as JSON.

**Fix**: Set `Accept-Encoding: identity` on all outbound test HTTP requests:

```groovy
conn.setRequestProperty('Accept-Encoding', 'identity')
```

This tells the server not to compress the response. Applied in `BaseLiferaySpec._httpGet` and `_httpPost`.

## 6. Workspace plugin ≥16 requires explicit `include` for all subprojects in `settings.gradle`

**Symptom**: `:modules:liferay-dummy-factory:jar` task is not found; Gradle reports "project ':modules:liferay-dummy-factory' not found".

**Root cause**: Workspace plugin 16.x no longer auto-discovers subprojects. Every subproject must be declared explicitly.

**Fix** in `settings.gradle`:

```groovy
include 'integration-test'
include 'modules:liferay-dummy-factory'
```

Both lines are required. Without the `modules:liferay-dummy-factory` line, the bundle JAR is never built and `integrationTest` fails immediately.

## 7. Headless `portal-instances list` endpoint has an indexing lag

**Symptom**: `GET /o/headless-admin-user/v1.0/organizations?search=<name>` returns an empty list immediately after a create call, even though the organization was created successfully.

**Root cause**: The `?search=` parameter routes through Elasticsearch. There is an observable indexing lag (typically 1–5 seconds, but potentially longer under load) between when the entity is persisted and when it appears in search results.

**Fix**: Use `?pageSize=100` without `?search=`, then filter client-side by name. For post-condition assertions in specs, always use the page-and-filter pattern:

```groovy
def resp = jsonwsGet("organization/get-organizations?companyId=${companyId}&parentOrganizationId=0&start=0&end=100")
def match = resp.list.find { it.name == expectedName }
assert match != null
```

## 8. Fixed Docker port conflict warning

The workspace plugin always binds fixed ports:

| Port | Service |
|------|---------|
| 8080 | HTTP |
| 11311 | GoGo Shell |
| 8000 | JPDA (debug) |

If another Liferay instance (or any other process) is already bound to port 8080 or 11311, `startDockerContainer` will fail with "port is already allocated".

**Before running `./gradlew startDockerContainer`**:

```bash
docker ps                # check for running containers
lsof -i :8080            # check for any process on 8080
lsof -i :11311           # check for any process on 11311
```

Stop any conflicting process or container first. There is no configuration option to change these ports without modifying the workspace plugin task directly.

## 9. Admin bootstrap is suppressed by `portal-ext.properties` properties

DXP 2026 normally requires an admin password change on first login, which triggers a 7-step HTTP ticket flow (`PASSWORDRESET`). This is suppressed by two properties in `configs/common/portal-ext.properties`:

```properties
company.security.update.password.required=false
passwords.default.policy.change.required=false
```

With these set, the DXP admin account (`test@liferay.com` / `test`) is usable immediately after container startup. `BaseLiferaySpec` does not need to handle the password change flow. If these properties are accidentally removed or overridden, all Playwright-based specs will fail at the login step with the admin redirect to the password change page.
