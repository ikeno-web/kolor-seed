# コードレビュー結果

## 対象
- プロジェクト: kolor-seed
- レビュー日: 2026-06-28
- レビュアー: Reviewer Agent (white-box)
- 対象ファイル: `kolor-seed-core/src/` 配下全 `.kt` ファイル (main 14, test 8)

## チェック項目
- [x] API設計(screen_flow.md)との一致
- [x] constraints.md違反がないか
- [x] セキュリティ（入力検証、エラーハンドリング）
- [x] 命名規則の一貫性
- [x] テストの網羅性
- [x] パフォーマンス懸念
- [x] 数学的正確性（HCTソルバー、sRGBガマットクランプ、色空間変換）
- [x] 数値安定性（tone 0/100、chroma 0、hue 0/360 境界値）

---

## 指摘事項

### [CRITICAL] TonalPalette.cache が非スレッドセーフ — constraints.md 必須要件違反

- ファイル: `D:\アプリ開発\kolor-seed\kolor-seed-core\src\commonMain\kotlin\io\github\nunoikeno\kolorseed\TonalPalette.kt`
- 行: 23
- 内容: `private val cache = mutableMapOf<Int, Color>()` は `HashMap` であり、複数スレッドからの並行アクセスでレースコンディションが発生する。constraints.md は「[必須] 全関数がスレッドセーフ」と明記している。JVM 上で `ConcurrentModificationException` またはデータ破損が発生しうる。
- 提案: 以下のいずれかで対処:
  1. `expect/actual` で JVM は `ConcurrentHashMap`、Native は通常 `HashMap` (シングルスレッド) とする
  2. コンストラクタ時に全 101 トーンを事前計算して不変 `IntArray(101)` に格納 (不変 = 自動的にスレッドセーフ)
  3. キャッシュを排除し毎回 `Hct.from()` を呼ぶ (101 トーンで十分高速)

### [CRITICAL] ColorScheme.fromJson() が NotImplementedError を投げる

- ファイル: `D:\アプリ開発\kolor-seed\kolor-seed-core\src\commonMain\kotlin\io\github\nunoikeno\kolorseed\ColorScheme.kt`
- 行: 188
- 内容: `fromJson()` は `NotImplementedError` を投げており、screen_flow.md (US-10) で定義されたパブリック API が未実装。KDoc には `@throws IllegalArgumentException` と記載されているが、実際には `NotImplementedError` が投げられるため API 契約違反。公開ライブラリとして常にクラッシュする public 関数を含むのは DX 上問題。
- 提案: 35 キーの flat JSON 構造なので手書きパーサーは ~50 行で実装可能。実装するか、公開 API から除外する。

---

### [HIGH] HctSolver の sRGB ガマットクランプが暗黙的

- ファイル: `D:\アプリ開発\kolor-seed\kolor-seed-core\src\commonMain\kotlin\io\github\nunoikeno\kolorseed\internal\HctSolver.kt`
- 行: 135-137
- 内容: `delinearized()` は内部で `coerceIn(0, 255)` しているが、ソルバーのバイセクション中にガマット外の linear RGB 値が発生した場合、クランプ後の ARGB から計算した `lstarFromArgb` は本来の意図したトーンとずれる。バイセクションの収束判定はクランプ後の L* で判定しているため、ガマット境界付近で誤った J に収束する可能性がある。
- 実際の影響: `HctTest.round_trip_gamut_boundary_colors` で赤/緑のトレランスを 12 まで広げていることがこの問題を示唆。
- 提案: バイセクション中にガマット外検出を行い、クロマを削減するフォールバックを導入する。material-color-utilities ではクロマ二分探索を併用している。

### [HIGH] HctSolver バイセクション 40 回は過剰

- ファイル: `D:\アプリ開発\kolor-seed\kolor-seed-core\src\commonMain\kotlin\io\github\nunoikeno\kolorseed\internal\HctSolver.kt`
- 行: 79
- 内容: J の範囲 [0, 100] を 40 回二分すると精度は `100 / 2^40 ~ 9.1e-11` となり、L* の収束閾値 0.2 に対して著しく過剰。20 回で `100 / 2^20 ~ 9.5e-5` で十分。ループ内で `Cam16.fromArgb()` (行 145) を呼んでおり、早期リターンしない場合は高コスト。
- 提案: ループ回数を 20 に削減する。

### [HIGH] Blend.kt が internal パッケージの型に FQN でアクセス

- ファイル: `D:\アプリ開発\kolor-seed\kolor-seed-core\src\commonMain\kotlin\io\github\nunoikeno\kolorseed\Blend.kt`
- 行: 56, 77-81
- 内容: `io.github.nunoikeno.kolorseed.internal.ColorUtils.lstarFromArgb(from.argb)` 等を完全修飾名で呼んでおり可読性が低い。
- 提案: ファイル先頭の import 文に `ColorUtils`, `HctSolver` を追加する。

---

### [MEDIUM] HslColor.from() で負の hue がサニタイズされない

- ファイル: `D:\アプリ開発\kolor-seed\kolor-seed-core\src\commonMain\kotlin\io\github\nunoikeno\kolorseed\HslColor.kt`
- 行: 50
- 内容: `hue = h % 360.0` は負の入力 (例: `-30.0`) で `-30.0` を返す。KDoc は "0.0..360.0" と記載しているため契約違反。テスト `from_clamps_values` (行 72-76) は `400.0 % 360.0 = 40.0` (正) なのでたまたまパスしている。
- 提案: `hue = ((h % 360.0) + 360.0) % 360.0` または `MathUtils.sanitizeDegreesDouble(h)` を使用する。

### [MEDIUM] Contrast.linearize() と ColorUtils.linearizedWcag() の重複

- ファイル: `D:\アプリ開発\kolor-seed\kolor-seed-core\src\commonMain\kotlin\io\github\nunoikeno\kolorseed\Contrast.kt`
- 行: 87-93
- 内容: 同じ sRGB -> linear 変換が `Contrast.linearize()` と `ColorUtils.linearizedWcag()` に二重実装されている。`Contrast.linearize` は `exp(exponent * ln(base))` で pow を計算、`ColorUtils.linearizedWcag` は `kotlin.math.pow` を使用。保守時に片方だけ修正される二重管理リスク。
- 提案: `Contrast.linearize` を `ColorUtils.linearizedWcag` に委譲する。

### [MEDIUM] ColorUtils.linearized() の sRGB 閾値の意図がコメントで明記されていない

- ファイル: `D:\アプリ開発\kolor-seed\kolor-seed-core\src\commonMain\kotlin\io\github\nunoikeno\kolorseed\internal\ColorUtils.kt`
- 行: 53, 153
- 内容: `linearized()` では閾値 `0.040449936` (IEC 61966-2-1 exact)、`linearizedWcag()` では `0.04045` (WCAG 2.1 spec)。用途で閾値が異なることは正しいがコメントがない。
- 提案: 各関数に閾値の出典コメントを追加する。

### [MEDIUM] MathUtils.matrixMultiply が未使用 (デッドコード)

- ファイル: `D:\アプリ開発\kolor-seed\kolor-seed-core\src\commonMain\kotlin\io\github\nunoikeno\kolorseed\internal\MathUtils.kt`
- 行: 51
- 内容: `matrixMultiply` はどこからも呼ばれていない。`ColorUtils.xyzFromArgb` はインライン計算。引数名 `row` も紛らわしい。
- 提案: 削除するか、使用箇所を作る。

### [MEDIUM] screen_flow.md に定義された ColorSchemeSerializer.kt / ColorSchemeExporter.kt が不在

- ファイル: screen_flow.md セクション 1 Module Structure
- 内容: screen_flow.md では `ColorSchemeSerializer.kt` と `ColorSchemeExporter.kt` が独立ファイルとして記載されているが、実際には `ColorScheme.kt` 内にインラインで実装。設計ドキュメントと実装の乖離。
- 提案: screen_flow.md を更新して現実の構造に合わせる。

---

### [LOW] CorePalette.fromSeed の tertiary hue で sanitize 未使用

- ファイル: `D:\アプリ開発\kolor-seed\kolor-seed-core\src\commonMain\kotlin\io\github\nunoikeno\kolorseed\CorePalette.kt`
- 行: 100
- 内容: `(hue + 60.0) % 360.0` は動作するが、他の箇所では `MathUtils.sanitizeDegreesDouble()` を使用。一貫性の欠如。
- 提案: `MathUtils.sanitizeDegreesDouble(hue + 60.0)` に統一する。

### [LOW] Cam16 / HctSolver の pow ヘルパーが重複

- ファイル: `D:\アプリ開発\kolor-seed\kolor-seed-core\src\commonMain\kotlin\io\github\nunoikeno\kolorseed\internal\Cam16.kt` (行 167), `HctSolver.kt` (行 179)
- 内容: `private fun pow(base: Double, exp: Double)` が 2 箇所に同一ロジックで定義。
- 提案: `MathUtils` に `safePow()` として統合する。

### [LOW] Cam16 の数学的係数にコメントがない

- ファイル: `D:\アプリ開発\kolor-seed\kolor-seed-core\src\commonMain\kotlin\io\github\nunoikeno\kolorseed\internal\Cam16.kt`
- 行: 75-79
- 内容: opponent color dimension の係数 (`-12.0`, `11.0`, `9.0`, `20.0`, `21.0` 等) の出典が不明。
- 提案: `// Eq. (X) from Li et al. 2017` 形式のコメントを追加する。

### [LOW] ViewingConditions.surround の意味がコメント不足

- ファイル: `D:\アプリ開発\kolor-seed\kolor-seed-core\src\commonMain\kotlin\io\github\nunoikeno\kolorseed\internal\Cam16.kt`
- 行: 193-199
- 内容: `surround = 2.0` の意味 (average surround) が説明されていない。CAM16 では `{0 = dark, 1 = dim, 2 = average}`。
- 提案: `@param surround Surround condition: 0.0 = dark, 1.0 = dim, 2.0 = average` を追加する。

---

### [LOW] テスト: HCT ソルバーのエッジケース不足

- ファイル: `D:\アプリ開発\kolor-seed\kolor-seed-core\src\commonTest\kotlin\io\github\nunoikeno\kolorseed\HctTest.kt`
- 内容: 以下がテストされていない:
  1. hue = 0.0 と hue = 360.0 の等価性
  2. 非常に高い chroma (例: 200.0) のクランプ動作
  3. tone の極小値 (0.001) と極大値 (99.999)
  4. NaN / Infinity 入力のハンドリング
- 提案: 上記テストを追加する。特に `Hct.from(Double.NaN, ...)` でクラッシュしないことの確認が重要。

### [LOW] テスト: Blend.cam16Ucs で中間値 (amount = 0.5) のテストがない

- ファイル: `D:\アプリ開発\kolor-seed\kolor-seed-core\src\commonTest\kotlin\io\github\nunoikeno\kolorseed\BlendTest.kt`
- 内容: amount = 0.0 / 1.0 のテストはあるが 0.5 (中間ブレンド) のテストがない。ブレンドの正確性検証には中間値が不可欠。
- 提案: `cam16Ucs_amount_05_is_between` テストを追加する。

### [LOW] テスト: internal パッケージの単体テストが不在

- ファイル: `D:\アプリ開発\kolor-seed\kolor-seed-core\src\commonTest\kotlin\io\github\nunoikeno\kolorseed\` 配下
- 内容: `Cam16.kt`, `HctSolver.kt`, `ColorUtils.kt`, `MathUtils.kt` の単体テストが存在しない。公開 API 経由で間接的にテストされているが、constraints.md の「テストカバレッジ 95% 以上」達成には直接テストが必要。
- 提案: `internal/` 配下のユニットテストを追加する。特に `sanitizeDegreesDouble` の負値、`labF/labInvf` の境界値、`delinearized` の 0/255 クランプ。

---

## 数学的正確性の検証結果

### sRGB <-> Linear RGB 変換
- `ColorUtils.linearized()` / `delinearized()`: IEC 61966-2-1 準拠。閾値 `0.040449936` / `0.0031308` は正確。ガンマ `2.4` / `1/2.4` も正しい。**問題なし**。

### sRGB <-> XYZ 変換
- `SRGB_TO_XYZ` / `XYZ_TO_SRGB` マトリックス: D65 白色点 sRGB の標準値と一致。Y 行 (0.2126, 0.7152, 0.0722) は BT.709 輝度係数と一致。逆行列も正確。**問題なし**。

### L*a*b* 変換
- `labF()` / `labInvf()`: CIE 標準の `e = 216/24389`, `kappa = 24389/27` を使用。正確。**問題なし**。

### CAM16 Forward Transform
- M16 マトリックス (Hunt-Pointer-Estevez adapted): 正確。
- 色順応度 D の計算: 正確。
- 非線形圧縮 `400 * af / (af + 27.13)`: CAM16 仕様通り。
- eccentricity factor: 正確 (`h' = h + 360 if h < 20.14`)。
- CAM16-UCS 座標: `KL=1.0, c1=0.007, c2=0.0228` は標準パラメータ。**問題なし**。

### CAM16 Inverse Transform (fromUcs)
- J recovery: `J = Jstar / (1 - (Jstar - 100) * 0.007)` は `Jstar = (1 + 100*0.007)*J / (1 + 0.007*J)` の正確な逆。**問題なし**。

### HCT Solver
- J バイセクションアプローチ: 概念的に正しい。収束閾値 0.2 (L*) は 8-bit sRGB で十分。
- 逆 M16 マトリックス値: material-color-utilities と一致。
- ガマット境界処理に改善余地あり (上記 HIGH 指摘)。

### M3 トーンマッピング (ColorScheme.kt)

全 35 色ロールのトーン割当を M3 ベースライン仕様と照合:

| Role | Light Tone | Dark Tone | 検証結果 |
|------|-----------|----------|---------|
| primary | P-40 | P-80 | OK |
| onPrimary | P-100 | P-20 | OK |
| primaryContainer | P-90 | P-30 | OK |
| onPrimaryContainer | P-10 | P-90 | OK |
| surface | N-98 | N-6 | OK |
| surfaceDim | N-87 | N-6 | OK |
| surfaceBright | N-98 | N-24 | OK |
| surfaceContainerLowest | N-100 | N-4 | OK |
| surfaceContainerLow | N-96 | N-10 | OK |
| surfaceContainer | N-94 | N-12 | OK |
| surfaceContainerHigh | N-92 | N-17 | OK |
| surfaceContainerHighest | N-90 | N-22 | OK |
| outline | NV-50 | NV-60 | OK |
| outlineVariant | NV-80 | NV-30 | OK |
| inverseSurface | N-20 | N-90 | OK |
| inverseOnSurface | N-95 | N-20 | OK |
| inversePrimary | P-80 | P-40 | OK |
| scrim | N-0 | N-0 | OK |
| shadow | N-0 | N-0 | OK |
| background | N-98 | N-6 | OK (= surface) |

全 35 ロール割当が M3 仕様と一致。

### WCAG Contrast (Contrast.kt)
- 相対輝度: `0.2126*R + 0.7152*G + 0.0722*B` -- WCAG 2.1 準拠。
- コントラスト比: `(L1 + 0.05) / (L2 + 0.05)` -- 正確。
- 閾値: AA >= 4.5, AAA >= 7.0, AA large text >= 3.0 -- 全て正確。**問題なし**。

---

## constraints.md 準拠チェック

| 制約 | 状態 | 備考 |
|---|---|---|
| [必須] Kotlin Multiplatform | OK | commonMain/commonTest 構成 |
| [必須] 純粋関数のみ | OK | 全関数が参照透過 (キャッシュは副作用だが出力不変) |
| [必須] Android依存なし | OK | `android.*` import なし |
| [禁止] 外部依存ゼロ | OK | kotlin-stdlib + kotlin.test のみ |
| [禁止] mutableなグローバル状態 | OK | object は全て stateless |
| [必須] HCT色空間の正確な実装 | OK (条件付き) | ガマット境界精度に改善余地 |
| [必須] 全関数がスレッドセーフ | **NG** | TonalPalette.cache が非スレッドセーフ |
| [必須] inline関数の積極活用 | OK | Color の val プロパティが inline |
| [禁止] リフレクション使用 | OK | 不使用 |
| [必須] テストカバレッジ 95%以上 | 未計測 | internal/ の単体テストなし。間接テストではカバレッジ不足の可能性 |
| [必須] KDoc 全public API | OK | 全 public 関数に KDoc あり |

---

## テストカバレッジ概要

| ファイル | テスト数 | カバレッジ | 備考 |
|---------|---------|----------|------|
| Color.kt | 12 | High | fromRgb, fromHex (3/6/8 digit), toHex, round-trip, edge cases |
| Hct.kt | 14 | High | Forward (R/G/B/M3/BW), solver, withXxx, round-trip, equals |
| TonalPalette.kt | 9 | Good | 極値, 順序, キャッシュ, fromColor, M3 参照値 |
| CorePalette.kt | 17 | High | 6 パレット全パラメータ, M3 参照色, スキーム生成 |
| ColorScheme.kt | 14 | Good | Light/Dark ロール検証, シリアライズ形式, コンテナ階層 |
| Blend.kt | 9 | Good | Harmonize (方向/上限/トーン保持), hctHue, cam16Ucs |
| Contrast.kt | 11 | Good | 既知比率, 対称性, WCAG レベル, findToneForContrast |
| HslColor.kt | 8 | Good | 原色, 無彩色, round-trip, クランプ |
| CustomColor.kt | 8 | Good | 作成, ハーモニゼーション, チェイン, Light/Dark ロール |
| **合計** | **102** | | |

---

## 総評

kolor-seed の core 実装は、HCT 色空間の数学的基盤、M3 パレット生成ロジック、API 設計の全てにおいて高品質である。material-color-utilities からの移植は忠実で、CAM16 forward/inverse 変換、L*a*b* 変換、色順応モデルの係数は全て数学的に正確。

**主要な懸念は 2 点**:

1. **TonalPalette のキャッシュが非スレッドセーフ** -- constraints.md の必須要件違反。マルチスレッド環境で ConcurrentModificationException またはデータ破損が発生しうる。

2. **ColorScheme.fromJson() が NotImplementedError** -- 公開 API として定義されながら常にクラッシュする関数が含まれている。

上記 2 点を除けば、コードは well-structured で、命名は Kotlin 慣例に従い、KDoc は全 public API に付与されている。テストは 102 ケースで主要な公開 API を網羅しているが、internal パッケージの単体テストと数値エッジケースの追加が望ましい。

---

## 判定

**FAIL**

### FAIL 理由
1. **[CRITICAL] TonalPalette.cache が非スレッドセーフ** -- constraints.md「[必須] 全関数がスレッドセーフ」違反
2. **[CRITICAL] ColorScheme.fromJson() が NotImplementedError** -- screen_flow.md で公開 API として定義されているが未実装 (常にクラッシュ)

### PASS 条件
1. TonalPalette のキャッシュをスレッドセーフにする (expect/actual による ConcurrentHashMap、または事前計算 IntArray)
2. `fromJson()` を実装するか、公開 API から除外する (companion object から削除し screen_flow.md を更新)
3. [MEDIUM] HslColor.from() の負 hue サニタイズ修正
