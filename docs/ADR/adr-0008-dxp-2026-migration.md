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

## Breaking API Changes Applied

### GroupLocalService.addGroup()

DXP 2026 adds `externalReferenceCode` as the first parameter:

```java
// CE 7.4 (16 params)
_groupLocalService.addGroup(userId, parentGroupId, ...)

// DXP 2026 (17 params)
_groupLocalService.addGroup(null, userId, parentGroupId, ...)
```

Affected file: `service/SiteCreator.java`

### All other APIs remain compatible

`UserLocalService.addUserWithWorkflow()`, `BlogsEntryLocalService.addEntry()`,
`JournalArticleLocalService.addArticle()`, `RoleLocalService.addRole()`,
`OrganizationLocalService.addOrganization()`, `MBMessageLocalService.addMessage()`,
`DLAppLocalService.addFileEntry()`, `AssetVocabularyLocalService.addVocabulary()`,
`AssetCategoryLocalService.addCategory()`, `MBCategoryLocalService.addCategory()`
— all are already compatible with DXP 2026 (no changes needed).

## DXP License Handling

DXP 2026 requires a valid activation key. The integration test container reads the license from one of two environment variables (checked in order):

1. `LIFERAY_DXP_LICENSE_FILE` — path to a local `activation-key-*.xml` file
2. `LIFERAY_DXP_LICENSE_BASE64` — base64-encoded content of the license XML

If neither is set, `LiferayContainer.getInstance()` throws `IllegalStateException` before Docker start.

Local license files must be placed in `licenses/` (gitignored). For CI/CD, store the base64-encoded license in a GitHub Actions repository secret named `LIFERAY_DXP_LICENSE_BASE64`.

## DXP-Specific Configuration Added

These env vars and portal-ext.properties entries are required for DXP 2026 but not CE 7.4:

| Setting | Value | Reason |
|---|---|---|
| `LIFERAY_DISABLE_TRIAL_LICENSE` | `true` | Prevents DXP image from loading trial license on startup |
| `enterprise.product.notification.enabled` | `false` | Suppresses DXP-only EPN modal that blocks first login |
| `company.security.strangers` | `false` | Prevents open registration prompts |
| `company.security.strangers.verify` | `false` | Suppresses email verification requirement |
| `company.security.update.password.required` | `false` | Prevents update-password wall at first login |
| `admin.email.user.added.enabled` | `false` | Suppresses email notifications during bulk user creation |

## Consequences

- CE 7.4 is no longer supported. The `-Pbuild.target=ce` Gradle flag is removed.
- All `javax.portlet.*` and `javax.servlet.*` imports in portlet-related source files become `jakarta.*`.
- Integration tests require a valid DXP license via environment variable before each run.
- The `licenses/` directory at repo root is gitignored and must not be committed.
