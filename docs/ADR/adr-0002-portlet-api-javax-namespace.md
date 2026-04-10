# ADR-0002: Portlet API は javax.portlet (3.0) を使用する

## Status

Accepted

## Date

2026-04-10

## Context

Calculator ポートレットを Docker イメージ `liferay/portal:7.4.3.132-ga132` にデプロイしたところ、バンドルは Active だが PanelApp の `@Reference(com.liferay.portal.kernel.model.Portlet)` が UNSATISFIED のままで、Control Panel にポートレットが表示されなかった。

### 調査の経緯

1. GoGo Shell で確認すると、ポートレットの SCR コンポーネントは SATISFIED で `jakarta.portlet.Portlet` としてサービス登録されていた
2. しかし PanelApp が依存する `com.liferay.portal.kernel.model.Portlet` サービスが作成されなかった
3. Liferay ソースの `PortletTracker` を追跡し、`_addingPortlet()` → `setReady(true)` → `model.Portlet` OSGi サービス登録の流れを特定
4. PortletTracker のログ（`"Adding"`, `"Added"`, `"failed to initialize"`, `"already in use"`）が一切出力されていなかった → `addingService()` 自体が呼ばれていない
5. **決定的発見**: Docker コンテナ内の PortletTracker（バンドル 27、`com.liferay.portal.osgi.web.portlet.tracker:6.0.39`）の `Import-Package` を確認したところ、`javax.portlet` をインポートしていた（`jakarta.portlet` ではない）

### 根本原因

| レイヤー | 使用していた名前空間 | Portlet API バージョン |
|---------|-------------------|---------------------|
| ビルド依存関係 (`release.dxp.api:2026.q1.2`) | `jakarta.portlet` | 4.0 |
| Docker イメージ (`liferay/portal:7.4.3.132-ga132`) | `javax.portlet` | 3.0 |

`release.dxp.api:default` は DXP 2024+ 向けの API（`2026.q1.2`）に解決されており、CE 7.4 GA132 のランタイムと互換性がなかった。PortletTracker の `ServiceTracker` は `javax.portlet.Portlet` を追跡するため、`jakarta.portlet.Portlet` として登録されたポートレットは検出できなかった。

## Decision

### 1. ビルド依存関係を CE 版に変更

```groovy
// Before (DXP 2024+ API — jakarta 名前空間)
compileOnly group: "com.liferay.portal", name: "release.dxp.api", version: "default"
// → 2026.q1.2 に解決される

// After (CE 7.4 GA132 API — javax 名前空間)
compileOnly group: "com.liferay.portal", name: "release.portal.api", version: "default"
// → 7.4.3.132 に解決される
```

### 2. ポートレットコードで `javax.portlet` を使用

```java
// Before
import jakarta.portlet.Portlet;
// @Component property: "jakarta.portlet.name=...", "jakarta.portlet.version=4.0"

// After
import javax.portlet.Portlet;
// @Component property: "javax.portlet.name=...", "javax.portlet.version=3.0"
```

### 3. PanelApp の @Reference ターゲットも統一

```java
// Before
@Reference(target = "(jakarta.portlet.name=" + ... + ")")

// After
@Reference(target = "(javax.portlet.name=" + ... + ")")
```

## Consequences

### Positive

- ビルド API とランタイム（Docker イメージ）のバージョンが一致し、OSGi サービス追跡が正常に動作する
- PortletTracker が `javax.portlet.Portlet` サービスを検出し、`com.liferay.portal.kernel.model.Portlet` を登録するため、PanelApp の `@Reference` が解決される

### Negative

- `.claude/rules/code-conventions.md` の「`jakarta.portlet` を使用する」ルールは CE 7.4 GA132 には適用されない。DXP 2024+ / CE GA120+ でこのルールが有効になるのは、対応する `release.portal.api` が jakarta 名前空間を提供するバージョン以降のみ

### 教訓: デバッグ手法

PortletTracker が無反応な場合のデバッグ手順:

1. `scr:info <PanelApp FQCN>` — コンポーネント状態と UNSATISFIED REFERENCE の確認
2. `scr:info <Portlet FQCN>` — ポートレットコンポーネントの状態確認
3. `services jakarta.portlet.Portlet` / `services javax.portlet.Portlet` — サービス登録の名前空間確認
4. `headers <PortletTracker bundle ID>` — PortletTracker の `Import-Package` で `javax.portlet` か `jakarta.portlet` かを確認
5. `docker logs <container> 2>&1 | grep "failed to initialize"` — PortletTracker の初期化エラー確認

## References

- Liferay PortletTracker ソース: `modules/apps/static/portal-osgi-web/portal-osgi-web-portlet-tracker/`
  - `PortletTracker.java` — `addingService()` (L119-213), `_addingPortlet()` (L363-473)
- `PortletImpl.java` — `setReady(true)` が `com.liferay.portal.kernel.model.Portlet` を OSGi サービスとして登録 (L3725-3758)
- ADR-0001: Integration Test Architecture
