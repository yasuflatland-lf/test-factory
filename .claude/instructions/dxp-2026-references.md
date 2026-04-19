# DXP 2026 Migration — Persistent Reference Pointers

**Read this file first** before acting on any DXP 2026 migration task.
Source of truth: `.claude/plan/dxp_migration.md` section 0.

---

## 0.1 Liferay Portal Source Tree

| Item | Path | Purpose |
|---|---|---|
| Root | `/home/yasuflatland/tmp/liferay-portal` | DXP 2026 API signature verification, portal.properties cross-check |
| Workspace plugin | `/home/yasuflatland/tmp/liferay-portal/modules/sdk/gradle-plugins-workspace` | `createDockerContainer` / `buildDockerImage` / `dockerDeploy` behavior |
| WorkspacePlugin.java | `.../modules/sdk/gradle-plugins-workspace/src/main/java/com/liferay/gradle/plugins/workspace/WorkspacePlugin.java` | Plugin entry point |
| WorkspaceExtension.java | `.../modules/sdk/gradle-plugins-workspace/src/main/java/com/liferay/gradle/plugins/workspace/WorkspaceExtension.java` | `liferay.workspace.*` property list |
| RootProjectConfigurator.java | `.../modules/sdk/gradle-plugins-workspace/src/main/java/com/liferay/gradle/plugins/workspace/configurator/RootProjectConfigurator.java` | Task registration, dependency graph, container config (1900-line class, most important) |
| 100_liferay_image_setup.sh.tpl | `.../modules/sdk/gradle-plugins-workspace/src/main/resources/com/liferay/gradle/plugins/workspace/configurator/dependencies/100_liferay_image_setup.sh.tpl` | Configs runtime merge behavior |
| portal.properties | `/home/yasuflatland/tmp/liferay-portal/portal-impl/src/portal.properties` | DXP 2026 authoritative properties |
| portlet.tld | `/home/yasuflatland/tmp/liferay-portal/portal-impl/src/META-INF/portlet.tld` | JSP taglib URI verification |

## 0.2 License File

- **Source**: `/mnt/c/Users/yasuf/Dropbox/Liferay/share/liferay/activation-key-development-7.0de-liferaycom.xml` (845 bytes, readable from WSL)
- **Local deploy target**: `./configs/local/deploy/activation-key.xml` (Gradle copies automatically from env var)
- **CI deploy target**: same (after base64 decode)
- **`.gitignore`**: `configs/*/deploy/activation-key.xml` must be added (prevent accidental commit)

## 0.3 PR #42 (Reference Only)

- GitHub: `yasuflatland-lf/test-factory#42` (`feature/migrate_to_dxp`)
- **Close** after this plan completes. Do NOT merge the code.
- The following docs/ADRs are to be created fresh in part2:
  - `docs/ADR/adr-0008-dxp-2026-migration.md`
  - `docs/details/api-liferay-dxp2026.md`
  - `docs/details/dxp-2026-gotchas.md`

## 0.4 Workspace Plugin Docker Task Graph

```
gradle startDockerContainer
  └─ createDockerContainer          (containerName = ${project.name}-liferay)
       ├─ buildDockerImage          (image = ${project.name}-liferay:${version})
       │    ├─ createDockerfile     (template: 100_liferay_image_setup.sh.tpl)
       │    │    ├─ dockerDeploy    (copies configs/{common,docker,<env>}/ to build/docker/)
       │    │    └─ verifyProductTask
       │    └─ verifyProductTask
       └─ verifyProductTask

gradle stopDockerContainer   (standalone; error handler absorbs "not running")
gradle removeDockerContainer (depends on stopDockerContainer)
```

**Key behaviors**:
- `startDockerContainer` internally chains: `createDockerContainer` → `buildDockerImage` → `dockerDeploy`.
- Fixed port bindings: `8000:8000` (JPDA) / `8080:8080` (HTTP) / `11311:11311` (GoGo).
- **No built-in readiness check** — must implement polling (curl loop) in `awaitLiferayReady` task.
- `LIFERAY_JVM_OPTS` injection: `createDockerContainer { withEnvVar('LIFERAY_JVM_OPTS', '...') }`.
- Configs runtime expansion: `configs/common/` + `configs/<LIFERAY_WORKSPACE_ENVIRONMENT>/` → `/home/liferay/`. `configs/<env>/deploy/` → `/mnt/liferay/deploy/` (license goes here).
