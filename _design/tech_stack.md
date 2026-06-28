# 技術選定: kolor-seed

## 1. 言語

### Kotlin 2.0+（K2 コンパイラ）
- **選定理由**:
  - 制約「Kotlin Multiplatform (JVM/JS/Native)」を直接充足
  - K2 コンパイラで型推論の精度向上・コンパイル速度 2 倍。HCT 変換のような数値演算コードのインライン最適化が改善
  - `inline` 関数の積極活用（制約「inline 関数の積極活用（パフォーマンス）」）で TonalPalette 生成のホットパスでオブジェクト生成を抑制（NFR パフォーマンス「不要なオブジェクト生成を抑制」）
  - `value class`（旧 inline class）で `Color` / `Hct` の値ラッパーをゼロコスト化
  - 制約「リフレクション使用禁止」に Kotlin の静的ディスパッチが合致
- **バージョン方針**: `kotlin("multiplatform") version "2.0.+"` で 2.0 系最新を追従。`languageVersion = "2.0"` を明示
- **却下した代替案**:
  - Java: KMP 非対応。JVM 限定ではプロジェクトの存在意義が薄い
  - Rust + KMP バインディング: 数値演算は高速だが、FFI ブリッジの複雑さとバンドルサイズ増が制約「外部依存ゼロ」に矛盾
  - TypeScript: JS ターゲットのみ。JVM/Native をカバーできない

---

## 2. プラットフォーム

### Kotlin Multiplatform（JVM / JS(IR) / Native）
- **選定理由**:
  - 要件「JVM / JS / Native すべての KMP ターゲットで動作」を直接充足
  - 純粋関数のみ（制約「純粋関数のみ・副作用なし」）の数学ライブラリであり、プラットフォーム固有コードがゼロ。`commonMain` のみで完結
  - JVM: Android / サーバーサイド / CLI ツール
  - JS(IR): Compose for Web / Node.js ツール
  - Native: iOS / macOS / Linux / Windows（Compose Multiplatform 連携）
- **ターゲット一覧**:
  ```kotlin
  kotlin {
      jvm()
      js(IR) { browser(); nodejs() }
      // Native
      macosX64(); macosArm64()
      iosX64(); iosArm64(); iosSimulatorArm64()
      linuxX64()
      mingwX64()
  }
  ```
- **NFR 互換性との対応**: 「KMP ターゲット: JVM (17+), JS (IR backend), Native (macOS x64/arm64, iOS x64/arm64/simulatorArm64, Linux x64, mingwX64)」を全て網羅
- **却下した代替案**:
  - Android 専用ライブラリ: 制約「Android 依存なし（android.graphics.Color 等は禁止）」。KMP が必須
  - Flutter パッケージ: Kotlin エコシステム向け。Dart 実装は別プロジェクト

---

## 3. ビルド

### Gradle 8.x + Kotlin Multiplatform プラグイン
- **選定理由**:
  - KMP の標準ビルドシステム。`kotlin-multiplatform` プラグインが全ターゲットのコンパイル・テスト・パブリッシュを統合管理
  - Gradle 8.x のコンフィグキャッシュで増分ビルド高速化
  - `allOpen` / `noArg` 等のコンパイラプラグインを使わず、純粋 Kotlin のみで構成（制約「外部依存ゼロ」の精神に合致）
- **ビルド構成**:
  ```
  kolor-seed/
  ├── build.gradle.kts          # ルート
  ├── kolor-seed-core/
  │   ├── build.gradle.kts      # KMP: commonMain/commonTest
  │   └── src/
  │       ├── commonMain/       # HCT, TonalPalette, CorePalette, ColorScheme
  │       └── commonTest/       # kotlin.test
  └── kolor-seed-compose/
      ├── build.gradle.kts      # Compose Multiplatform 依存
      └── src/
          ├── commonMain/       # Color.toComposeColor() 等
          └── commonTest/
  ```
- **却下した代替案**:
  - Maven: KMP サポートなし
  - Amper (JetBrains): 実験段階。本番 OSS には時期尚早
  - Bazel: Kotlin KMP サポートが未成熟。Gradle エコシステムから外れるとコントリビューターの参入障壁が上がる

---

## 4. テスト

### kotlin.test（マルチプラットフォーム）+ JUnit5（JVM）
- **選定理由**:
  - `kotlin.test`: KMP 標準のテスト API。`commonTest` に記述するだけで JVM/JS/Native 全ターゲットで実行可能
  - JUnit5: JVM ターゲット限定で `@ParameterizedTest` を活用。HCT ↔ ARGB 変換の大量パラメータテスト（material-color-utilities 出力との一致検証）に最適
  - NFR「テストカバレッジ 95% 以上（コア数学関数は 100%）」を Kover で計測
  - NFR「material-color-utilities (Java) との出力一致率 99.9%（丸め誤差 ±1/255 許容）」を JVM テストで検証。Java 版の出力をゴールデンデータとして保持
- **テスト戦略**:
  - commonTest: HCT 変換の数学的正確性（既知の入出力ペア）、TonalPalette/CorePalette/ColorScheme の生成検証
  - jvmTest: material-color-utilities との全数一致テスト（ゴールデンデータ 1000+ ケース）、JUnit5 パラメタライズド
  - jsTest: 浮動小数点精度の JS 固有検証
  - nativeTest: パフォーマンスベンチマーク（Native 固有最適化の確認）
- **却下した代替案**:
  - Kotest: 表現力が高いが外部依存追加。kotlin.test で十分
  - Spock: JVM 専用・Groovy 依存。KMP 非対応

---

## 5. コードカバレッジ

### Kover（JetBrains）
- **選定理由**:
  - Kotlin 公式のカバレッジツール。KMP プロジェクトの JVM ターゲットを正確に計測
  - Gradle プラグインとして統合。`koverVerify` タスクで CI ゲート化（95% 下限）
  - IntelliJ / Android Studio と統合してローカルでもカバレッジ確認可能
- **却下した代替案**:
  - JaCoCo: Kotlin inline 関数のカバレッジ計測が不正確。Kover は Kotlin コンパイラと連携して正確な計測を実現

---

## 6. リント・静的解析

### ktlint + detekt
- **選定理由**:
  - ktlint: Kotlin 公式コーディング規約の自動適用。フォーマッターとして CI で `ktlintCheck` を実行
  - detekt: 複雑度・コードスメル検出。`complexity` / `performance` ルールセットで数学関数の過度な複雑化を防止
  - NFR「Detekt / ktlint による静的解析ゼロ違反」を CI ゲートで強制
  - 両ツールとも Gradle プラグインとして統合済み
- **カスタムルール**:
  - `detekt.yml` で `TooManyFunctions` のしきい値を調整（色変換ユーティリティは関数数が多くなる傾向）
  - ktlint の `no-wildcard-imports` を有効化
- **却下した代替案**:
  - Diktat: ルールが独自的すぎてコミュニティ標準から外れる
  - Spotless: フォーマッターのみ。静的解析機能がない（detekt との併用が必要で二重管理）

---

## 7. CI / CD

### GitHub Actions（マトリクス: JVM / JS / Native）
- **選定理由**:
  - OSS の標準 CI プラットフォーム。Kotlin コミュニティの期待に合致
  - マトリクスビルドで全 KMP ターゲットの動作保証（NFR CI/CD「GitHub Actions: lint → test → publish」）
  - ワークフロー構成:
    - **PR**: ktlint → detekt → test(JVM) → test(JS) → test(Native) → koverVerify (95%) → API 互換性チェック
    - **main push**: 上記 + snapshot publish to Sonatype OSSRH
    - **tag push (v*)**: build → publish to Maven Central → GitHub Release
  - Native テストは macOS runner（`macos-latest`）で iOS/macOS ターゲットを実行。Linux/Windows は対応 runner で実行
- **却下した代替案**:
  - JetBrains Space: Kotlin 親和性は高いが、OSS コミュニティの主戦場は GitHub
  - TeamCity: セルフホスト型。OSS には GitHub Actions の無料枠が優位

---

## 8. パッケージ公開

### Maven Central via Sonatype（semantic versioning）
- **選定理由**:
  - Kotlin / JVM エコシステムの標準配布チャネル
  - `semver` に厳密に従い、breaking change は major bump
  - Sonatype OSSRH 経由で Maven Central に publish（NFR CI/CD「タグプッシュで Maven Central release」）
  - `maven-publish` プラグイン + `signing` プラグインで GPG 署名付きアーティファクト公開
- **アーティファクト構成**:
  ```
  com.github.<org>:kolor-seed-core:<version>       # コアモジュール（HCT, Palette, Scheme）
  com.github.<org>:kolor-seed-compose:<version>     # Compose 拡張（Color 相互変換）
  ```
  - 各 KMP ターゲット用のアーティファクト（`-jvm`, `-js`, `-native` 等）は Gradle KMP プラグインが自動生成
- **却下した代替案**:
  - JitPack: ビルドの信頼性が低く、Maven Central ほどの信頼がない
  - GitHub Packages: Maven Central と比較して検索性・発見可能性が劣る

---

## 9. API 互換性

### Binary Compatibility Validator（kotlinx.binary-compatibility-validator）
- **選定理由**:
  - NFR CI/CD「API 互換性チェック（binary compatibility validator）」を直接充足
  - public API の変更を `.api` ファイルで追跡。意図しない破壊的変更を CI で検出
  - JetBrains 公式ツール。KMP プロジェクトに対応
  - `apiCheck` タスクを PR の CI ゲートに設定
- **却下した代替案**:
  - 手動 API レビュー: 漏れが発生する。自動化すべき

---

## 10. ドキュメント

### Dokka（KDoc → HTML）
- **選定理由**:
  - Kotlin 公式のドキュメントエンジン。KDoc コメントから API リファレンスを自動生成
  - KMP 対応: 全ターゲットの API を統合ドキュメント化
  - GitHub Pages にデプロイ
  - NFR「全 public API に KDoc」「README.md は英語」を遵守
- **却下した代替案**:
  - Javadoc: Kotlin の KDoc 構文に非対応
  - Docusaurus: ライブラリ規模に対してオーバーエンジニアリング

---

## 11. Compose 拡張モジュール

### kolor-seed-compose（別アーティファクト）
- **選定理由**:
  - 要件 S-04「Compose Color 拡張（別アーティファクト `kolor-seed-compose`）」を直接充足
  - `compose-ui`（`androidx.compose.ui:ui` / Compose Multiplatform）への依存をコアモジュールから分離
  - コアの `Color` ↔ Compose `Color` 相互変換拡張関数を提供
  - Compose を使わないプロジェクト（CLI ツール、サーバーサイド）はコアモジュールのみで利用可能
- **依存構成**:
  ```kotlin
  // kolor-seed-compose/build.gradle.kts
  kotlin {
      sourceSets {
          commonMain.dependencies {
              api(project(":kolor-seed-core"))
              implementation(compose.ui)  // Compose Multiplatform
          }
      }
  }
  ```
- **Compose Multiplatform バージョン**: 1.6+（NFR 互換性）
- **却下した代替案**:
  - コアモジュールに Compose 依存を含める: 非 Compose ユーザーに不要な依存を強制。制約「外部依存ゼロ」の精神に反する

---

## 12. 依存関係サマリ

### kolor-seed-core

| 区分 | パッケージ | バージョン | 種別 |
|------|-----------|-----------|------|
| Runtime | kotlin-stdlib | Kotlin 2.0+ 同梱 | 暗黙依存 |
| Test | kotlin.test | Kotlin 同梱 | commonTest |
| Test | JUnit5 | ^5.10.0 | jvmTest のみ |
| Lint | ktlint | ^1.3.0 | Gradle プラグイン |
| Lint | detekt | ^1.23.0 | Gradle プラグイン |
| Coverage | Kover | ^0.8.0 | Gradle プラグイン |
| API compat | binary-compatibility-validator | ^0.16.0 | Gradle プラグイン |
| Doc | Dokka | ^1.9.0 | Gradle プラグイン |

**外部 runtime dependency: ゼロ**（制約「外部依存ゼロ（kotlin-stdlib 以外）」充足）

### kolor-seed-compose

| 区分 | パッケージ | バージョン | 種別 |
|------|-----------|-----------|------|
| Runtime | kolor-seed-core | プロジェクト内参照 | api 依存 |
| Runtime | compose-ui | Compose Multiplatform 1.6+ | implementation |

---

## 13. ライセンス

### Apache License 2.0
- **選定理由**:
  - 制約「Apache 2.0 License（Kotlin ecosystem 慣例）」を直接充足
  - Kotlin 本体・KotlinX ライブラリ群・Compose Multiplatform が Apache 2.0 を採用しており、エコシステムの慣例に従う
  - 特許条項があり、コントリビューターからの特許攻撃を防御
- **却下した代替案**:
  - MIT: よりシンプルだが Kotlin エコシステムの慣例と異なる

---

## 14. 開発環境セットアップ

```bash
# 前提: JDK 17+ / Kotlin 2.0+
git clone https://github.com/<org>/kolor-seed.git
cd kolor-seed

# ビルド
./gradlew build

# テスト（全ターゲット）
./gradlew allTests

# テスト（JVM のみ — 高速フィードバック）
./gradlew :kolor-seed-core:jvmTest

# テスト（JS）
./gradlew :kolor-seed-core:jsTest

# テスト（Native — macOS のみ）
./gradlew :kolor-seed-core:macosArm64Test

# カバレッジ
./gradlew koverVerify        # 95% ゲート
./gradlew koverHtmlReport    # HTML レポート生成

# リント
./gradlew ktlintCheck
./gradlew detekt

# API 互換性チェック
./gradlew apiCheck

# ドキュメント生成
./gradlew dokkaHtml

# ローカル Maven publish（テスト用）
./gradlew publishToMavenLocal
```

### 推奨エディタ
- IntelliJ IDEA 2024.1+（K2 モード有効化）
- Android Studio Koala+（KMP プロジェクト対応）

### ゴールデンデータ生成（初回のみ）
- material-color-utilities (Java) を使って 1000+ ケースのゴールデンデータを生成し `testResources/golden/` に配置
- `./gradlew :kolor-seed-core:generateGoldenData`（カスタムタスク）で再生成可能

---

## セルフチェック

- [x] 全技術選定に要件・制約との紐付けがあるか
- [x] 却下した代替案とその理由が記載されているか
- [x] 外部 runtime dependency がゼロであるか（kotlin-stdlib 以外）
- [x] Android 依存がないか（android.graphics.Color 等の禁止）
- [x] 純粋関数のみ・副作用なしの制約が設計に反映されているか
- [x] リフレクション不使用であるか
- [x] inline 関数の積極活用が設計に含まれるか
- [x] 全ターゲット（JVM/JS/Native）のテスト戦略があるか
- [x] テストカバレッジ目標（95%）の計測手段があるか
- [x] material-color-utilities との一致検証手段があるか
- [x] API 互換性チェックがあるか
- [x] Apache 2.0 License が明記されているか
- [x] Compose 拡張が別アーティファクトとして分離されているか
- [x] 開発環境セットアップ手順があるか
