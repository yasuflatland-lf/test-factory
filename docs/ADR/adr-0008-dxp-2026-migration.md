# ADR-0008: Complete Migration to Liferay DXP 2026.q1.3-lts

## Status

Accepted

## Context

The project was originally built for Liferay CE 7.4 GA132. CE requires `javax.portlet` (Portlet API 3.0) because CE 7.4's PortletTracker only recognizes `javax.portlet.Portlet` components. DXP 2026.q1.3-lts ships Portlet API 4.0 and exports `jakarta.portlet;version='4.0'` from the OSGi framework. CE backward compatibility is no longer a requirement.

## Decision

Migrate exclusively to Liferay DXP 2026.q1.3-lts:

- **Docker image**: `liferay/dxp:2026.q1.3-lts`
- **Workspace product**: `dxp-2026.q1.3-lts`
- **Build dependency**: `release.dxp.api` (replaces `release.portal.api`)
- **Portlet namespace**: `jakarta.portlet.*` (replaces `javax.portlet.*`)
- **Portlet API version property**: `jakarta.portlet.version=4.0` (replaces `javax.portlet.version=3.0`)
- **OSGi @Reference target**: `(jakarta.portlet.name=...` (replaces `(javax.portlet.name=...`)
- **Language.properties key prefix**: `jakarta.portlet.title.*` (replaces `javax.portlet.title.*`)

## Container Lifecycle: Workspace-Native Docker Flow

The integration test harness uses the Liferay workspace plugin's Docker tasks (Bmuschko-based) instead of Testcontainers. The task graph is:

```
:dockerDeploy -> :createDockerfile -> :buildDockerImage
             -> :createDockerContainer -> :startDockerContainer
             -> :integration-test:awaitLiferayReady
             -> :integration-test:integrationTest
             -> :stopDockerContainer (autoRemove removes)
```

`LiferayContainer` is a POJO holding constants (`host=localhost`, `httpPort=8080`, `gogoPort=11311`, `container=test-factory-liferay`). It performs no container lifecycle work — all start/stop is owned by the workspace plugin. `deployJar(Path)` uses `docker cp` via `ProcessBuilder` against the running container.

Reference file:line for workspace plugin internals: `modules/sdk/gradle-plugins-workspace/src/main/java/com/liferay/gradle/plugins/workspace/configurator/RootProjectConfigurator.java` (Bmuschko task wrappers at lines 373–1479, constants at 114–187, orchestration at 341–370).

## Breaking API Changes Applied

### GroupLocalService.addGroup()

DXP 2026 adds `externalReferenceCode` as the first parameter:

```java
// CE 7.4 (15 params)
_groupLocalService.addGroup(userId, parentGroupId, ...)

// DXP 2026 (16 params — externalReferenceCode prepended)
_groupLocalService.addGroup(null, userId, parentGroupId, ...)
```

Affected file: `service/SiteCreator.java`

### All other APIs remain compatible

`UserLocalService.addUserWithWorkflow()`, `BlogsEntryLocalService.addEntry()`,
`JournalArticleLocalService.addArticle()`, `RoleLocalService.addRole()`,
`OrganizationLocalService.addOrganization()`, `MBMessageLocalService.addMessage()`,
`DLAppLocalService.addFileEntry()`, `AssetVocabularyLocalService.addVocabulary()`,
`AssetCategoryLocalService.addCategory()`, `MBCategoryLocalService.addCategory()`,
`CompanyLocalService.addCompany()` (13-arg signature unchanged) — all compatible.

## DXP License Handling

DXP 2026 requires a valid activation key. The workspace plugin's `dockerDeploy` task copies `configs/<env>/deploy/activation-key.xml` into the generated image at build time, where Liferay's deploy-on-startup machinery picks it up.

Local developers place their license at `configs/local/deploy/activation-key.xml` (gitignored). CI stores the license content similarly (either as a mounted secret or extracted from a base64 repo secret into the same path before running Gradle).

No `LIFERAY_DXP_LICENSE_*` env vars are used. `LiferayContainer` no longer mounts or copies the license — the workspace plugin owns this responsibility.

## portal-ext.properties Placement

The workspace plugin's generated Dockerfile sets `ENV LIFERAY_WORKSPACE_ENVIRONMENT=local` and its image entrypoint (`100_liferay_image_setup.sh`) copies only `configs/common/` and `configs/<env>/` (where `env=local` by default). **`configs/docker/` is never copied.**

Every DXP 2026 portal override that the tests depend on — `setup.wizard.enabled=false`, `passwords.default.policy.change.required=false`, `enterprise.product.notification.enabled=false`, the `configuration.override.*` BasicAuth wiring, the JSONWS-enabling trio (`jsonws.web.service.api.discoverable`, `json.web.service.enabled`, `json.servlet.hosts.allowed=`), etc. — lives in `configs/common/portal-ext.properties` so that any environment inherits the same working baseline. Environment-only overrides (JDBC, etc.) go under `configs/local/portal-ext.properties`. The empty `configs/common/portal-liferay-online-config.properties` shadows the DXP-baked `json.servlet.hosts.allowed=N/A` that otherwise blocks all JSONWS access (commit `6021828`; see `docs/details/dxp-2026-gotchas.md`).

## Bundle Resolution Trap: Portlet Taglib URI Stays on JCP Namespace

Despite the move to `jakarta.portlet 4.0`, the OSGi `Provide-Capability` advertised by `util-taglib.jar` in DXP 2026.q1.3-lts does NOT include `jakarta.tags.portlet`. It advertises only the legacy URIs:

- `http://java.sun.com/portlet`
- `http://java.sun.com/portlet_2_0`
- `http://xmlns.jcp.org/portlet_3_0`

Consequently `init.jsp` must use `<%@ taglib uri="http://xmlns.jcp.org/portlet_3_0" prefix="portlet" %>`. Using `jakarta.tags.portlet` causes the bundle's auto-generated `Require-Capability: osgi.extender;filter:="(&(osgi.extender=jsp.taglib)(uri=jakarta.tags.portlet))"` to never resolve, leaving the bundle in `Installed` state and breaking deploy.

This is a DXP 2026 release inconsistency (the TLD file inside `util-taglib.jar` declares `jakarta.tags.portlet` but the bnd `Provide-Capability` does not wire it). The @Component `jakarta.portlet.version=4.0` property remains correct.

## DXP-Specific Configuration in portal-ext.properties

| Property | Value | Reason |
|---|---|---|
| `setup.wizard.enabled` | `false` | Suppresses first-run setup wizard so `test@liferay.com` bootstrap is stable |
| `terms.of.use.required` | `false` | Skips ToU modal |
| `users.reminder.queries.enabled` | `false` | Skips reminder question step (note: key is `queries` not `query`) |
| `passwords.default.policy.change.required` | `false` | Creates default PasswordPolicy with `changeRequired=false` on first start |
| `enterprise.product.notification.enabled` | `false` | Suppresses DXP-only EPN modal |
| `company.security.strangers` | `false` | Prevents open registration |
| `company.security.strangers.verify` | `false` | Suppresses email verification |
| `company.security.update.password.required` | `false` | Prevents update-password wall at first login |
| `admin.email.user.added.enabled` | `false` | Suppresses notification emails during bulk user creation |
| `jsonws.web.service.api.discoverable` | `true` | Required for integration-test JSONWS calls |
| `json.web.service.enabled` | `true` | Required for JSONWS endpoints |

## DXP 2026 License Activation Race

DXP 2026 processes its activation key asynchronously ~14 s after Tomcat startup. Until the license is activated, every page (including `/c/portal/login`) redirects to `/c/portal/license_activation`. The workspace plugin's `awaitLiferayReady` task polls `/c/portal/login` and exits on the first HTTP 200 — but DXP happens to return 200 for `/c/portal/login` even during license-activation state (the login form is served under that URL while the license page is being composed).

`BaseLiferaySpec.bootstrapAdminCredentials()` runs `_waitForLicenseActivated()` first, polling `GET /sign-in` and waiting for the `Location` response header to stop matching `license_activation`. Cap is 120 s.

## DXP 2026 Baked-In Admin Password Reset Trap

The DXP 2026 Docker image ships with the HSQL database pre-seeded. The admin user row (`test@liferay.com`) has `USER_.PASSWORDRESET=1` baked in. The `passwords.default.policy.change.required=false` property only affects the default PasswordPolicy created at first boot — it does NOT retroactively clear the pre-seeded `PASSWORDRESET` flag on the admin user row.

Until that flag is cleared, every `/api/jsonws/*` and `/o/headless-*` Basic Auth request returns HTTP 403 `{}`. DXP's `UserLocalServiceImpl._isPasswordReset()` + request filter chain gate authenticated actions on the flag.

The integration test harness clears this flag once per container lifetime in `BaseLiferaySpec.bootstrapAdminCredentials()` by walking the official UI flow:

1. GET `/sign-in` to extract `p_auth`.
2. POST `/c/portal/login` with `test@liferay.com:test`. DXP returns a 302 to an auto-submit form containing `ticketId`/`ticketKey`.
3. GET `/c` to pick up those ticket values.
4. POST `/c/portal/update_password` with the ticket + `password1=password2=Test12345` (new password must differ from old per DXP policy).
5. Poll `GET /api/jsonws/user/get-current-user` with Basic Auth `test@liferay.com:Test12345` until HTTP 200 (cap at 30 s). Because the form-login cookies are cleared before the Basic Auth probe, DXP re-evaluates against the updated DB state immediately without cache delay.
6. Switch the shared `activePassword` to `NEW_PASSWORD=Test12345`. All subsequent JSONWS / Headless calls use it.

## Headless portal-instances API: `portalInstanceId`, not `webId`

`/o/headless-portal-instances/v1.0/portal-instances` exposes the company's `webId` under the field name `portalInstanceId`. Tests querying for a newly-created company must use the path-based getter `/o/headless-portal-instances/v1.0/portal-instances/{portalInstanceId}` and compare on `.portalInstanceId` (not `.webId`). The list endpoint (`/portal-instances`) can lag for newly-created companies because `PortalInstancePool` caches the iterator.

## Workspace Plugin Module Discovery

The workspace plugin 16.0.5 (required for DXP 2026) does not auto-discover `modules/liferay-dummy-factory` from the plugin-configured `modules.dir` alone. The two settings are both required: `liferay.workspace.modules.dir=modules` in `gradle.properties` AND an explicit `include 'modules:liferay-dummy-factory'` with `project(':modules:liferay-dummy-factory').projectDir = file('modules/liferay-dummy-factory')` in `settings.gradle`. Omitting either makes `:modules:liferay-dummy-factory:jar` fail with "project not found". Refs: commits `717ae24`, `2d006cd`.

## bnd.bnd `Import-Package` must exclude `javax.servlet`

The DXP 2026 OSGi framework exports `jakarta.servlet` but NOT `javax.servlet`. Transitive references from older libraries can still emit `javax.servlet` into the bundle's auto-generated `Import-Package`, leaving the bundle `Installed` with `Unresolved requirements`. Add `!javax.servlet,!javax.servlet.http,*` to the bundle's `bnd.bnd` `Import-Package` directive. Ref: commit `423924e`.

## Consequences

- CE 7.4 is no longer supported. The `-Pbuild.target=ce` Gradle flag is removed.
- All `javax.portlet.*` and `javax.servlet.*` imports in portlet-related source files become `jakarta.*`.
- JSP portlet taglib URI stays on `http://xmlns.jcp.org/portlet_3_0` per DXP 2026's `Provide-Capability` inventory.
- Integration tests require a valid DXP license at `configs/<env>/deploy/activation-key.xml`.
- `configs/docker/` is no longer read by the workspace plugin. Shared overrides live in `configs/common/`; environment-only overrides in `configs/<env>/`.
- JaCoCo is out of scope for the DXP-native flow (the workspace plugin cannot inject `-javaagent` via env vars). The `jacocoIntegrationReport` task remains as a no-op when no exec data exists.
- Integration specs may assume the admin password is `Test12345` after `ensureBundleActive()` returns (not `test`).
- Runtime / HTTP-client / auth-cache gotchas discovered during migration live in `docs/details/dxp-2026-gotchas.md`. Read on demand.
