# ADR-0008: Migrate to Liferay DXP 2026.Q1.3-LTS

## Status: Accepted (2026-04-18)

## Context

Liferay Portal CE 7.4 GA132 reached end-of-life. The project must migrate to Liferay DXP 2026.Q1.3-LTS (`liferay/dxp:2026.q1.3-lts`). A first migration attempt was made on branch `feature/migrate_to_dxp` (PR #42), which was closed without merging. Part 2 starts cleanly from `master`.

Key changes imposed by the new platform:

- **Portlet API**: DXP 2026 requires `jakarta.portlet` 4.0 (`jakarta.portlet.*` imports, `jakarta.portlet.version=4.0` component properties). The `javax.portlet` namespace is no longer exported.
- **Servlet API**: DXP 2026 OSGi runtime does not export `javax.servlet` or `javax.servlet.http`. Bundles that try to import them at startup will be UNSATISFIED.
- **JSP taglib URI**: DXP 2026's `Provide-Capability` only advertises `http://xmlns.jcp.org/portlet_3_0`. Switching the JSP `<%@ taglib %>` URI to `jakarta.tags.portlet` causes bundle resolution failure. The taglib URI stays on JCP.
- **JSONWS base path**: DXP 2026 exposes JSON Web Services at `/portal/api/jsonws/` instead of `/api/jsonws/`. Tests and helpers that hard-code the old path will return 404.
- **Docker lifecycle**: The previous Testcontainers approach is replaced by the workspace plugin's native Docker tasks. This eliminates the Testcontainers dependency entirely.
- **License**: DXP requires a valid activation key. Local and CI environments inject it via environment variables.
- **BOM**: Individual API dependencies are replaced by the `release.dxp.api` BOM, which includes journal, DDM, message-boards, and other APIs at the correct version.

## Decision

All eight decisions below were finalized via user Q&A recorded in `.claude/plan/dxp_migration.md`, section 1.

### D1 — Base branch: master, one-shot

Start part 2 from `master`. PR #42 is closed without merging. A single clean PR will carry all DXP 2026 changes. Rationale: avoids carrying forward uncommitted state or partial decisions from the aborted first attempt.

### D2 — Admin password reset suppressed via portal-ext.properties

`company.security.update.password.required=false` and `passwords.default.policy.change.required=false` are set in `configs/common/portal-ext.properties`. This prevents the `PASSWORDRESET` flag from being set at container startup, eliminating the 7-step HTTP ticket flow that was required in the Testcontainers-based harness. The password-reset ticket flow is removed from `BaseLiferaySpec`, simplifying the login path (file total line count may remain similar because new JSONWS/headless helpers were added).

### D3 — License injection via environment variable

A Gradle task (`resolveLicenseFile`) reads either `LIFERAY_DXP_LICENSE_FILE` (a file path, for local development) or `LIFERAY_DXP_LICENSE_BASE64` (a base64-encoded XML blob, for CI). It writes the result to `configs/local/deploy/activation-key.xml` before `dockerDeploy` copies configs into the Docker image. If both variables are absent the task fails fast with a clear error message. The activation key file is `.gitignore`d to prevent accidental commits.

### D4 — Container lifecycle: autoRemove=false, stop-only on test completion

The Docker container is not removed after the test run (`autoRemove=false` is the observed behavior with workspace plugin 16.0.5 when no explicit override is applied; see `RootProjectConfigurator.java` at `/home/yasuflatland/tmp/liferay-portal/modules/sdk/gradle-plugins-workspace/src/main/java/com/liferay/gradle/plugins/workspace/configurator/RootProjectConfigurator.java`). `integrationTest` uses `finalizedBy ':stopDockerContainer'` so the container is stopped but its volume is preserved. This allows post-mortem inspection (logs, `docker exec`, JaCoCo exec file recovery) after test failures. To force volume recreation, run `./gradlew removeDockerContainer` explicitly.

### D5 — Fixed Docker ports: 8080 / 11311 / 8000

The workspace plugin binds fixed ports: `8080:8080` (HTTP), `11311:11311` (GoGo Shell), `8000:8000` (JPDA). There are no dynamically mapped ports. Developers must ensure no other Liferay process is listening on these ports before running `./gradlew startDockerContainer`. The simplicity of fixed ports outweighs the minor inconvenience of stopping a local Liferay when needed.

### D6 — JaCoCo coverage preserved via LIFERAY_JVM_OPTS

The JaCoCo agent is injected into the container JVM through the `LIFERAY_JVM_OPTS` environment variable, set in the `createDockerContainer` task via `withEnvVar(...)`. The agent runs in `tcpserver` mode on port 6300. `BaseLiferaySpec.cleanupSpec()` dumps per-spec `.exec` files; `jacocoIntegrationReport` merges them. Coverage collection is unaffected by the Testcontainers removal.

### D7 — Bundle deployment via dockerDeploy (image-baked)

The module JAR is deployed by `dockerDeploy`, which copies `configs/` overlays (including the JAR from a deploy/ directory) into the Docker build context before the image is built. The JAR is baked into the image layer. Docker's layer cache means subsequent runs with an unchanged JAR reuse the cached layer, making rebuilds fast in practice. The alternative (deploying the JAR into the running container via `docker cp` after startup) is reserved for mid-session development loops and is handled by `LiferayContainer.deployJar(...)`.

### D8 — Workspace plugin pinned to 16.0.5

`com.liferay:com.liferay.gradle.plugins.workspace:16.0.5` is the version certified for DXP 2026.Q1.3-LTS. Workspace plugin 16.x requires all subprojects to be declared explicitly in `settings.gradle` via `include 'modules:liferay-dummy-factory'`. The plugin version is pinned in `settings.gradle` to prevent unexpected behavior from future updates.

## Consequences

### Positive

- **CI is simpler**: No Testcontainers library, no Docker-in-Docker workaround, no dynamic port mapping. The workspace plugin's native Docker tasks handle container lifecycle.
- **JaCoCo preserved**: Coverage from the Liferay container JVM is collected exactly as before; only the injection mechanism changes from Testcontainers `withEnv` to `createDockerContainer.withEnvVar`.
- **Testcontainers removed entirely**: `org.testcontainers:testcontainers` and `org.testcontainers:testcontainers-spock` dependencies are deleted from `integration-test/build.gradle`. `LiferayContainer` becomes a thin POJO.
- **Admin bootstrap is one-shot**: `portal-ext.properties` suppresses the password change requirement at image build time. `BaseLiferaySpec` no longer needs to handle `PASSWORDRESET` flow.
- **`release.dxp.api` BOM**: A single dependency declaration pulls in all DXP 2026 APIs at the correct version. No more per-API version management or skew risk.

### Negative

- **Fixed ports conflict warning**: If another Liferay process is already using port 8080 or 11311, `startDockerContainer` will fail. Developers must stop conflicting processes manually. See `docs/details/dxp-2026-gotchas.md`.
- **License file required**: Without `LIFERAY_DXP_LICENSE_FILE` or `LIFERAY_DXP_LICENSE_BASE64`, the build fails before the container starts. CI must have the `LIFERAY_DXP_LICENSE_BASE64` secret configured.
- **Slower first image build**: The first `startDockerContainer` invocation builds the full Docker image from scratch (~5–10 minutes depending on network). Subsequent runs with an unchanged JAR are fast due to layer caching.

### Neutral

- The JSP taglib URI (`http://xmlns.jcp.org/portlet_3_0`) is unchanged — intentionally. This is not a regression; it reflects the actual capability advertised by DXP 2026. See `adr-0002-portlet-api-javax-namespace.md` for the background on the JSP taglib URI decision.
- `BaseLiferaySpec` retains `ensureBundleActive()` and the GoGo Shell polling mechanism unchanged.
- Playwright version (1.59.0) is unchanged.

## References

- PR #42 `yasuflatland-lf/test-factory#42` (closed — reference only)
- Migration plan: `.claude/plan/dxp_migration.md`
- Expert input: Brian Chan (Portal Architect), Marco Leo (Deployment), David H Nebinger (Test Harness)
- Workspace plugin 16.0.5 source: `/home/yasuflatland/tmp/liferay-portal/modules/sdk/gradle-plugins-workspace`
- Supersedes: `docs/ADR/adr-0001-integration-test-architecture.md` (container strategy section)
- API constraints: `docs/details/api-liferay-dxp2026.md`
- Runtime gotchas: `docs/details/dxp-2026-gotchas.md`
