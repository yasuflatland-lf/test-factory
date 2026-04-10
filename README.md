# test-factory

A Liferay Portal 7.4 (CE GA132) workspace featuring a Calculator portlet built with Service Builder + React, along with Testcontainers-based integration tests.

## Project Structure

```
test-factory/
├── modules/
│   └── test-factory-calculator/   # OSGi bundle (API + Service + Web)
│       ├── service.xml            # CalcEntry entity definition
│       └── src/main/
│           ├── java/              # MVCPortlet, Service Builder
│           └── resources/
│               └── META-INF/resources/
│                   └── js/        # React frontend
├── integration-test/              # Spock + Testcontainers + Playwright
│   └── src/test/groovy/
└── .github/workflows/             # CI (GitHub Actions)
```

## Tech Stack

| Layer | Technology |
|-------|------------|
| Portal | Liferay Portal 7.4.3.132-ga132 |
| Backend | Service Builder (CalcEntry entity) |
| Frontend | React + Clay CSS |
| Build | Gradle 8.5 + Liferay Workspace Plugin 10.1.9 |
| Testing | Spock 2.4 / Groovy 5.0 / Testcontainers 2.0.4 / Playwright 1.42.0 |
| Java | JDK 21 |

## Quick Start (Docker)

Docker で Liferay を起動し、Calculator ポートレットをデプロイする手順です。

### 1. Liferay コンテナを起動

```bash
# フォアグラウンド起動（ログを直接確認できる、Ctrl+C で停止）
docker run -it -m 8g -p 8080:8080 -p 11311:11311 \
  -e LIFERAY_SETUP_PERIOD_WIZARD_PERIOD_ENABLED=false \
  -e LIFERAY_TERMS_PERIOD_OF_PERIOD_USE_PERIOD_REQUIRED=false \
  -e LIFERAY_USERS_PERIOD_REMINDER_PERIOD_QUERIES_PERIOD_ENABLED=false \
  liferay/portal:7.4.3.132-ga132

# バックグラウンド起動（コンテナ名を指定）
docker run -d --name liferay -m 8g -p 8080:8080 -p 11311:11311 \
  -e LIFERAY_SETUP_PERIOD_WIZARD_PERIOD_ENABLED=false \
  -e LIFERAY_TERMS_PERIOD_OF_PERIOD_USE_PERIOD_REQUIRED=false \
  -e LIFERAY_USERS_PERIOD_REMINDER_PERIOD_QUERIES_PERIOD_ENABLED=false \
  liferay/portal:7.4.3.132-ga132
```

| オプション | 説明 |
|-----------|------|
| `-m 8g` | メモリ上限 8GB |
| `-p 8080:8080` | HTTP ポート |
| `-p 11311:11311` | GoGo Shell ポート（バンドル状態確認用） |
| 環境変数 | セットアップウィザード・利用規約・リマインダーを無効化 |

起動完了まで約 5-8 分かかります。バックグラウンド起動の場合は以下で確認：

```bash
docker logs -f liferay   # "Server startup" メッセージが出れば完了、Ctrl+C で抜ける
```

既存のコンテナを再起動する場合：

```bash
docker start liferay && docker logs -f liferay
```

### 2. ポートレットのビルドとデプロイ

```bash
# モジュール JAR をビルド
./gradlew :modules:test-factory-calculator:jar

# JAR をコンテナにデプロイ
docker cp modules/test-factory-calculator/build/libs/com.liferay.test.factory-1.0.0.jar liferay:/opt/liferay/deploy/

# デプロイ確認（STARTED が表示されれば OK）
docker logs -f liferay 2>&1 | grep -i "test.factory"
```

### 3. 動作確認

http://localhost:8080 にアクセスし、admin (`test@liferay.com` / `test`) でログイン。
Control Panel > Configuration に **Test Factory Calculator** が表示されます。

GoGo Shell でバンドル状態を確認：

```bash
docker exec liferay bash -c "(echo 'lb test.factory'; sleep 2) | telnet localhost 11311"
```

## Build

```bash
# Service Builder code generation
./gradlew :modules:test-factory-calculator:buildService

# Module build
./gradlew :modules:test-factory-calculator:build
```

## Testing

Requires Docker to be running.

```bash
# Run all integration tests
./gradlew :integration-test:integrationTest

# Run a specific spec
./gradlew :integration-test:integrationTest --tests "com.liferay.test.factory.it.spec.DeploymentSpec"
```

Tests automatically start a Liferay container via Testcontainers and verify:

- **DeploymentSpec** -- Bundle deployment and activation (via GoGo Shell)
- **CalculatorHappyPathSpec** -- Login and calculation through the browser (Playwright)

## CI

GitHub Actions (`.github/workflows/integration-test.yml`) runs automatically on push / PR to `master`.
