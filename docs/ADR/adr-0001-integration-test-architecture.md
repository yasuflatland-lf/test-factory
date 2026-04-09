# ADR-0001: Integration Test Architecture for Liferay Portal CE

## Status

Accepted (partially implemented — PanelApp navigation on CE pending)

## Date

2026-04-09

## Context

test-factory プロジェクトの calculator ポートレット（Service Builder + React）に対して、Testcontainers を使った E2E インテグレーションテストを構築する必要がある。

### 制約

- **ターゲット**: Liferay Portal CE 7.4 GA132（DXP ではなく Community Edition）
- **実行環境**: WSL2 + Docker Desktop 29.x
- **ビルド**: Gradle 8.5 + Liferay Workspace Plugin
- **テスト対象**: OSGi バンドルのデプロイ確認、GoGo Shell によるバンドル状態検証、Playwright によるブラウザ E2E テスト

## Decision

### 1. テストフレームワーク構成

| コンポーネント | 選択 | 理由 |
|-------------|------|------|
| テストフレームワーク | Spock 2.4 + Groovy 5.0.4 | Groovy の簡潔な記法、`@Stepwise` によるテスト順序制御、Power Assert |
| コンテナ管理 | Testcontainers 2.0.4 | Docker Engine 29.x 対応（docker-java 3.7.1 同梱）。1.21.x はシェーディング版 docker-java が API v1.44 まで、Docker 29.x の最小 API v1.40 に対応不可 |
| ブラウザテスト | Playwright Java 1.42.0 (Chromium only) | Liferay 公式テストと同じ技術スタック。Chromium のみインストールしてダウンロード時間を短縮 |
| GoGo Shell 通信 | Apache Commons Net (Telnet) | Liferay OSGi コンソールへの接続に使用 |

### 2. ログイン方式: API POST (CSRF トークン付き)

**決定**: Liferay 公式 Playwright テスト (`performLoginViaApi`) と同じ方式を採用。

```
1. page.navigate("/") でセッション確立
2. page.evaluate("() => Liferay.authToken") で CSRF トークン取得
3. page.request().post("/c/portal/login") に CSRF トークン付きで POST
4. page.navigate("/") でリロード
```

**却下した代替案**:
- **フォームログイン**: ログイン後のリダイレクトで `/web/guest` に遷移するとセッションが引き継がれない問題。パスワード変更ページの条件分岐が複雑。
- **Basic 認証**: Liferay CE ではデフォルトで無効。

### 3. パスワードポリシー対応

**決定**: 複数パスワードの順次試行 + `portal-ext.properties` 配置。

- `portal-ext.properties` を `/opt/liferay/tomcat/webapps/ROOT/WEB-INF/classes/` に配置（`withCopyToContainer`）
  - ただし Docker イメージの事前構築済み DB には `passwords.default.policy.change.required=false` が効かない
- テスト側で `test` と `Test12345` の両方を順に試行し、`withReuse(true)` でパスワード変更が永続化されたコンテナにも対応
- パスワード変更ページが出た場合は自動的にハンドリング

**却下した代替案**:
- **環境変数 `LIFERAY_PASSWORDS_DEFAULT_POLICY_CHANGE_REQUIRED`**: Liferay が認識しなかった
- **GoGo Shell 経由 DB 更新**: OSGi コンソールでは SQL 直接実行不可
- **Groovy スクリプト実行**: GoGo Shell の `groovy:exec` は CE Docker イメージで利用不可

### 4. JAR デプロイ方式: /tmp 経由コピー + chown

**決定**: `copyFileToContainer` で `/tmp` に配置 → `execInContainer` で `cp` + `chown liferay:liferay` して `/opt/liferay/deploy/` に移動。

**理由**: `copyFileToContainer` は root 所有でファイルを作成するが、Liferay の AutoDeployScanner は liferay ユーザー (uid=1000) で実行されるため、直接 `/deploy/` にコピーすると `Unable to write` エラーが発生する。

### 5. GoGo Shell バンドル検証: 全出力取得 + Java フィルタリング

**決定**: `lb` コマンドの全出力（約 1394 行）を取得し、Java/Groovy 側で `Test Factory` を含む行をフィルタリング。

**理由**: GoGo Shell は OSGi コンソールであり、Unix シェルのパイプ (`|`) や `grep` コマンドは使用不可。`lb | grep test.factory` は `grep` コマンドが `false` を返すだけ。

### 6. コンテナ設定

```groovy
withReuse(true)                    // 起動に2-3分かかるため再利用
withCopyToContainer(...)           // portal-ext.properties 配置
withEnv([                          // 環境変数
    'LIFERAY_SETUP_WIZARD_ENABLED': 'false',
    'LIFERAY_TERMS_OF_USE_REQUIRED': 'false',
    'LIFERAY_USERS_REMINDER_QUERY_ENABLED': 'false',
])
```

## Consequences

### Positive

- Liferay 公式 Playwright テストパターンに準拠したログイン方式
- `withReuse(true)` により開発中のテスト実行が高速（コンテナ起動不要）
- Chromium のみインストールによりダウンロード時間短縮
- Testcontainers 2.0.4 で Docker Engine 29.x の最新版に対応

### Negative

- **CE 版の Global Menu 不在**: DXP 専用の Global Menu (`Open Applications Menu`) が CE GA132 に存在しない。PanelApp (`CONTROL_PANEL_CONFIGURATION`) へのブラウザナビゲーションが未解決。
- `withReuse(true)` によるパスワード変更の永続化に対応するため、複数パスワード試行のロジックが必要
- `portal-ext.properties` が Docker イメージの事前構築済み DB に対して一部のプロパティ（`passwords.default.policy.change.required`）が無効

### Open Questions

1. **CE GA132 での PanelApp ナビゲーション**: Product Menu サイドバーに Control Panel セクションが表示されない。URL 直接アクセス (`/group/control_panel/manage`) は 404。次の選択肢を検討:
   - PanelApp の `panel.category.key` を変更してサイト管理セクションに配置
   - ポートレットの `display-category` を変更してウィジェットページに配置可能にし、ページ配置テストに切り替え
   - CE 版の Control Panel への正しいアクセスパスを特定（手動ブラウザ確認が必要）

2. **Playwright バージョン**: 現在 1.42.0 を使用。Liferay 公式テストとの互換性を維持しつつ、必要に応じて更新を検討。

## References

- Liferay Portal ソース: `/home/yasuflatland/tmp/liferay-portal`
- Liferay 公式 Playwright テスト: `modules/test/playwright/`
  - `utils/performLogin.ts` — API ログインパターン
  - `helpers/ApiHelpers.ts` — CSRF トークン取得
  - `pages/product-navigation-applications-menu/GlobalMenuPage.ts` — Global Menu (DXP)
  - `utils/productMenu.ts` — Product Menu
  - `env/portal-ext.properties` — テスト用プロパティ
- Testcontainers ソース: `/home/yasuflatland/tmp/testcontainers-java`
- 詳細な実装計画: `.claude/plan/integrationtest.md`
