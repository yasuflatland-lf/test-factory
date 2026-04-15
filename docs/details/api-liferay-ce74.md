# Liferay CE 7.4 GA132 — API notes

L3 detail. Non-obvious API facts that have cost real time. This file is the single source of truth for CE 7.4 API constraints. Read on demand from `.claude/rules/writing-code.md` or `.claude/rules/debugging.md`.

> **DXP 2026.Q1 Compatibility Notes**  
> Each constraint below is annotated with its DXP 2026.Q1.3-LTS status where verified.  
> Verified against Liferay Portal master branch (post-Jakarta migration, April 2026).

## 1. `MBThreadLocalService.getThreads(groupId, categoryId, status, start, end)` — `categoryId` is an exact-match filter

The `categoryId` parameter is an **exact-match filter**, NOT a "no filter" sentinel. Passing `categoryId=0L` returns ONLY threads whose parent `categoryId` is exactly 0 (root-level threads under no category), NOT all threads in the group.

To list ALL threads in a group regardless of category, iterate `MBCategoryLocalService.getCategories(groupId)` and union per-category results with the root-level call. There is no single overload that returns group-wide threads in one shot on CE 7.4 GA132.

This was the root cause of an empty `#threadId` dropdown in the MB Reply form.

## 2. `MBCategoryLocalService.addCategory` — only the 6-arg overload exists

Available signature:

```java
addCategory(String externalReferenceCode, long userId, long parentCategoryId,
            String name, String description, ServiceContext serviceContext)
```

Pass `externalReferenceCode=null` and `parentCategoryId=0L` for top-level categories. The 5-arg overload (without `externalReferenceCode`) referenced in some older docs does not exist on CE 7.4 GA132.

## 3. `CompanyService` is blacklisted from JSON-WS

`portal.properties` lists `com.liferay.portal.kernel.service.CompanyServiceUtil` in `json.service.invalid.class.names` (DXP: unchanged — still blacklisted). As a result, every path under `/api/jsonws/company/*` returns HTTP 404 regardless of method or parameter format. `CompanyService.deleteCompany(long)` is defined but cannot be invoked via JSON-WS.

For tests, there is no working remote delete path in CE 7.4 GA132. The workaround is to rely on `withReuse(false)` and skip cleanup (see `.claude/rules/testing.md`).

## 4. `CompanyLocalService.addCompany` only exposes the 13-arg overload

Signature (DXP: confirmed compatible — same 13-arg signature with Long companyId first):

```java
addCompany(Long companyId, String webId, String virtualHostname, String mx,
           int maxUsers, boolean active, boolean addDefaultAdminUser,
           String defaultAdminPassword, String defaultAdminScreenName,
           String defaultAdminEmailAddress, String defaultAdminFirstName,
           String defaultAdminMiddleName, String defaultAdminLastName)
```

No simpler 6-arg overload exists on `CompanyLocalService` in CE 7.4 GA132. The 6-arg version lives on `CompanyService`, which is the blacklisted remote interface (see #3 above). For dummy company creation, pass `addDefaultAdminUser=false` and all remaining admin fields as `null`. Reference: `CompanyCreator.java`.

## 5. `DefaultScreenNameValidator` accepts only `[a-zA-Z0-9._-]`

CE 7.4 GA132's `com.liferay.portal.kernel.security.auth.DefaultScreenNameValidator` rejects any screen name containing characters outside `[a-zA-Z0-9._-]`. It also rejects email-address form and reserved words such as `postfix`. The error surfaces as `UserScreenNameException.MustValidate` with a message listing the allowed characters.

Practical consequences:

- Names from Datafaker locales with apostrophes (`O'Conner`, `D'Angelo`), whitespace (`Mary Ann`), or non-ASCII (漢字, кириллица) will fail. Even `en_US` is unsafe — Datafaker still emits names like `O'Brien`.
- Screen names are NOT required to contain a letter — purely numeric is rejected by a different rule. A sanitizer that strips all letters (`"山田2"` → `"2"`) will therefore fail downstream with a different `UserScreenNameException` subclass.
- The caller must lowercase before validation; `DefaultScreenNameValidator` itself does not lowercase.

Use `com.liferay.support.tools.utils.ScreenNameSanitizer` for any external-generated name source (#6 below).

## 6. `com.liferay.support.tools.utils.ScreenNameSanitizer`

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

## 7. `ResourceCommandUtil.setErrorResponse` writes `error`, not `errorMessage`

The helper `com.liferay.support.tools.portlet.actions.ResourceCommandUtil.setErrorResponse` writes the failure message to the JSON field named `error`. New resource commands should use this helper rather than hand-rolling a JSON error response so the frontend `parseResponse` in `js/utils/api.ts` sees a single consistent field name. Do NOT invent alternate field names like `errorMessage`, `message`, `reason`, or `detail` — the frontend does not read them.

## 8. Throw input-validation exceptions OUTSIDE `TransactionInvokerUtil.invoke(...)` (DXP: confirmed compatible — unchanged signature)

When a Creator validates caller input (e.g. a regex check on `baseName` before looping), throw the validation exception **before** entering any `TransactionInvokerUtil.invoke(...)` call. No transaction has started, so no rollback is needed and no partial commit is possible. The `throws Throwable` signature on the Creator combined with the resource command's `catch (Throwable)` routes the exception directly to `ResourceCommandUtil.setErrorResponse` → `{success: false, error: "..."}`. No additional plumbing is required.

Do NOT wrap the validation in `invoke(...)` just to match the per-entity calls. Doing so costs a meaningless null-rollback and hides the contract that input validation happens at the boundary, not per-entity.

## 9. PortletTracker tracks `javax.portlet.Portlet`, not `jakarta.portlet.Portlet`

The PortletTracker in CE 7.4 GA132 tracks `javax.portlet.Portlet` services, **not** `jakarta.portlet.Portlet`. If the portlet `@Component` declares `service = jakarta.portlet.Portlet.class`, the PortletTracker will not detect it and the corresponding `com.liferay.portal.kernel.model.Portlet` OSGi service will never be registered. Any PanelApp with a `@Reference` to that portlet model will stay in an **UNSATISFIED** state.

**Debugging:**

1. `scr:info <component-name>` in GoGo Shell — look for `UNSATISFIED REFERENCE`.
2. `headers <bundle-id>` in GoGo Shell — inspect `Import-Package` to verify `javax.portlet` vs `jakarta.portlet`.
3. If the bundle imports `jakarta.portlet`, switch the portlet `@Component` to `service = javax.portlet.Portlet.class` and use `javax.portlet` imports.

The corresponding workspace-level decision is recorded in `docs/ADR/adr-0002-portlet-api-javax-namespace.md`.

## 10. `BlogsEntryLocalService.addEntry` auto-deduplicates `urlTitle`

**Why:** Unlike organizations (`DuplicateOrganizationException`) or users (`DuplicateScreenNameException`), blog entries have no title-uniqueness constraint. `BlogsEntryLocalServiceImpl._getUniqueUrlTitle()` via `FriendlyURLEntryLocalService.getUniqueUrlTitle()` automatically appends a numeric suffix on collision (`test`, `test-1`, `test-2`).

**What:** No `DuplicateEntryException` exists for blog titles. The only blog duplicate exception is `DuplicateBlogsEntryExternalReferenceCodeException` (for external reference codes). Creators do not need catch-and-continue duplicate handling for blogs — the `skipped` counter tracks generic per-entity `Exception` catches only.

## 11. JSONWS paths for module-level services use a dot-prefixed context

**Why:** Portal-core services (e.g. `user`, `company`, `role`) use simple paths: `/api/jsonws/user/get-user-by-email-address`. Services from OSGi module JARs (blogs, journal, DDM, etc.) require the module context prefix with a dot separator.

**What:** The JSONWS path pattern for module services is `/<module>.<entity>/method-name`:
- Blogs: `/api/jsonws/blogs.blogsentry/get-group-entries`
- Journal: `/api/jsonws/journal.journalarticle/get-articles`
- DDM: `/api/jsonws/ddm.ddmstructure/get-structures`

The module name matches the `Bundle-SymbolicName` prefix (e.g. `com.liferay.blogs.service` → `blogs`). Omitting the prefix returns HTTP 404.

## 12. `LIFERAY_JVM_OPTS` is the correct env var for JVM option injection in CE 7.4 Docker images

**Why:** The Liferay CE 7.4.3.x Docker image's `setenv.sh` appends `$LIFERAY_JVM_OPTS` to the JVM startup command. `CATALINA_OPTS` would replace Liferay's built-in JVM options rather than supplement them. `JAVA_OPTS_APPEND` is not recognized by this image.

**What:** To inject JVM agents or flags into the container JVM (e.g. JaCoCo tcpserver mode), set `LIFERAY_JVM_OPTS` in `withEnv(...)` — not `CATALINA_OPTS` or `JAVA_OPTS_APPEND`. Always call `.toString()` on any GString value passed to `withEnv(Map)` — see the JaCoCo pitfalls section in `docs/details/testing-gradle.md`.

## 13. `PanelCategoryKeys.CONTROL_PANEL_MARKETPLACE` is not registered in CE 7.4 GA132

**Why:** The constant `PanelCategoryKeys.CONTROL_PANEL_MARKETPLACE` (`"control_panel.marketplace"`) is defined in the `application-list-api` JAR but no `PanelCategory` component implements it on CE 7.4 GA132. Portlets registered under this key are orphaned and invisible.

**What:** The "MARKETPLACE" section visible in the Control Panel UI is rendered under `PanelCategoryKeys.CONTROL_PANEL_APPS` (`"control_panel.apps"`). Existing Marketplace items (Purchased order=100, Store order=200, License Manager order=300) all use `CONTROL_PANEL_APPS`. Use this key with `panel.app.order` lower than 100 to appear first.

## 14. `UserLocalService.addUserWithWorkflow` — `prefixListTypeId` and `suffixListTypeId` positions (DXP: confirmed compatible — prefixListTypeId/suffixListTypeId at same positions, 0L is valid)

On CE 7.4 GA132, the `addUserWithWorkflow` method accepts `prefixListTypeId` and `suffixListTypeId` as `long` parameters. Passing `0L` for both is valid and produces a user with no name prefix or suffix. The parameter positions have not changed in DXP 2026.Q1.3-LTS — callers built against CE 7.4 GA132 are binary-compatible with DXP when compiled against `release.dxp.api`.

## 15. `GroupLocalService.addGroup` — DXP 2026.Q1.3 signature is 16 args (CE 7.4 is 15)

**Why:** DXP 2026.Q1.3 added `String externalReferenceCode` as the first argument and inserted `String typeSettings` between the `type` and `manualMembership` parameters. CE 7.4 GA132 callers that omit these two arguments will fail to compile against `release.dxp.api`.

**What:** DXP signature (16 args):

```java
addGroup(String externalReferenceCode, long userId, long parentGroupId,
         String className, long classPK, int type, String typeSettings,
         boolean manualMembership, int membershipRestriction,
         String friendlyURL, boolean site, boolean inheritContent,
         boolean active, ServiceContext serviceContext)
```

Pass `StringPool.BLANK` for both `externalReferenceCode` and `typeSettings` when no external reference code or custom type settings are needed. The DXP source set (`java-dxp/`) must use the 16-arg overload; the CE source set (`java-ce/`) continues to use the 15-arg form.

## 16. `javax.servlet` and `javax.ws.rs` also migrated to Jakarta namespace in DXP 2026.Q1

**Why:** The Jakarta EE namespace migration in DXP 2026.Q1 covers the full servlet and JAX-RS stacks, not only `javax.portlet`. Classes implementing `DataListProvider` (which receives `HttpServletRequest`), `WorkflowApplication`, and `WorkflowResource` all depend on `javax.servlet.*` or `javax.ws.rs.*` — both of which become `jakarta.servlet.*` and `jakarta.ws.rs.*` in DXP.

**What:** Any class in `src/main/java/` that imports `javax.servlet.*` or `javax.ws.rs.*` must be moved to both `java-ce/` and `java-dxp/` source sets if it also requires a portlet-API import. If the class is portlet-agnostic but still uses `javax.servlet`, the namespace split still applies: `javax.servlet` for CE, `jakarta.servlet` for DXP. The dual source-set strategy (see `docs/ADR/adr-0007-ce-dxp-dual-profile.md`) covers all three namespace axes — portlet, servlet, and ws.rs.

## 17. `LocaleUtil.fromLanguageId(String, boolean)` — pass `validate=false` to skip Language service

**Why:** The single-arg overload `LocaleUtil.fromLanguageId(String)` delegates to a Language service lookup that requires the portal Language service to be registered. In host-JVM unit tests (e.g. Vitest/JUnit without a running portal), this lookup fails with a service-unavailability error.

**What:** The 2-arg overload `LocaleUtil.fromLanguageId(id, false)` performs pure string parsing (splitting on `_`) without touching the Language service. Pass `validate=false` in any code path that must work in a unit-test environment or early in the OSGi lifecycle before the Language service is active.
