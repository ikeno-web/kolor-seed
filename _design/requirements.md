# 要件定義書: kolor-seed

## 1. アプリ概要

kolor-seed は、単一のシードカラーから Material 3 カラーパレット全体を生成する Kotlin Multiplatform ライブラリである。
Google の material-color-utilities と同等のパレット生成を、HCT (Hue-Chroma-Tone) 色空間の純粋 Kotlin 実装により実現する。
外部依存ゼロ・純粋関数のみで構成し、JVM / JS / Native すべてのKMPターゲットで動作する。
Compose および Compose Multiplatform プロジェクトへの組み込みを主な利用シーンとし、拡張アーティファクトとして Compose Color 連携を提供する。

## 2. ターゲットユーザーペルソナ

### ペルソナ A: Android / Compose 開発者（経験3年）
- **状況**: Material 3 のダイナミックテーマを実装したいが、Google の material-color-utilities は JAR が大きく、Android 専用 API に依存しており KMP プロジェクトで使えない
- **ニーズ**: 1色を渡すだけで light/dark 両方の ColorScheme を取得したい。既存の Compose プロジェクトに最小の変更で導入したい
- **技術力**: Kotlin 中級、Compose 経験あり、色空間の理論には詳しくない

### ペルソナ B: KMP ライブラリ開発者（経験5年）
- **状況**: iOS / Desktop / Web 横断のデザインシステムを構築中。プラットフォーム非依存のパレット生成が必要
- **ニーズ**: HCT 色空間を直接操作してカスタムパレットを作りたい。CI でパレットを生成して Design Token として出力したい
- **技術力**: Kotlin 上級、色空間理論の基礎知識あり

## 3. ユーザーストーリー

### US-01: シードカラーからパレット生成
- **As a** Compose 開発者
- **I want to** HEX カラーコード 1 つからMaterial 3 の全パレットを生成したい
- **So that** アプリのテーマカラーを簡単に決められる
- **受入条件**: `KolorSeed.from("#6750A4")` で CorePalette が返る

### US-02: Tonal Palette の取得
- **As a** デザインシステム開発者
- **I want to** 任意の色からトーン 0〜100 の TonalPalette を生成したい
- **So that** デザイントークンとして各トーン値を利用できる
- **受入条件**: TonalPalette から tone(0), tone(10), ..., tone(100) の 13 段階が取得できる

### US-03: ライト/ダークスキーム生成
- **As a** アプリ開発者
- **I want to** シードカラーからライトテーマとダークテーマ両方の ColorScheme を一括生成したい
- **So that** テーマ切り替えを正確な色で実装できる
- **受入条件**: `CorePalette.lightScheme()` / `CorePalette.darkScheme()` でMaterial 3仕様準拠の29色ロールが返る

### US-04: HEX / RGB 変換
- **As a** 開発者
- **I want to** HEX文字列・RGB int・ARGB int 間で相互変換したい
- **So that** 既存コードや外部ツールとの連携が容易になる
- **受入条件**: `Color.toHex()`, `Color.fromHex()`, `Color.toRgb()`, `Color.fromArgb()` が利用可能

### US-05: HCT 色空間の直接操作
- **As a** 上級開発者
- **I want to** HCT (Hue, Chroma, Tone) の各パラメータを個別に調整したい
- **So that** 微調整されたカスタムパレットを作成できる
- **受入条件**: `Hct.from(hue, chroma, tone)` でHCTオブジェクトが生成でき、各成分を変更して新しい色を得られる

### US-06: カスタムカラーロール
- **As a** ブランドデザイナー
- **I want to** プライマリ以外にブランド固有のカスタムカラーロールを追加したい
- **So that** Material 3 の標準6グループ（Primary / Secondary / Tertiary / Neutral / NeutralVariant / Error）以外の色もパレットに含められる
- **受入条件**: `CorePalette.withCustomColor(name, color)` でカスタムロールを追加した拡張パレットが得られる

### US-07: カラーハーモニゼーション
- **As a** デザイナー
- **I want to** 任意の色をシードカラーに調和（ハーモナイズ）させたい
- **So that** ブランドカラーがテーマ全体と視覚的に馴染む
- **受入条件**: `Harmonize.harmonize(designColor, seedColor)` でHue が寄せられた色が返る

### US-08: コントラスト比チェック
- **As a** アクセシビリティ重視の開発者
- **I want to** 2色間のWCAGコントラスト比を計算したい
- **So that** テキストと背景の可読性を担保できる
- **受入条件**: `Contrast.ratioOf(color1, color2)` が WCAG 2.1 準拠の比率を返し、AA/AAA 判定メソッドもある

### US-09: Compose Color 拡張（別アーティファクト）
- **As a** Compose 開発者
- **I want to** `androidx.compose.ui.graphics.Color` との相互変換を型安全に行いたい
- **So that** kolor-seed の結果をそのまま Compose テーマに適用できる
- **受入条件**: `kolor-seed-compose` アーティファクトで `Color.toComposeColor()` / `ComposeColor.toKolorSeed()` が利用可能

### US-10: パレットのシリアライズ
- **As a** ツール開発者
- **I want to** 生成したパレットを JSON 文字列として出力したい
- **So that** デザインツールや CI パイプラインと連携できる
- **受入条件**: `ColorScheme.toJson()` / `ColorScheme.fromJson()` で可逆な JSON 変換ができる（kotlinx.serialization 不要、手書きパーサー）

### US-11: テーマエクスポート
- **As a** フロントエンド開発者
- **I want to** CSS カスタムプロパティとしてパレットをエクスポートしたい
- **So that** Web プロジェクトでも同じパレットを利用できる
- **受入条件**: `ColorScheme.toCssCustomProperties()` で `--md-sys-color-primary: #xxx;` 形式の文字列が返る

### US-12: HSL 変換
- **As a** 開発者
- **I want to** HSL (Hue-Saturation-Lightness) との相互変換をしたい
- **So that** HSL ベースのデザインツールとの連携が容易になる
- **受入条件**: `Color.toHsl()`, `Color.fromHsl()` が利用可能

### US-13: Figma トークンエクスポート
- **As a** デザインシステム管理者
- **I want to** Figma Tokens 形式（JSON）でパレットを出力したい
- **So that** デザイナーと開発者でトークンを共有できる
- **受入条件**: `ColorScheme.toFigmaTokens()` で Figma Variables 互換の JSON が返る

## 4. 機能一覧 (MoSCoW)

### Must Have（必須）
| ID | 機能 | 説明 |
|---|---|---|
| M-01 | HCT 色空間 | Hue-Chroma-Tone の完全な Kotlin 実装。ARGB ↔ HCT 双方向変換 |
| M-02 | TonalPalette | 任意の HCT 色からトーン 0〜100 の連続パレット生成 |
| M-03 | CorePalette | シードカラーから Primary / Secondary / Tertiary / Neutral / NeutralVariant / Error の 6 TonalPalette 生成 |
| M-04 | Light ColorScheme | CorePalette から Material 3 仕様準拠のライトテーマ 29 色ロール生成 |
| M-05 | Dark ColorScheme | CorePalette から Material 3 仕様準拠のダークテーマ 29 色ロール生成 |
| M-06 | HEX 変換 | `#RRGGBB` / `#AARRGGBB` 文字列 ↔ Int 変換 |
| M-07 | RGB 変換 | R, G, B 各 0-255 ↔ ARGB Int 変換 |
| M-08 | ARGB ↔ sRGB 線形化 | 正確な sRGB → Linear RGB → XYZ 変換パイプライン |

### Should Have（推奨）
| ID | 機能 | 説明 |
|---|---|---|
| S-01 | カスタムカラーロール | ユーザー定義の追加カラーグループ |
| S-02 | カラーハーモニゼーション | 任意の色をシード色に HCT Hue 軸で調和 |
| S-03 | コントラスト比計算 | WCAG 2.1 準拠のコントラスト比算出 + AA/AAA 判定 |
| S-04 | Compose Color 拡張 | 別アーティファクト `kolor-seed-compose` で Compose Color 相互変換 |
| S-05 | HSL 変換 | HSL ↔ ARGB 変換 |

### Could Have（あれば嬉しい）
| ID | 機能 | 説明 |
|---|---|---|
| C-01 | CSS カスタムプロパティ出力 | ColorScheme を CSS 変数として文字列出力 |
| C-02 | Figma トークン出力 | Figma Variables 互換 JSON 出力 |
| C-03 | JSON シリアライズ | ColorScheme の JSON 入出力（外部依存なし） |
| C-04 | CLI ツール | パレットをターミナルで可視化するユーティリティ |

### Won't Have（対象外）
| ID | 機能 | 理由 |
|---|---|---|
| W-01 | UI コンポーネント | ライブラリのスコープ外。下流の責務 |
| W-02 | Compose Theme ビルダー | MaterialTheme 直接統合は消費者側で行う |
| W-03 | カラーピッカーウィジェット | UI ライブラリではない |
| W-04 | Wallpaper / Dynamic Color 取得 | OS 依存。Android の DynamicColors API は消費者側で呼ぶ |

## 5. 非機能要件

### パフォーマンス
- パレット生成（シード → CorePalette → light/dark ColorScheme）: < 1ms（JVM, Warm）
- TonalPalette 単体生成: < 100μs
- ホットパスでの不要なオブジェクト生成を抑制（TonalPalette のトーンキャッシュ等）

### サイズ
- コアモジュールのバンドルサイズ: < 50KB（minified, JVM JAR）
- Compose 拡張モジュール: < 10KB

### 互換性
- Kotlin: 1.9+（K2 compiler 対応）
- KMP ターゲット: JVM (17+), JS (IR backend), Native (macOS x64/arm64, iOS x64/arm64/simulatorArm64, Linux x64, mingwX64)
- Compose Multiplatform: 1.6+（compose 拡張モジュールのみ）

### 品質
- テストカバレッジ: 95% 以上（コア数学関数は 100%）
- material-color-utilities (Java) との出力一致率: 99.9%（丸め誤差 ±1/255 許容）
- 全 public API に KDoc
- Detekt / ktlint による静的解析ゼロ違反

### CI / CD
- GitHub Actions: lint → test → publish (snapshot)
- タグプッシュで Maven Central release
- API 互換性チェック（binary compatibility validator）

## 6. スコープ外の明示

以下は本ライブラリのスコープに含めない:

- **UI レイヤー**: Compose の `MaterialTheme`、`ColorScheme` への直接バインディング（消費者側の責務）
- **OS 固有カラー取得**: Android の `DynamicColors`、iOS の `UIColor.systemBackground` 等
- **画像からのカラー抽出**: QuantizerCelebi / Score アルゴリズム（別ライブラリの責務）
- **アニメーション**: カラートランジション、パレットモーフィング
- **永続化**: SharedPreferences / DataStore 等への保存ヘルパー
- **ネットワーク**: パレット共有、リモートテーマ取得

## 7. 用語集

| 用語 | 定義 |
|---|---|
| **Color** | ライブラリ独自の不変データクラス。ARGB 32bit Int をラップし、HCT変換・HEX文字列化等のユーティリティを提供する。Compose Color への変換拡張関数は別アーティファクト (kolor-seed-compose) で提供 |
| **HCT** | Hue-Chroma-Tone。Google が Material 3 用に設計した知覚均等色空間。CIE CAM16 の色相・彩度と CIE L* の明度を組み合わせたもの |
| **Hue（色相）** | 色の種類。0〜360 の角度で表現。0=赤、120=緑、240=青 |
| **Chroma（彩度）** | 色の鮮やかさ。0（無彩色）から最大値は色相・トーンに依存 |
| **Tone（トーン）** | 知覚的な明るさ。0（黒）〜100（白）。CIE L* と同等 |
| **TonalPalette** | 同一の Hue/Chroma を持ち Tone のみが異なる色の系列。通常 13 段階（0, 10, 20, ..., 90, 95, 99, 100） |
| **CorePalette** | シードカラーから導出される 6 つの TonalPalette のセット: Primary, Secondary, Tertiary, Neutral, NeutralVariant, Error |
| **ColorScheme** | Material 3 デザインシステムで定義される 29 の色ロール（primary, onPrimary, primaryContainer 等）の具体値セット |
| **Seed Color（シードカラー）** | パレット生成の起点となる 1 色。ユーザーが指定するブランドカラーやアクセントカラー |
| **Color Role（カラーロール）** | Material 3 で定義される色の役割名。primary, secondary, surface, error 等 |
| **ARGB** | Alpha-Red-Green-Blue。32bit 整数で色を表現する形式 (0xAARRGGBB) |
| **sRGB** | 標準 RGB 色空間。ディスプレイの標準的な色域 |
| **CIE XYZ** | CIE が定義した3刺激値による色空間。色変換の中間表現として使用 |
| **CIE CAM16** | CIE Color Appearance Model 2016。環境条件を考慮した色の見え方のモデル |
| **Harmonize（調和）** | 任意の色の Hue を、シードカラーの Hue に近づけて視覚的な統一感を出す処理 |
| **WCAG** | Web Content Accessibility Guidelines。テキストと背景のコントラスト比の基準を定める |

## 8. セルフチェック

- [ ] 全ユーザーストーリーに受入条件があるか → YES
- [ ] MoSCoW の Must がユーザーストーリーをカバーしているか → YES（M-01〜M-03 → US-01・US-02、M-04・M-05 → US-03、M-06・M-07 → US-04、M-08 → US-01・US-05 の変換基盤）
- [ ] 非機能要件に数値目標があるか → YES（1ms, 50KB, 95%, 99.9%）
- [ ] スコープ外が明示されているか → YES（セクション6）
- [ ] 用語集にドメイン固有用語が網羅されているか → YES（15用語）
- [ ] 制約リストとの整合性 → YES（constraints.md と矛盾なし）
- [ ] ペルソナがユーザーストーリーの主語と対応しているか → YES
