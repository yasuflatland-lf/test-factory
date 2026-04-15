# ADR-0007: CE/DXP Dual-Profile Build Architecture

**Date:** 2026-04-15  
**Status:** Accepted  
**Deciders:** Yasuyuki Takeo

## Context

The project targets both Liferay CE 7.4 GA132 and Liferay DXP 2026.Q1.3-LTS. These two
releases differ in a compile-time-breaking way: CE 7.4 uses `javax.portlet` (Portlet API 3.0)
while DXP 2026.Q1 uses `jakarta.portlet` (Portlet API 4.0). Java `@Component` property
strings like `"javax.portlet.name=..."` are annotation literals and cannot be conditionally
compiled within a single source file.

The previous approach (ADR-0007 v1) maintained two mirrored source directories (`java-ce/`
and `java-dxp/`) for ~22 portlet-adapter files — 62 files total in split source sets. Any
behavior change required edits in two places, and adding a new `*ResourceCommand` meant
creating it twice.

The goal was to eliminate the duplication while preserving correct CE and DXP output.

## Decision

`src/main/java/` is the single canonical source. It uses `jakarta.portlet` / `jakarta.servlet`
/ `jakarta.ws.rs` namespaces throughout. DXP is the default build target.

**CE build** (`-Pbuild.target=ce`) applies a Gradle `generateCeSources` task that transforms
`src/main/java/` into `build/generated/ce/` with these string replacements:

| From | To |
|---|---|
| `import jakarta.portlet.` | `import javax.portlet.` |
| `import jakarta.servlet.` | `import javax.servlet.` |
| `import jakarta.ws.rs.` | `import javax.ws.rs.` |
| `"jakarta.portlet.` | `"javax.portlet.` |
| `jakarta.portlet.version=4.0` | `javax.portlet.version=3.0` |

`processResources` also rewrites `Language.properties` for CE:
`jakarta.portlet.title.` → `javax.portlet.title.`

**`SiteCreator.java` is excluded from the transform.** `src/main/java-ce-overrides/` contains
a hand-authored CE version of `SiteCreator.java` only. The `GroupLocalService.addGroup()` API
signature differs between CE 7.4 and DXP 2026 (DXP adds extra leading parameters), so a
text-substitution transform cannot bridge the gap — the CE version must be maintained manually.

The same `-Pbuild.target=ce` property continues to drive:

- `release.portal.api` (CE) vs `release.dxp.api` (DXP) in the BOM dependency
- `liferay.docker.image` (CE) vs `dxp.docker.image` (DXP) in integration tests

## Alternatives Rejected

**Feature flags (runtime detection):** Java annotation literals cannot be conditional at
runtime. The `@Component` property string determines OSGi service registration; it must be
correct at compile time.

**Separate branches:** Increases maintenance overhead. Every business logic change would need
cherry-picking across branches. A single-source transform keeps everything on one branch.

**Source-level transform tool (source-formatter):** Liferay's Jakarta transform tool rewrites
source during the release process. Running it as part of the local build adds tooling complexity
and makes the transform implicit rather than explicit.

**Manual dual source sets (previous approach):** `java-ce/` and `java-dxp/` mirrored the
same ~22 portlet-adapter files. Every behavior change required edits in both directories.
Adding a new `*ResourceCommand` required creating it twice. The transform approach eliminates
62 files and reduces that maintenance burden to a single file per class.

## Consequences

- `src/main/java/` is the only place to edit portlet-adapter and business-logic files.
  The `java-ce/` and `java-dxp/` directories no longer exist.
- DXP is the default build. No flag is needed for DXP. CE requires `-Pbuild.target=ce`.
- `SiteCreator.java` in `src/main/java-ce-overrides/` is the one file that requires
  manual CE maintenance. Any change to the DXP `SiteCreator.java` must be reviewed
  for applicability to the CE override.
- The `generateCeSources` Gradle task is the single place that owns the namespace mapping.
  All portlet-adapter namespace differences are concentrated there.
- Adding a new `*ResourceCommand` requires creating it once in `src/main/java/` only.
