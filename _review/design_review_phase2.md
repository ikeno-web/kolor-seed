# API設計レビュー結果

## レビュー対象
- ファイル: `_design/screen_flow.md`
- レビュー日: 2026-06-28
- レビュアー: Reviewer Agent

## チェック項目
- [x] 全MUSTユーザーストーリーがマトリクスでカバーされている
- [x] APIに行き止まりがない（初期化→使用→破棄の完全なライフサイクル）
- [x] エラーケースが定義されている
- [x] 型安全性が確保されている
- [x] 命名規則が一貫している
- [x] requirements.mdの非機能要件と整合している

## 詳細評価

### 1. MUSTユーザーストーリーカバレッジ

全MUST機能（M-01〜M-08）がAPIにマッピングされている。

| MUST機能 | カバー状況 | 対応API |
|---|---|---|
| M-01 HCT色空間 | OK | `Hct.from()`, `Hct.fromColor()`, `Hct.fromArgb()`, `withHue/Chroma/Tone()` |
| M-02 TonalPalette | OK | `TonalPalette.fromHueAndChroma()`, `TonalPalette.fromColor()`, `tone()`, `toMap()` |
| M-03 CorePalette | OK | `CorePalette.fromSeed()`, `CorePalette.fromHex()`, 6グループ構成 |
| M-04 Light ColorScheme | OK | `ColorScheme.light()`, `CorePalette.lightScheme()` |
| M-05 Dark ColorScheme | OK | `ColorScheme.dark()`, `CorePalette.darkScheme()` |
| M-06 HEX変換 | OK | `Color.toHex()`, `Color.fromHex()` — `#RGB`, `#RRGGBB`, `#AARRGGBB` 対応 |
| M-07 RGB変換 | OK | `Color.fromRgb()`, `Color.red/green/blue/alpha` プロパティ |
| M-08 ARGB↔sRGB線形化 | OK | `internal/ColorUtils.kt` (内部実装、APIには非公開で正しい) |

### 2. ライフサイクル完全性

本ライブラリは純粋関数のみで構成されるステートレス設計のため、「初期化→破棄」のライフサイクルは不要。これは正しい設計判断。

- **生成**: `Color.fromHex()` → `CorePalette.fromSeed()` → `ColorScheme.light()` / `.dark()` の変換チェーンが一方向に自然に流れる
- **変換**: `Color` ↔ `Hct` ↔ `HslColor` の相互変換が完備
- **出力**: `toJson()`, `toCssCustomProperties()`, `toFigmaTokens()` で外部システムへの橋渡し
- **Compose統合**: `toComposeColor()` / `toM3ColorScheme()` で Compose への直接適用

行き止まりなし。全入口から全出口への変換パスが存在する。

### 3. エラーケース

- `Color.fromRgb()`: コンポーネント範囲外で `IllegalArgumentException`
- `Color.fromHex()`: 不正フォーマットで `IllegalArgumentException`
- `TonalPalette.tone()`: トーン範囲外 (0..100 外) で `IllegalArgumentException`
- `ColorScheme.fromJson()`: 不正JSONで `IllegalArgumentException`

純粋関数ライブラリとして、入力バリデーション例外が適切に定義されている。非同期処理やI/Oがないため、これで十分。

### 4. 型安全性

- `@JvmInline value class Color` でボクシング回避かつ型安全性確保
- `Hct` は `private constructor` + ファクトリで不変条件を強制
- `data class ColorScheme` で 35 色ロールを名前付きプロパティとして型安全に保持
- `data class HslColor` / `CustomColorGroup` / `CustomColorRoles` も適切
- `TonalPalette` / `CorePalette` は不変オブジェクトとして設計

Kotlin の型システムを最大限に活用した堅牢な設計。

### 5. 命名規則

- クラス: PascalCase — `Color`, `Hct`, `TonalPalette`, `CorePalette`, `ColorScheme` — 一貫
- ファクトリ: `from` / `fromXxx` — `from()`, `fromSeed()`, `fromHex()`, `fromColor()`, `fromArgb()` — Kotlin慣例準拠
- 変換: `toXxx` — `toHex()`, `toHct()`, `toHsl()`, `toJson()`, `toComposeColor()` — Kotlin慣例準拠
- オブジェクト: PascalCase — `Blend`, `Contrast` — 一貫
- ビルダー風: `withXxx` — `withHue()`, `withChroma()`, `withTone()`, `withCustomColor()` — 一貫

命名パターンが極めて一貫しており、Kotlin / KMP エコシステムの慣例に完全準拠。

### 6. 非機能要件との整合

| NFR | 設計での対応 | 判定 |
|---|---|---|
| パレット生成 < 1ms (JVM) | `@JvmInline value class` でボクシング回避、トーンキャッシュ設計 | OK |
| TonalPalette < 100us | 内部キャッシュ設計記載あり | OK |
| コアモジュール < 50KB | 外部依存ゼロ、`internal/` で実装隠蔽 | OK |
| Compose拡張 < 10KB | 拡張関数3つのみで最小 | OK |
| Kotlin 1.9+ / K2対応 | KMP標準構成で阻害要因なし | OK |
| KMPターゲット全対応 | `internal/` の数学演算がプラットフォーム非依存 | OK |
| テストカバレッジ 95% | 全APIが純粋関数で入出力テスト容易 | OK |
| material-color-utilities一致率 99.9% | 設計上の阻害要因なし（実装+テストで検証） | OK |

## 指摘事項

### [INFO-01] `ColorScheme` の `companion object` が2つ記述されている

`screen_flow.md` セクション 2.7 で `ColorScheme` の `companion object` ブロックが2箇所に分かれて記述されている（`light()` / `dark()` / `fromCorePalette()` と `fromJson()`）。Kotlinでは `companion object` は1つしか持てないため、実装時には統合が必要。設計ドキュメント上の記述分割であり、実装への影響はない。

**重要度**: 極低（ドキュメント上の記述の問題のみ）

### [INFO-02] US-04 受入条件 `Color.toRgb()` がAPI設計に不在

requirements.md の US-04 受入条件に `Color.toRgb()` が記載されているが、screen_flow.md のAPI定義には `toRgb()` メソッドが存在しない。`Color` クラスは `red`, `green`, `blue` プロパティを個別に持つため機能的には同等だが、受入条件との名称不一致がある。`toRgb(): Triple<Int, Int, Int>` のようなコンビニエンスメソッドを追加するか、受入条件の記述を更新すべき。

**重要度**: 低（機能的には個別プロパティでカバー、受入条件の文言調整で解決）

### [INFO-03] US-07 受入条件の `Harmonize.harmonize()` と実API `Blend.harmonize()` の命名差異

requirements.md の US-07 受入条件では `Harmonize.harmonize()` だが、API設計では `Blend.harmonize()` となっている。`Blend` オブジェクトにはハーモニゼーション以外のブレンド関数 (`hctHue`, `cam16Ucs`) も含まれるため、`Blend` への統合は合理的。受入条件の記述を更新すればよい。

**重要度**: 極低（設計判断として合理的、受入条件の文言更新のみ）

### [INFO-04] US-01 受入条件の `KolorSeed.from()` と実API `CorePalette.fromSeed()` の差異

requirements.md の US-01 受入条件では `KolorSeed.from("#6750A4")` でCorePaletteが返るとあるが、API設計では `CorePalette.fromSeed(Color.fromHex("#6750A4"))` または `CorePalette.fromHex("#6750A4")` となっている。`CorePalette.fromHex()` がコンビニエンスメソッドとして提供されており、ワンステップでの生成は可能。`KolorSeed` というファサードクラスは設けない判断がされている。

**重要度**: 極低（`CorePalette.fromHex()` で受入条件の意図は満たされる、受入条件の文言更新のみ）

## 判定

**PASS**

設計品質は非常に高い。純粋関数・不変オブジェクト・外部依存ゼロという制約の中で、M3カラーパレット生成に必要な全APIを網羅的に設計している。`@JvmInline value class` によるパフォーマンス最適化、`private constructor` + ファクトリによる不変条件の強制、`internal/` による実装隠蔽、KMP全ターゲット対応を考慮したプラットフォーム非依存設計など、Kotlinライブラリとしてのベストプラクティスが全面的に適用されている。指摘事項は全て極低〜低重要度で、requirements.md の受入条件文言とAPI設計間の軽微な命名差異にとどまる。Phase 3以降で受入条件を更新すれば解消する。
