# DXP 2026.Q1 Migration Operational Notes

L3 detail. Practical findings from the CE 7.4 → DXP 2026.Q1.3-LTS dual-profile migration.
Read on demand from `docs/ADR/adr-0007-ce-dxp-dual-profile.md`.

## 1. `ProgressTracker` / `ProgressTrackerThreadLocal` still exist in DXP 2026.Q1.3

**Why this matters:** The original dual-profile plan assumed `ProgressTracker` and
`ProgressTrackerThreadLocal` might have been removed in the Jakarta migration, which would
have required a BackgroundTask-based replacement.

**What was found:** Both classes are present and unchanged in DXP 2026.Q1.3-LTS.
No migration of batch-progress tracking code is needed. CE code using these classes
is binary-compatible with DXP when compiled against `release.dxp.api`.

## 2. Activation key must be deployed via `withCopyToContainer`, not `deployJar`

**Why:** Liferay DXP validates the activation license at JVM boot. The standard
`deployJar()` helper uses `execInContainer()` (post-`start()` exec), so the license file
arrives after validation has already failed. `withCopyToContainer()` runs before `start()`,
placing the file in `/opt/liferay/deploy/` before the container JVM launches.

**What:** Use `container.withCopyToContainer(MountableFile.forHostPath(keyPath), "/opt/liferay/deploy/activation-key.xml")` in the container builder chain, before `start()` is called. `deployJar()` is correct for OSGi bundle deployment (post-boot hot-deploy), but wrong for license keys.

## 3. DXP containers take approximately 12 minutes to start

**Why:** The DXP 2026.Q1.3 image performs additional startup steps (license validation,
feature flag initialization, more OSGi bundle activation) compared to CE 7.4 GA132.

**What:** The wait strategy timeout must be extended from 8 minutes (CE) to at least
12 minutes for DXP. Use the same log-based wait strategy (Catalina startup message) but
with the longer ceiling. If the container appears to time out under DXP, extend the timeout
before suspecting other causes.

## 4. `releases.json` provides CDN artifact URLs, not Docker Hub tags

**Why:** `releases.json` at `https://releases.liferay.com/releases.json` is the release
catalogue for Liferay's artifact CDN (`releases-cdn.liferay.com`). The `url` field in each
entry is the CDN download link, not a Docker Hub image reference.

**What:** The Docker Hub tag must be determined separately. For DXP 2026.Q1.3, the verified
tag is `liferay/dxp:2026.q1.3-lts`. Always verify Docker Hub directly if pulls fail — CDN URLs
and Docker Hub tags are independent identifiers for the same release. See the DXP vs CE
table in `docs/details/dependency-policy.md` for the workspace product key mapping.
