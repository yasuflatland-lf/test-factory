# Liferay DXP 2026.q1.3-lts — API notes

L3 detail. Non-obvious API facts for Liferay DXP 2026.q1.3-lts that have cost real time. This file is the single source of truth for DXP 2026 API constraints. Read on demand from `.claude/rules/writing-code.md` or `.claude/rules/debugging.md`.

For the migration decisions themselves (Docker image, `release.dxp.api`, `jakarta.*` namespace, license, taglib URI, admin password bootstrap, etc.), see `docs/ADR/adr-0008-dxp-2026-migration.md`. Runtime / HTTP-client / auth-cache traps discovered during the migration live in `docs/details/dxp-2026-gotchas.md`.

## 1. `release.dxp.api` already bundles journal and DDM

`com.liferay.journal.api` and `com.liferay.dynamic.data.mapping.api` are included inside `release.dxp.api-2026.q1.3.jar`. Do NOT declare them as separate `compileOnly` dependencies — the workspace BOM does not enumerate them separately for the DXP 2026 product release and they will fail to resolve. Remove any standalone declarations from `build.gradle`.

## 2. `GroupLocalService.addGroup()` — 16-param signature

DXP 2026 prepends `externalReferenceCode` as the first parameter (16 params total):

```java
_groupLocalService.addGroup(
    null,                            // externalReferenceCode
    userId, parentGroupId, className, classPK, liveGroupId,
    nameMap, descriptionMap, type, typeSettings,
    manualMembership, membershipRestriction, friendlyURL,
    site, inheritContent, active, serviceContext);
```

Pass `StringPool.BLANK` for `typeSettings` if unused. Reference: `SiteCreator.java`.

## 3. `CompanyService` is blacklisted from JSON-WS

`portal.properties` lists `com.liferay.portal.kernel.service.CompanyServiceUtil` in `json.service.invalid.class.names`. Every path under `/api/jsonws/company/*` returns HTTP **403** (not 404) regardless of method or parameter format — `JSONServiceAction.isValidRequest()` returns `false` for blacklisted class names.

Workarounds:

- To obtain `companyId`: call `/api/jsonws/user/get-current-user` — `UserServiceUtil` is not blacklisted and its response includes `companyId`.
- To verify a newly-created company by `webId`: call the headless endpoint `/o/headless-portal-instances/v1.0/portal-instances/{portalInstanceId}` (the field is exposed as `portalInstanceId`, NOT `webId`).
- There is no working remote delete path. Rely on `withReuse(false)` and skip cleanup (see `.claude/rules/testing.md`).

## 4. `CompanyLocalService.addCompany` — 13-arg signature

```java
addCompany(Long companyId, String webId, String virtualHostname, String mx,
           int maxUsers, boolean active, boolean addDefaultAdminUser,
           String defaultAdminPassword, String defaultAdminScreenName,
           String defaultAdminEmailAddress, String defaultAdminFirstName,
           String defaultAdminMiddleName, String defaultAdminLastName)
```

No simpler 6-arg overload exists on `CompanyLocalService`. The 6-arg version lives on `CompanyService`, which is the blacklisted remote interface (see §3). For dummy company creation, pass `addDefaultAdminUser=false` and all remaining admin fields as `null`. Reference: `CompanyCreator.java`.

## 5. `OrganizationService.addOrganization` returns 404 via JSONWS — use headless-admin-user

JSONWS consistently returns HTTP 404 for form-encoded POSTs to `/api/jsonws/organization/add-organization` regardless of overload (the simple 10-arg form and the 15-arg form with contact-info lists both take a `ServiceContext`, which the JSONWS form encoder cannot serialize). Route through the headless REST API instead:

```
POST /o/headless-admin-user/v1.0/organizations
Content-Type: application/json
{"name":"Example"}
```

The response nests the created entity; normalize `id` → `organizationId` at the caller site. Reference: `JsonwsSetupHelper.createOrganization`.

## 6. `MBThreadLocalService.getThreads(groupId, categoryId, status, start, end)` — `categoryId` is an exact-match filter

The `categoryId` parameter is an **exact-match filter**, NOT a "no filter" sentinel. Passing `categoryId=0L` returns ONLY threads whose parent `categoryId` is exactly 0 (root-level threads), NOT all threads in the group.

To list ALL threads in a group regardless of category, iterate `MBCategoryLocalService.getCategories(groupId)` and union per-category results with the root-level call. There is no single overload that returns group-wide threads in one shot.

This was the root cause of an empty `#threadId` dropdown in the MB Reply form.

## 7. `MBCategoryLocalService.addCategory` — only the 6-arg overload exists

```java
addCategory(String externalReferenceCode, long userId, long parentCategoryId,
            String name, String description, ServiceContext serviceContext)
```

Pass `externalReferenceCode=null` and `parentCategoryId=0L` for top-level categories.

## 8. `DefaultScreenNameValidator` accepts only `[a-zA-Z0-9._-]`

`com.liferay.portal.kernel.security.auth.DefaultScreenNameValidator` rejects any screen name containing characters outside `[a-zA-Z0-9._-]`. It also rejects email-address form and reserved words such as `postfix`. The error surfaces as `UserScreenNameException.MustValidate` with a message listing the allowed characters.

Practical consequences:

- Names from Datafaker locales with apostrophes (`O'Conner`, `D'Angelo`), whitespace (`Mary Ann`), or non-ASCII (漢字, кириллица) will fail. Even `en_US` is unsafe — Datafaker still emits names like `O'Brien`.
- Screen names are NOT required to contain a letter — purely numeric is rejected by a different rule. A sanitizer that strips all letters (`"山田2"` → `"2"`) will therefore fail downstream with a different `UserScreenNameException` subclass.
- The caller must lowercase before validation; `DefaultScreenNameValidator` itself does not lowercase.

Use `com.liferay.support.tools.utils.ScreenNameSanitizer` for any external-generated name source (§9 below).

## 9. `com.liferay.support.tools.utils.ScreenNameSanitizer`

Pure static utility for converting arbitrary text into a Liferay-legal screen-name component:

```java
public static String sanitize(String input);
```

Behavior:

1. `null` input → returns `"user"` (logged at WARN).
2. Strips everything outside `[a-zA-Z0-9._-]`.
3. Collapses `..` runs into a single `.`.
4. Strips leading/trailing `._-`.
5. Returns `"user"` as a fallback if the result is empty (logged at WARN).

The caller is responsible for lowercasing and for appending any disambiguating index suffix. The method does NOT lowercase (to keep it composable).

Always use this for **external-generated** names. For **user-supplied** names (e.g. `baseName` from a portlet form), prefer validation and rejection at the resource-command boundary — silently rewriting user input is a UX regression. See the input-boundary policy in `.claude/rules/writing-code.md`.

## 10. `ResourceCommandUtil.setErrorResponse` writes `error`, not `errorMessage`

The helper `com.liferay.support.tools.portlet.actions.ResourceCommandUtil.setErrorResponse` writes the failure message to the JSON field named `error`. New resource commands should use this helper rather than hand-rolling a JSON error response so the frontend `parseResponse` in `js/utils/api.ts` sees a single consistent field name. Do NOT invent alternate field names like `errorMessage`, `message`, `reason`, or `detail` — the frontend does not read them.

## 11. Throw input-validation exceptions OUTSIDE `TransactionInvokerUtil.invoke(...)`

When a Creator validates caller input (e.g. a regex check on `baseName` before looping), throw the validation exception **before** entering any `TransactionInvokerUtil.invoke(...)` call. No transaction has started, so no rollback is needed and no partial commit is possible. The `throws Throwable` signature on the Creator combined with the resource command's `catch (Throwable)` routes the exception directly to `ResourceCommandUtil.setErrorResponse` → `{success: false, error: "..."}`. No additional plumbing is required.

Do NOT wrap the validation in `invoke(...)` just to match the per-entity calls. Doing so costs a meaningless null-rollback and hides the contract that input validation happens at the boundary, not per-entity.

## 12. `BlogsEntryLocalService.addEntry` auto-deduplicates `urlTitle`

**Why:** Unlike organizations (`DuplicateOrganizationException`) or users (`DuplicateScreenNameException`), blog entries have no title-uniqueness constraint. `BlogsEntryLocalServiceImpl._getUniqueUrlTitle()` via `FriendlyURLEntryLocalService.getUniqueUrlTitle()` automatically appends a numeric suffix on collision (`test`, `test-1`, `test-2`).

**What:** No `DuplicateEntryException` exists for blog titles. The only blog duplicate exception is `DuplicateBlogsEntryExternalReferenceCodeException` (for external reference codes). Creators do not need catch-and-continue duplicate handling for blogs — the `skipped` counter tracks generic per-entity `Exception` catches only.

## 13. JSONWS paths for module-level services use a dot-prefixed context

**Why:** Portal-core services (e.g. `user`, `role`) use simple paths: `/api/jsonws/user/get-user-by-email-address`. Services from OSGi module JARs (blogs, journal, DDM, etc.) require the module context prefix with a dot separator.

**What:** The JSONWS path pattern for module services is `/<module>.<entity>/method-name`:
- Blogs: `/api/jsonws/blogs.blogsentry/get-group-entries`
- Journal: `/api/jsonws/journal.journalarticle/get-articles`
- DDM: `/api/jsonws/ddm.ddmstructure/get-structures`

The module name matches the `Bundle-SymbolicName` prefix (e.g. `com.liferay.blogs.service` → `blogs`). Omitting the prefix returns HTTP 404.

Note: the JSONWS base path on DXP 2026 is `/portal/api/jsonws/` (the servlet is mounted under the portal context). See `dxp-2026-gotchas.md` for the full list of DXP 2026 JSONWS gotchas.

## 14. `PanelCategoryKeys.CONTROL_PANEL_MARKETPLACE` has no implementing `PanelCategory`

**Why:** The constant `PanelCategoryKeys.CONTROL_PANEL_MARKETPLACE` (`"control_panel.marketplace"`) is defined in the `application-list-api` JAR but no `PanelCategory` component implements it. Portlets registered under this key are orphaned and invisible.

**What:** The "MARKETPLACE" section visible in the Control Panel UI is rendered under `PanelCategoryKeys.CONTROL_PANEL_APPS` (`"control_panel.apps"`). Existing Marketplace items (Purchased order=100, Store order=200, License Manager order=300) all use `CONTROL_PANEL_APPS`. Use this key with `panel.app.order` lower than 100 to appear first. Reference: `DummyFactoryPanelApp.java`.

## 15. `bnd.bnd` must exclude `javax.servlet` from `Import-Package`

The DXP 2026 OSGi framework exports `jakarta.servlet` but NOT `javax.servlet`. Transitive references from older libraries can still emit `javax.servlet` into the bundle's auto-generated `Import-Package`, leaving the bundle `Installed` with `Unresolved requirements`. Add `!javax.servlet,!javax.servlet.http,*` to the bundle's `bnd.bnd` `Import-Package` directive. Ref: commit `423924e`.
