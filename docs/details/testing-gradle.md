# Gradle test execution — concrete details

L3 detail. Source of truth for Gradle task wiring, deploy verification, the incremental build trap, and JaCoCo coverage. Read on demand from `.claude/rules/testing.md` or `.claude/rules/debugging.md`.

## Running tests

```bash
# Build the module JAR first (required by integration tests)
./gradlew :modules:liferay-dummy-factory:jar

# Run integration tests (requires Docker)
./gradlew :integration-test:integrationTest

# Single spec
./gradlew :integration-test:integrationTest \
    --tests "com.liferay.support.tools.it.spec.DeploymentSpec"
```

Integration tests require a DXP license at `configs/local/deploy/activation-key.xml` (gitignored). The workspace plugin's `dockerDeploy` copies everything under `configs/<env>/deploy/` into the image at build time; Liferay's deploy-on-startup machinery picks it up automatically.

- **DXP license is required before each integration test run.** Place the license at `configs/local/deploy/activation-key.xml`. The file is gitignored via `configs/*/deploy/activation-key.xml` and must never be committed. No `LIFERAY_DXP_LICENSE_*` env vars are used.
- The module build depends on `release.dxp.api` (DXP 2026), **not** `release.portal.api`. `com.liferay.journal.api` and `com.liferay.dynamic.data.mapping.api` do not need separate declarations — both are bundled inside `release.dxp.api`. The migration decision is recorded in `docs/ADR/adr-0008-dxp-2026-migration.md`.
- The default `test` task is **disabled** (`enabled = false`). All integration tests run exclusively via the `integrationTest` task.
- The `integrationTest` task automatically depends on `:modules:liferay-dummy-factory:jar`, so a standalone `./gradlew :integration-test:integrationTest` will build the JAR first.
- JVM args: `-Xms4g -Xmx4g`.
- Test logging outputs `passed`, `skipped`, `failed`, `standardOut`, and `standardError`.

## Deploy verification

1. The module JAR is copied into the container at `/opt/liferay/deploy/` using `liferay.deployJar(path)`.
2. The JAR must be pre-built — running `./gradlew :modules:liferay-dummy-factory:jar` is a hard prerequisite.
3. Bundle activation is verified via GoGo Shell: `lb | grep dummy.factory` must show `Active` or `ACTIVE`.
4. The `ensureBundleActive()` method in `BaseLiferaySpec` polls GoGo Shell every 5 seconds for up to 5 minutes until the bundle is active. It is `synchronized` and runs only once per test suite.

## Container configuration file handling

### Only `configs/common/` and `configs/<env>/` (env = `local`) are copied

The workspace plugin generates a Dockerfile that sets `ENV LIFERAY_WORKSPACE_ENVIRONMENT=local`. Its image entrypoint (`100_liferay_image_setup.sh`) copies `/home/liferay/configs/common/` and `/home/liferay/configs/local/` into `$LIFERAY_HOME` before Liferay starts. **`configs/docker/` is never copied.**

All DXP 2026 portal-ext overrides that the integration tests depend on (`setup.wizard.enabled=false`, `passwords.default.policy.change.required=false`, the `configuration.override.*` BasicAuth wiring, `json.servlet.hosts.allowed=`, etc.) live in `configs/common/portal-ext.properties` so every environment inherits the same working baseline. Environment-only knobs (JDBC, etc.) go under `configs/<env>/portal-ext.properties`. The empty `configs/common/portal-liferay-online-config.properties` is required — it shadows the DXP-baked `json.servlet.hosts.allowed=N/A` that otherwise blocks JSONWS (commit `6021828`; see `docs/details/dxp-2026-gotchas.md`).

### Admin password bootstrap

The DXP 2026 Docker image ships HSQL with `USER_.PASSWORDRESET=1` baked into the `test@liferay.com` row, which makes Basic Auth JSONWS / Headless calls return HTTP 403 `{}`. `BaseLiferaySpec.bootstrapAdminCredentials()` runs once per JVM and:

1. Checks Basic Auth with `test` and `Test12345` — if either works, skips.
2. Otherwise walks the form-login → `/c/portal/update_password` ticket flow, POSTs a new password (must differ from old; we use `Test12345`).
3. Polls `GET /api/jsonws/user/get-current-user` for up to 30 s. Because the form-login cookies are cleared before the Basic Auth probe, DXP re-evaluates against the updated DB state immediately. The 30-second poll is a safety margin.
4. Switches `activePassword` to `NEW_PASSWORD=Test12345`.

All subsequent specs inherit the bootstrapped password via `BaseLiferaySpec.basicAuthHeader()`. The ADR at `docs/ADR/adr-0008-dxp-2026-migration.md` has the architectural rationale; the HTTP-client and auth-cache traps (gzip `Accept-Encoding`, 3-minute auth-verifier refresh, cookie-clear before Basic Auth probe, license-activation redirect detection, `p_auth` fallback extraction) live in `docs/details/dxp-2026-gotchas.md`.

## Gradle Incremental Build Trap

`:integration-test:integrationTest` does **not** declare `package.json` as an input. Changing a JavaScript dependency (e.g. bumping React, swapping Jest for Vitest) does not invalidate the `integrationTest` task, so Gradle marks it `UP-TO-DATE` and **replays the previous run's result** without executing anything. A regression introduced in the JS toolchain will appear as a green build.

For any change touching the frontend toolchain, the only trustworthy verification is:

```bash
./gradlew :modules:liferay-dummy-factory:clean :integration-test:clean
./gradlew :integration-test:integrationTest
```

Tell real runs from cached replays by the elapsed time on the `BUILD SUCCESSFUL in Xs` line. A real run is on the order of minutes (container startup + Playwright); a cached replay is single-digit seconds. If the build completes in single-digit seconds, assume the tests did not actually run and re-invoke with the `clean` tasks above.

## Coverage (JaCoCo)

### Host-JVM unit tests

Unit tests under `modules/liferay-dummy-factory/src/test/java` are measured by JaCoCo. The report is generated automatically after `test` via `finalizedBy`.

```bash
./gradlew :modules:liferay-dummy-factory:test
```

Report locations:

- HTML: `modules/liferay-dummy-factory/build/reports/jacoco/test/html/index.html`
- XML:  `modules/liferay-dummy-factory/build/reports/jacoco/test/jacocoTestReport.xml`

### Integration tests — out of scope for DXP-native flow

Container-JVM coverage collection is not wired in the DXP-native workspace-plugin flow. The workspace plugin's generated Dockerfile cannot receive a `-javaagent` directive via env vars the way a Testcontainers-managed container can. Integration coverage is currently off by design. The `jacocoIntegrationReport` task remains as a no-op when no exec data exists.

**Spock `cleanupSpec()` inheritance in Spock 2.x.** A `cleanupSpec()` defined in an abstract base spec IS invoked even when the concrete subclass defines its own `cleanupSpec()`. Spock's `PlatformSpecRunner.doRunCleanupSpec` chains the hierarchy. `BaseLiferaySpec.cleanupSpec()` is therefore guaranteed to run at the end of every spec, regardless of whether the subclass defines its own. No explicit `super.cleanupSpec()` call is needed in subclasses.

## DXP 2026 unit test classpath requirements

`release.dxp.api` does not provide `log4j-core` as a transitive runtime dependency for host-JVM tests. `com.liferay.portal.kernel.log.LogFactoryUtil` (used by `ProgressCallback` and other utility classes) initialises log4j2 statically — any test that loads one of these classes will fail with `NoClassDefFoundError: org/apache/logging/log4j/LogManager` unless log4j-core is explicitly on the test classpath.

Fix: declare `testImplementation 'org.apache.logging.log4j:log4j-core:2.24.3'` in `modules/liferay-dummy-factory/build.gradle`. `release.dxp.api` does not pull log4j transitively — the unit-test classpath needs it declared explicitly.
