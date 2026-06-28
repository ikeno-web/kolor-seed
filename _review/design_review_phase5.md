# 技術選定レビュー結果: kolor-seed

**レビュー対象**: `_design/tech_stack.md`
**参照**: `_design/requirements.md`, `_meta/constraints.md`
**レビュー日**: 2026-06-28
**レビュアー**: Reviewer Agent

---

## チェック項目

- [x] 要件に対してオーバースペックでないか
- [x] 各技術の組み合わせに既知の不整合がないか
- [x] constraints.mdの禁止技術を採用していないか
- [x] ビルド・テスト・CI環境が要件の品質基準を満たせるか
- [x] ライセンスに問題がないか

---

## 詳細レビュー

### 1. 要件に対してオーバースペックでないか

合格。選定は適切。

- Kotlin 2.0+ (K2): value class によるゼロコストラッパーや inline 関数最適化がパフォーマンス要件に直結しており、過剰ではない
- Gradle 8.x + KMP プラグイン: KMP の事実上唯一の選択肢。Amper/Bazel を却下した判断は妥当
- ktlint + detekt: Kotlin エコシステム標準の組み合わせ。2ツール併用は冗長に見えるが、フォーマッター (ktlint) と静的解析 (detekt) は責務が異なるため適切
- Binary Compatibility Validator: NFR で API 互換性チェックが明示されており、必要な選定
- Dokka: Kotlin 公式ドキュメントツール。KMP 対応の唯一の選択肢であり過不足なし

### 2. 各技術の組み合わせに既知の不整合がないか

合格。不整合なし。

- Kotlin 2.0 + Gradle 8.x + KMP プラグイン: K2 コンパイラは Gradle 8.x で安定サポート
- kotlin.test + JUnit5 (JVM): commonTest で kotlin.test、jvmTest で JUnit5 パラメタライズドテストの併用は KMP プロジェクトの標準パターン
- Kover + inline 関数: tech_stack.md セクション5 で JaCoCo の inline 関数計測不正確問題を指摘し、Kover を選定。正しい判断
- Compose Multiplatform 1.6+ と kolor-seed-compose: 別アーティファクトとして分離されており、コアモジュールに Compose 依存を持ち込まない設計は適切

### 3. constraints.md の禁止技術を採用していないか

合格。全制約を充足。

| 制約 | 充足状況 |
|------|----------|
| Kotlin Multiplatform (JVM/JS/Native) | 全ターゲットを明示。commonMain のみで完結 |
| 純粋関数のみ（副作用なし） | 設計方針として明記。mutable グローバル状態なし |
| Android 依存なし | android.graphics.Color 等の禁止を遵守。commonMain のみ |
| 外部依存ゼロ（kotlin-stdlib 以外） | runtime dependency ゼロ。依存サマリで確認済み |
| mutable なグローバル状態禁止 | 純粋関数 + value class + スレッドセーフ設計 |
| HCT 色空間の正確な実装 | material-color-utilities との一致率 99.9% をゴールデンテストで検証 |
| 全関数がスレッドセーフ | 純粋関数 + 不変データで暗黙的に保証 |
| inline 関数の積極活用 | セクション1 で明記。value class + inline でオブジェクト生成抑制 |
| リフレクション使用禁止 | Kotlin の静的ディスパッチに合致。リフレクション API 不使用 |
| README 英語 | 明記済み |
| Apache 2.0 License | セクション13 で明記。Kotlin エコシステム慣例に従う |
| テストカバレッジ 95% 以上 | Kover + koverVerify タスクで CI ゲート化 |
| Maven Central publish 可能 | Sonatype OSSRH 経由の publish 構成を明記。GPG 署名あり |
| KDoc 全 public API | Dokka による自動生成 + NFR で要求 |

### 4. ビルド・テスト・CI 環境が要件の品質基準を満たせるか

合格。

- **パフォーマンス NFR**: nativeTest でベンチマーク実行可能。inline + value class によるオブジェクト生成抑制が設計に組込まれている
- **サイズ NFR**: コア < 50KB (JVM JAR)。外部依存ゼロのため達成可能性が高い
- **品質 NFR 95%**: Kover で CI ゲート化。コア数学関数 100% はゴールデンテスト (1000+ ケース) でカバー
- **material-color-utilities 一致率 99.9%**: jvmTest でゴールデンデータとの全数比較テストを実行。生成タスク (`generateGoldenData`) も用意
- **CI マトリクス**: JVM / JS / Native の全ターゲットをマトリクスビルド。Native テストは macOS runner で iOS/macOS をカバー。Linux/Windows は対応 runner
- **API 互換性**: binary-compatibility-validator の `apiCheck` を PR ゲートに設定

### 5. ライセンスに問題がないか

合格。

- 本ライブラリ: Apache License 2.0 (Kotlin エコシステム慣例)
- kotlin-stdlib: Apache 2.0 -- 同一ライセンス
- Compose Multiplatform (compose 拡張のみ): Apache 2.0 -- 同一ライセンス
- JUnit5 (dev, JVM のみ): Eclipse Public License 2.0 -- テスト専用、配布物に含まれないため問題なし
- ktlint, detekt, Kover, Dokka, binary-compatibility-validator: いずれも Apache 2.0 系。devDependency/Gradle プラグインのため配布物に含まれない

**補足**: requirements.md セクション1 で「Kotlin: 1.9+（K2 compiler 対応）」と記載されているが、tech_stack.md では「Kotlin 2.0+」を選定している。K2 コンパイラは Kotlin 2.0 で安定版となったため、tech_stack.md の 2.0+ が正しい判断である。requirements.md の「1.9+」は K2 プレビュー対応を含む最低ラインの記述であり、矛盾ではなく上位互換として問題ない。

---

## 指摘事項

なし。全項目において要件・制約との整合性が確認できた。KMP プロジェクトとしての技術選定が堅実で、コアとCompose拡張の分離も適切。

---

## 判定

**PASS**
