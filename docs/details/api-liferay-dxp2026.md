# Liferay DXP 2026.Q1.3-LTS — API notes

L3 detail. Non-obvious API facts for DXP 2026.Q1.3-LTS. This file is the single source of truth for DXP 2026 API constraints. It replaces `api-liferay-ce74.md` (deleted). Read on demand from `.claude/rules/writing-code.md` or `.claude/rules/debugging.md`.

## 1. `release.dxp.api` BOM replaces individual API dependencies

DXP 2026 ships a managed BOM artifact. Use it as the single dependency for all Liferay APIs in `modules/liferay-dummy-factory/build.gradle`:

```groovy
compileOnly group: "com.liferay.portal", name: "release.dxp.api"
```

The BOM includes journals (`com.liferay.journal.api`), DDM (`com.liferay.dynamic.data.mapping.api`), message boards (`com.liferay.message.boards.api`), blogs, vocabulary/category, and all portal-kernel artifacts at the correct version. Adding individual API dependencies alongside `release.dxp.api` causes version skew and runtime `ClassCastException` or `NoClassDefFoundError`. Do not add per-API entries.

## 2. `GroupLocalService.addGroup` — new 18-argument signature

DXP 2026 adds `externalReferenceCode` as the first argument and `typeSettings` before `serviceContext`:

```java
// Before (CE 7.4 GA132) — 16 args
_groupLocalService.addGroup(
    userId, parentGroupId, className, classPK,
    liveGroupId, nameMap, descriptionMap, type,
    manualMembership, membershipRestriction, friendlyURL,
    site, inheritContent, active,
    serviceContext);

// After (DXP 2026) — 18 args
_groupLocalService.addGroup(
    externalReferenceCode,           // NEW: pass null for auto-generated ERC
    userId, parentGroupId, className, classPK,
    liveGroupId, nameMap, descriptionMap, type,
    manualMembership, membershipRestriction, friendlyURL,
    site, inheritContent, active,
    typeSettings,                    // NEW: pass null for defaults
    serviceContext);
```

Verify the exact signature against the source:
`/home/yasuflatland/tmp/liferay-portal/portal-service/src/com/liferay/portal/kernel/service/GroupLocalService.java`

Affected file: `modules/liferay-dummy-factory/src/main/java/com/liferay/support/tools/service/SiteCreator.java`.

## 3. `CompanyService` is blacklisted from JSON-WS

`portal.properties` lists `com.liferay.portal.kernel.service.CompanyServiceUtil` in `json.service.invalid.class.names`. Every path under `/portal/api/jsonws/company/*` (and the legacy `/api/jsonws/company/*`) returns HTTP 404 regardless of method or parameters.

For tests that need the current company ID, use `user/get-current-user` and extract the `.companyId` field:

```groovy
// BaseLiferaySpec.getCompanyId()
def json = jsonwsGet('user/get-current-user')
return json.companyId as long
```

## 4. `CompanyLocalService.addCompany` — 13-argument overload

```java
addCompany(Long companyId, String webId, String virtualHostname, String mx,
           int maxUsers, boolean active, boolean addDefaultAdminUser,
           String defaultAdminPassword, String defaultAdminScreenName,
           String defaultAdminEmailAddress, String defaultAdminFirstName,
           String defaultAdminMiddleName, String defaultAdminLastName)
```

No simpler overload exists on `CompanyLocalService`. The shorter overload lives on `CompanyService`, which is blacklisted (see #3). For dummy company creation, pass `addDefaultAdminUser=false` and all admin fields as `null`. Reference: `CompanyCreator.java`.

## 5. `OrganizationService.addOrganization` returns 404 via JSONWS — use Headless

`OrganizationService.addOrganization` is reachable via JSONWS in CE 7.4, but DXP 2026 routes it through Headless Admin User instead:

```
POST /o/headless-admin-user/v1.0/organizations
Authorization: Basic <base64>
Content-Type: application/json
{"name": "...", "organizationType": "Organization"}
```

Tests that previously called `/portal/api/jsonws/organization/add-organization` must migrate to the Headless endpoint. For verification (read), use `GET /o/headless-admin-user/v1.0/organizations?search=<name>` with `?pageSize=100` and client-side filtering (Elasticsearch ingestion lag makes `?search=` non-deterministic immediately post-create).

## 6. `MBThreadLocalService.getThreads` — `categoryId` is an exact-match filter

The `categoryId` parameter is an exact-match filter, not a "no filter" sentinel. Passing `categoryId=0L` returns only threads whose parent category ID is exactly 0 (root-level threads), not all threads in the group.

To list all threads regardless of category, iterate `MBCategoryLocalService.getCategories(groupId)` and union per-category results with the root-level call. No single overload returns group-wide threads in one shot.

## 7. `MBCategoryLocalService.addCategory` — only the 6-argument overload exists

```java
addCategory(String externalReferenceCode, long userId, long parentCategoryId,
            String name, String description, ServiceContext serviceContext)
```

Pass `externalReferenceCode=null` and `parentCategoryId=0L` for top-level categories. The 5-argument overload (without `externalReferenceCode`) does not exist.

## 8. `DefaultScreenNameValidator` accepts only `[a-zA-Z0-9._-]`

`com.liferay.portal.kernel.security.auth.DefaultScreenNameValidator` rejects any screen name outside this character class. It also rejects email-address form and reserved words such as `postfix`. The error surfaces as `UserScreenNameException.MustValidate`.

Practical consequences:

- Names from Datafaker locales with apostrophes (`O'Conner`), whitespace (`Mary Ann`), or non-ASCII (漢字) will fail.
- Purely numeric screen names are rejected by a separate rule; a sanitizer that strips all letters must then append a prefix.
- The validator does not lowercase; the caller must lowercase before passing.

Use `com.liferay.support.tools.utils.ScreenNameSanitizer` for external-generated name sources (#9 below).

## 9. `com.liferay.support.tools.utils.ScreenNameSanitizer`

Pure static utility for converting arbitrary text into a Liferay-legal screen name:

```java
public static String sanitize(String input);
```

Behavior:
1. `null` input → returns `"user"` (logged at WARN).
2. Strips everything outside `[a-zA-Z0-9._-]`.
3. Collapses consecutive `.` into a single `.`.
4. Strips leading/trailing `._-`.
5. Returns `"user"` as a fallback if the result is empty (logged at WARN).

The caller is responsible for lowercasing and for appending any disambiguating index suffix. Use this for **external-generated** names (Datafaker, RNGs, third-party APIs). For **user-supplied** names (e.g. `baseName` from a portlet form), validate and reject at the resource-command boundary — silently rewriting user input is a UX regression.

## 10. `ResourceCommandUtil.setErrorResponse` writes `error`, not `errorMessage`

`com.liferay.support.tools.portlet.actions.ResourceCommandUtil.setErrorResponse` writes the failure message to the JSON field named `error`. Do not invent alternate field names (`errorMessage`, `message`, `reason`, `detail`). The frontend `parseResponse` in `js/utils/api.ts` only reads `error`.

## 11. JSONWS paths for module-level services use a dot-prefixed context

Portal-core services use simple paths: `/portal/api/jsonws/user/get-user-by-email-address`. Module services (OSGi JARs) require a dot-prefixed module context:

| Service | JSONWS path |
|---------|-------------|
| Blogs | `/portal/api/jsonws/blogs.blogsentry/get-group-entries` |
| Journal | `/portal/api/jsonws/journal.journalarticle/get-articles` |
| DDM | `/portal/api/jsonws/ddm.ddmstructure/get-structures` |

The module name matches the `Bundle-SymbolicName` prefix (e.g. `com.liferay.blogs.service` → `blogs`). Omitting the prefix returns HTTP 404.

Note: In DXP 2026 the JSONWS base path changes from `/api/jsonws/` to `/portal/api/jsonws/`. All paths shown above use the new base. `BaseLiferaySpec.jsonwsGet/Post` centralizes this; individual specs pass only the path suffix.

## 12. `PanelCategoryKeys.CONTROL_PANEL_APPS` — `MARKETPLACE` constant does not exist in CE 7.4

The constant `PanelCategoryKeys.CONTROL_PANEL_MARKETPLACE` (`"control_panel.marketplace"`) is defined in the API JAR but no `PanelCategory` component implements it on CE 7.4 GA132. Portlets registered under it are orphaned and invisible.

In DXP 2026 the Marketplace category exists, but for this project the portlet is registered under `PanelCategoryKeys.CONTROL_PANEL_APPS` (`"control_panel.apps"`). Use `panel.app.order` lower than 100 to appear first in the Apps section. Do not reference `CONTROL_PANEL_MARKETPLACE`.

## 13. `bnd.bnd` must exclude `javax.servlet` and `javax.servlet.http`

DXP 2026 does not export `javax.servlet` or `javax.servlet.http` from the OSGi runtime. Any bundle that tries to import these packages will fail with UNSATISFIED state at activation time.

Add this to `modules/liferay-dummy-factory/bnd.bnd`:

```
Import-Package: !javax.servlet,!javax.servlet.http,*
```

Without this exclusion the bundle will show as UNSATISFIED in GoGo Shell even though the code compiles successfully.

## 14. JSONWS base path is `/portal/api/jsonws/`

DXP 2026 changed the JSONWS base path from `/api/jsonws/` to `/portal/api/jsonws/`. The old path returns 404 for all services.

`BaseLiferaySpec.jsonwsGet/Post` centralizes the base path. Individual specs and cleanup code must never hard-code the full path — pass only the suffix (e.g. `'user/get-current-user'`). When sweeping for old-path references, grep both dotted access and string literals:

```bash
grep -rn '"/api/jsonws/' integration-test/src/
grep -rn "'/api/jsonws/" integration-test/src/
```
