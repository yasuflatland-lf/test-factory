# ADR-0007: CE/DXP Dual-Profile Build Architecture

**Date:** 2026-04-15  
**Status:** Accepted  
**Deciders:** Yasuyuki Takeo

## Context

The project targets both Liferay CE 7.4 GA132 and Liferay DXP 2026.Q1.3-LTS. These two
releases differ in a fundamental, compile-time-breaking way: CE 7.4 uses `javax.portlet`
(Portlet API 3.0) while DXP 2026.Q1 uses `jakarta.portlet` (Portlet API 4.0). Java
`@Component` property strings like `"javax.portlet.name=..."` are annotation literals and
cannot be conditionally compiled within a single source file.

## Decision

Maintain two source directories for portlet-adapter files:

- `src/main/java-ce/` — portlet-facing classes using `javax.portlet` (CE 7.4)
- `src/main/java-dxp/` — portlet-facing classes using `jakarta.portlet` (DXP 2026.Q1)
- `src/main/java/` — shared business logic (Creators, DataListProviders, workflow, utils)

A Gradle property `-Pbuild.target=dxp` selects the DXP source set; the CE source set is
the default. The same property drives:

- `release.portal.api` (CE) vs `release.dxp.api` (DXP) in the BOM dependency
- `liferay.docker.image` (CE) vs `dxp.docker.image` (DXP) in integration tests
- `javax.portlet.title.*` (CE) vs `jakarta.portlet.title.*` (DXP) in Language.properties

## Alternatives Rejected

**Feature flags (runtime detection):** Java annotation literals cannot be conditional at runtime.
The `@Component` property string determines OSGi service registration; it must be correct at
compile time.

**Separate branches:** Increases maintenance overhead. Every business logic change would need
cherry-picking across branches. Source sets allow a single git branch for both targets.

**Source-level transform tool (source-formatter):** Liferay's Jakarta transform tool rewrites
source during the release process. Running it as part of the local build adds tooling complexity
and makes the transform implicit rather than explicit.

## Consequences

- Portlet-adapter files (~22 files) exist in both `java-ce/` and `java-dxp/` directories.
  Changes to portlet adapter behavior (not just namespace) must be applied to both.
- Business logic in `src/main/java/` is namespace-neutral and has no duplication.
- The Gradle `sourceSets` block is the single place that wires the correct source set.
- Adding a new `*ResourceCommand` requires creating it in both `java-ce/` and `java-dxp/`.
