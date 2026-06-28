# changelog

## 2026-06-28 — Phase 2: Public API Surface Design (screen_flow.md)

Designer Agent が `_design/screen_flow.md` を新規作成。

### 記載内容
1. **Module Structure**: core (KMP) + compose (拡張) の 2 モジュール構成。`internal/` で実装隠蔽
2. **Public API Signatures**: Color, Hct, HslColor, TonalPalette, CorePalette, ExtendedCorePalette, CustomColorGroup, CustomColorRoles, ExtendedColorScheme, ColorScheme, Blend, Contrast の全 12 型
3. **Compose Extensions**: `toComposeColor()`, `toKolorSeed()`, `toM3ColorScheme()`
4. **Utility Functions**: `lightColorScheme()`, `darkColorScheme()`, `colorSchemes()` トップレベル便利関数
5. **Usage Examples**: 5 つの完全コード例
6. **Traceability Matrix**: US-01..US-13 全ストーリーと API の対応を検証

### 設計上の主要判断
- `Color` は `@JvmInline value class` (ボクシング回避)
- `Hct` は `private constructor` + ファクトリ (ソルバー検証強制)
- `ColorScheme` は `data class` (35 プロパティ、M3 全ロール網羅)
- カスタムカラーは `ExtendedCorePalette` / `ExtendedColorScheme` で分離 (基本 API の複雑化回避)
- シリアライズは手書きパーサー (外部依存ゼロ制約)

## 2026-06-28 — Phase 1-R レビュー指摘修正 (requirements.md)

レビュー判定 FAIL → 3件の指摘を修正し再レビュー要求。

### P1: US-06 CorePalette グループ数の矛盾 (重大)
- **修正箇所**: US-06 の So that 文
- **変更内容**: 「標準5グループ」→「標準6グループ（Primary / Secondary / Tertiary / Neutral / NeutralVariant / Error）」に修正。M-03・用語集と整合。

### P2: セルフチェック MUST-US マッピング不正確 (軽微)
- **修正箇所**: セクション8 セルフチェック 2項目目
- **変更内容**: 冗長な「US-01〜05, US-04 に対応」→ M-01〜M-08 と US-01〜US-05 の具体的な対応関係を明記。

### P3: `Color` 型が未定義 (軽微)
- **修正箇所**: セクション7 用語集
- **変更内容**: 「Color」の定義を追加。ライブラリ独自の不変データクラス（ARGB 32bit Int ラップ）であること、Compose Color 変換は別アーティファクトで提供する旨を記載。用語数 14 → 15。
