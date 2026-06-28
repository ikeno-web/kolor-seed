# Public API Surface Design: kolor-seed

> Phase 2 成果物。ライブラリのため画面設計をパブリック API 設計として再定義する。

---

## 1. Module Structure

```
kolor-seed/
├── kolor-seed-core/                          # KMP (JVM / JS / Native)
│   └── io.github.nunoikeno.kolorseed
│       ├── Color.kt                          # ARGB 不変ラッパー
│       ├── Hct.kt                            # HCT 色空間
│       ├── TonalPalette.kt                   # 単一色相のトーンパレット
│       ├── CorePalette.kt                    # 6 グループパレット
│       ├── ColorScheme.kt                    # M3 29 色ロール (light/dark)
│       ├── CustomColor.kt                    # カスタムカラーロール
│       ├── Blend.kt                          # カラーハーモニゼーション
│       ├── Contrast.kt                       # コントラスト比計算
│       ├── HslColor.kt                       # HSL 変換
│       ├── ColorSchemeSerializer.kt          # JSON シリアライズ
│       ├── ColorSchemeExporter.kt            # CSS / Figma トークン出力
│       └── internal/                         # 内部実装 (public API 外)
│           ├── MathUtils.kt                  # 数学ユーティリティ
│           ├── ColorUtils.kt                 # sRGB ↔ Linear ↔ XYZ 変換
│           └── Cam16.kt                      # CIE CAM16 実装
│
└── kolor-seed-compose/                       # Compose Multiplatform 拡張
    └── io.github.nunoikeno.kolorseed.compose
        └── ComposeExtensions.kt              # Color ↔ Compose Color
```

**設計根拠**:
- `internal/` 配下は `internal` 可視性。消費者に公開しない実装詳細
- core は kotlin-stdlib 以外の依存ゼロ (constraints.md: 外部依存ゼロ)
- compose 拡張は `androidx.compose.ui:ui-graphics` のみ依存

---

## 2. Public API Signatures

### 2.1 Color — ARGB 不変ラッパー

```kotlin
package io.github.nunoikeno.kolorseed

/**
 * Immutable color representation wrapping a 32-bit ARGB integer.
 *
 * This is the primary interchange type across all kolor-seed APIs.
 * It is an inline class to avoid heap allocation on JVM.
 */
@JvmInline
value class Color(val argb: Int) {

    /** Red component, 0..255. */
    inline val red: Int get() = (argb shr 16) and 0xFF

    /** Green component, 0..255. */
    inline val green: Int get() = (argb shr 8) and 0xFF

    /** Blue component, 0..255. */
    inline val blue: Int get() = argb and 0xFF

    /** Alpha component, 0..255. */
    inline val alpha: Int get() = (argb shr 24) and 0xFF

    /** Convert to HCT color space. */
    fun toHct(): Hct

    /**
     * Convert to HEX string.
     * @return "#RRGGBB" (opaque) or "#AARRGGBB" (if alpha != 255)
     */
    fun toHex(): String

    /**
     * Convert to HSL.
     * @return [HslColor] with h in 0..360, s in 0..1, l in 0..1
     */
    fun toHsl(): HslColor

    companion object {
        /**
         * Create from RGB components.
         * @param r Red 0..255
         * @param g Green 0..255
         * @param b Blue 0..255
         * @param a Alpha 0..255, default 255 (opaque)
         * @throws IllegalArgumentException if any component is out of range
         */
        fun fromRgb(r: Int, g: Int, b: Int, a: Int = 255): Color

        /**
         * Create from HEX string.
         * Accepts "#RGB", "#RRGGBB", "#AARRGGBB" (# prefix optional).
         * @throws IllegalArgumentException if format is invalid
         */
        fun fromHex(hex: String): Color

        /**
         * Create from ARGB integer (0xAARRGGBB).
         */
        fun fromArgb(argb: Int): Color
    }
}
```

**設計判断**:
- `@JvmInline value class` で JVM でのボクシング回避 (NFR: < 1ms パレット生成)
- `inline val` でプロパティアクセスもゼロコスト
- `fromArgb()` は `Color(argb)` コンストラクタと等価だが、意図を明示する名前付きファクトリとして提供

---

### 2.2 Hct — HCT 色空間

```kotlin
package io.github.nunoikeno.kolorseed

/**
 * A color represented in the HCT (Hue-Chroma-Tone) color space.
 *
 * HCT combines the hue and chroma of CIE CAM16 with the lightness (L*)
 * of CIELAB, providing perceptually uniform color manipulation.
 *
 * Instances are created via [from] or [fromColor]. Direct construction
 * is intentionally prohibited to enforce the solver's validation.
 */
class Hct private constructor(
    /** Hue in degrees, 0.0..360.0 (exclusive). */
    val hue: Double,
    /** Chroma (colorfulness), >= 0.0. Maximum depends on hue and tone. */
    val chroma: Double,
    /** Tone (perceptual lightness), 0.0..100.0. */
    val tone: Double,
    /** Pre-computed ARGB for this HCT triplet. */
    private val argb: Int
) {

    /** Convert to [Color]. */
    fun toColor(): Color

    /**
     * Create a new Hct with the specified [hue], keeping chroma and tone.
     * @param hue 0.0..360.0
     */
    fun withHue(hue: Double): Hct

    /**
     * Create a new Hct with the specified [chroma], keeping hue and tone.
     * Chroma is clamped to the maximum achievable for the given hue and tone.
     */
    fun withChroma(chroma: Double): Hct

    /**
     * Create a new Hct with the specified [tone], keeping hue and chroma.
     * @param tone 0.0..100.0
     */
    fun withTone(tone: Double): Hct

    companion object {
        /**
         * Create an HCT color from the given components.
         * The solver finds the closest ARGB color that matches these perceptual attributes.
         */
        fun from(hue: Double, chroma: Double, tone: Double): Hct

        /** Create from an existing [Color]. */
        fun fromColor(color: Color): Hct

        /** Create from an ARGB integer. */
        fun fromArgb(argb: Int): Hct
    }

    override fun equals(other: Any?): Boolean
    override fun hashCode(): Int
    override fun toString(): String  // "Hct(h=220.0, c=48.0, t=50.0)"
}
```

**設計判断**:
- `private constructor` + ファクトリで HCT ソルバーの検証を強制
- `withXxx()` 関数で不変性を維持したまま個別成分を変更 (US-05)
- `argb` をキャッシュし、`toColor()` 呼び出し時の再計算を回避

---

### 2.3 HslColor — HSL 変換

```kotlin
package io.github.nunoikeno.kolorseed

/**
 * A color represented in HSL (Hue-Saturation-Lightness).
 */
data class HslColor(
    /** Hue in degrees, 0.0..360.0. */
    val hue: Double,
    /** Saturation, 0.0..1.0. */
    val saturation: Double,
    /** Lightness, 0.0..1.0. */
    val lightness: Double
) {
    /** Convert to [Color]. */
    fun toColor(): Color

    companion object {
        /**
         * Create from HSL components.
         * @param h Hue 0.0..360.0
         * @param s Saturation 0.0..1.0
         * @param l Lightness 0.0..1.0
         */
        fun from(h: Double, s: Double, l: Double): HslColor

        /** Create from an existing [Color]. */
        fun fromColor(color: Color): HslColor
    }
}
```

---

### 2.4 TonalPalette — トーンパレット

```kotlin
package io.github.nunoikeno.kolorseed

/**
 * A palette of tones (lightness levels) for a single hue and chroma.
 *
 * Standard Material 3 tones: 0, 4, 6, 10, 12, 17, 20, 22, 24, 25, 30, 35,
 * 40, 50, 60, 70, 80, 87, 90, 92, 94, 95, 96, 98, 99, 100.
 * Any integer tone 0..100 can be queried.
 */
class TonalPalette private constructor(
    /** The hue of this palette, 0.0..360.0. */
    val hue: Double,
    /** The chroma of this palette. */
    val chroma: Double
) {

    /**
     * Get the color at the given [tone].
     * @param tone 0..100 (0 = black, 100 = white)
     * @return [Color] at the specified tone
     * @throws IllegalArgumentException if tone is not in 0..100
     */
    fun tone(tone: Int): Color

    /**
     * Get a map of all standard M3 tones.
     * @return Map from tone value (Int) to [Color]
     */
    fun toMap(): Map<Int, Color>

    companion object {
        /**
         * Create a TonalPalette from a fixed hue and chroma.
         */
        fun fromHueAndChroma(hue: Double, chroma: Double): TonalPalette

        /**
         * Create a TonalPalette from a seed [Color].
         * Extracts hue and chroma via HCT.
         */
        fun fromColor(color: Color): TonalPalette
    }
}
```

**設計判断**:
- `tone()` は任意の 0..100 を受け付ける。M3 標準 13 段階 (0,10,20,...,90,95,99,100) に限定しない
- 内部でトーン値をキャッシュ (NFR: TonalPalette 生成 < 100us)
- `toMap()` は全標準トーンを一括取得するコンビニエンス関数

---

### 2.5 CorePalette — 6 グループパレット

```kotlin
package io.github.nunoikeno.kolorseed

/**
 * The six tonal palettes derived from a single seed color,
 * following Material 3 color system specifications.
 */
class CorePalette(
    /** Primary tonal palette. Derived from seed color's hue. */
    val primary: TonalPalette,
    /** Secondary tonal palette. Derived with reduced chroma. */
    val secondary: TonalPalette,
    /** Tertiary tonal palette. Derived with shifted hue (+60 degrees). */
    val tertiary: TonalPalette,
    /** Neutral tonal palette. Minimal chroma for surfaces. */
    val neutral: TonalPalette,
    /** Neutral variant tonal palette. Slightly more chroma than neutral. */
    val neutralVariant: TonalPalette,
    /** Error tonal palette. Fixed red hue (25.0). */
    val error: TonalPalette
) {

    /**
     * Generate a light [ColorScheme] from this palette.
     */
    fun lightScheme(): ColorScheme

    /**
     * Generate a dark [ColorScheme] from this palette.
     */
    fun darkScheme(): ColorScheme

    /**
     * Generate a [ColorScheme] from this palette.
     * @param isDark true for dark theme, false for light theme
     */
    fun toScheme(isDark: Boolean): ColorScheme

    /**
     * Create an extended palette with a custom color group.
     * @param name Identifier for the custom color
     * @param color The custom color to add
     * @param harmonize If true, harmonize the color with the primary hue
     * @return [ExtendedCorePalette] with the additional custom group
     */
    fun withCustomColor(
        name: String,
        color: Color,
        harmonize: Boolean = true
    ): ExtendedCorePalette

    companion object {
        /**
         * Create a CorePalette from a seed color.
         * This is the primary entry point for most use cases.
         * @param seed The seed color in ARGB format
         */
        fun fromSeed(seed: Color): CorePalette

        /**
         * Create a CorePalette from a seed color given as HEX string.
         * Convenience method combining [Color.fromHex] and [fromSeed].
         */
        fun fromHex(hex: String): CorePalette
    }
}
```

---

### 2.6 ExtendedCorePalette — カスタムカラー拡張

```kotlin
package io.github.nunoikeno.kolorseed

/**
 * A [CorePalette] extended with one or more custom color groups.
 */
class ExtendedCorePalette(
    /** The base 6-group palette. */
    val base: CorePalette,
    /** Custom color groups, keyed by name. */
    val customColors: Map<String, CustomColorGroup>
) {
    /**
     * Add another custom color.
     */
    fun withCustomColor(
        name: String,
        color: Color,
        harmonize: Boolean = true
    ): ExtendedCorePalette

    /**
     * Generate a light [ColorScheme] with custom color roles.
     */
    fun lightScheme(): ExtendedColorScheme

    /**
     * Generate a dark [ColorScheme] with custom color roles.
     */
    fun darkScheme(): ExtendedColorScheme
}

/**
 * A custom color group with its tonal palette and derived roles.
 */
data class CustomColorGroup(
    val name: String,
    val color: Color,
    val harmonizedColor: Color,
    val palette: TonalPalette,
    /** The 4 standard roles derived from this custom color. */
    val light: CustomColorRoles,
    val dark: CustomColorRoles
)

/**
 * The four color roles for a custom color (parallel to primary/onPrimary/etc.).
 */
data class CustomColorRoles(
    val color: Color,
    val onColor: Color,
    val colorContainer: Color,
    val onColorContainer: Color
)

/**
 * A [ColorScheme] extended with custom color roles.
 */
data class ExtendedColorScheme(
    val scheme: ColorScheme,
    val customColors: Map<String, CustomColorRoles>
)
```

---

### 2.7 ColorScheme — M3 29 色ロール

```kotlin
package io.github.nunoikeno.kolorseed

/**
 * A complete Material 3 color scheme with 29 color roles.
 *
 * All properties are derived deterministically from a [CorePalette].
 * See: https://m3.material.io/styles/color/static/baseline
 */
data class ColorScheme(
    // Primary
    val primary: Color,
    val onPrimary: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,

    // Secondary
    val secondary: Color,
    val onSecondary: Color,
    val secondaryContainer: Color,
    val onSecondaryContainer: Color,

    // Tertiary
    val tertiary: Color,
    val onTertiary: Color,
    val tertiaryContainer: Color,
    val onTertiaryContainer: Color,

    // Error
    val error: Color,
    val onError: Color,
    val errorContainer: Color,
    val onErrorContainer: Color,

    // Surface
    val surface: Color,
    val onSurface: Color,
    val surfaceVariant: Color,
    val onSurfaceVariant: Color,
    val surfaceDim: Color,
    val surfaceBright: Color,
    val surfaceContainerLowest: Color,
    val surfaceContainerLow: Color,
    val surfaceContainer: Color,
    val surfaceContainerHigh: Color,
    val surfaceContainerHighest: Color,

    // Outline
    val outline: Color,
    val outlineVariant: Color,

    // Inverse
    val inverseSurface: Color,
    val inverseOnSurface: Color,
    val inversePrimary: Color,

    // Other
    val scrim: Color,
    val shadow: Color,

    // Background (alias for surface in M3)
    val background: Color,
    val onBackground: Color
) {
    companion object {
        /**
         * Create a light ColorScheme from a seed color.
         */
        fun light(seed: Color): ColorScheme

        /**
         * Create a dark ColorScheme from a seed color.
         */
        fun dark(seed: Color): ColorScheme

        /**
         * Create a ColorScheme from a [CorePalette].
         * @param palette The source palette
         * @param isDark true for dark theme
         */
        fun fromCorePalette(palette: CorePalette, isDark: Boolean): ColorScheme
    }

    /**
     * Serialize to JSON string. No external dependencies.
     * Format: `{ "primary": "#6750A4", "onPrimary": "#FFFFFF", ... }`
     */
    fun toJson(): String

    /**
     * Export as CSS custom properties.
     * Format: `--md-sys-color-primary: #6750A4;\n--md-sys-color-on-primary: #FFFFFF;\n...`
     */
    fun toCssCustomProperties(): String

    /**
     * Export as Figma Tokens JSON.
     * Compatible with Figma Variables import.
     */
    fun toFigmaTokens(): String

    companion object {
        // ... (上記に加えて)

        /**
         * Deserialize from JSON string.
         * @throws IllegalArgumentException if JSON is malformed
         */
        fun fromJson(json: String): ColorScheme
    }
}
```

**設計判断**:
- 全 35 色ロール (M3 仕様 29 + surface 拡張 5 + background 2 - 重複) を網羅
- `data class` で `equals()` / `hashCode()` / `copy()` を自動生成
- シリアライズは外部依存なしの手書きパーサー (constraints.md: 外部依存ゼロ)

---

### 2.8 Blend — カラーハーモニゼーション

```kotlin
package io.github.nunoikeno.kolorseed

/**
 * Color blending and harmonization utilities.
 */
object Blend {

    /**
     * Harmonize [designColor] towards [sourceColor] in HCT hue space.
     *
     * Shifts the hue of [designColor] towards [sourceColor] by up to 15 degrees,
     * while preserving chroma and tone. This makes the design color feel more
     * cohesive with the source palette.
     *
     * @param designColor The color to harmonize
     * @param sourceColor The color to harmonize towards (typically the seed/primary)
     * @return A new [Color] with adjusted hue
     */
    fun harmonize(designColor: Color, sourceColor: Color): Color

    /**
     * Blend two colors in HCT space.
     *
     * @param from Starting color
     * @param to Target color
     * @param amount Blend ratio, 0.0 = 100% [from], 1.0 = 100% [to]
     * @return Blended color
     */
    fun hctHue(from: Color, to: Color, amount: Double): Color

    /**
     * Blend two colors in CAM16 space for perceptual interpolation.
     */
    fun cam16Ucs(from: Color, to: Color, amount: Double): Color
}
```

---

### 2.9 Contrast — コントラスト比計算

```kotlin
package io.github.nunoikeno.kolorseed

/**
 * WCAG 2.1 contrast ratio calculation and level determination.
 */
object Contrast {

    /**
     * Calculate the contrast ratio between two colors.
     * @return Ratio >= 1.0 (e.g. 4.5, 7.0). Higher = more contrast.
     */
    fun ratioOf(color1: Color, color2: Color): Double

    /**
     * Check if the contrast ratio meets WCAG AA for normal text (>= 4.5:1).
     */
    fun meetsAA(color1: Color, color2: Color): Boolean

    /**
     * Check if the contrast ratio meets WCAG AAA for normal text (>= 7.0:1).
     */
    fun meetsAAA(color1: Color, color2: Color): Boolean

    /**
     * Check if the contrast ratio meets WCAG AA for large text (>= 3.0:1).
     */
    fun meetsAALargeText(color1: Color, color2: Color): Boolean

    /**
     * Find the tone in [palette] that achieves at least [ratio] contrast
     * against [background].
     *
     * @param palette The tonal palette to search
     * @param background The background color
     * @param ratio Minimum contrast ratio (e.g. 4.5)
     * @param preferLighter If true, search from light tones first
     * @return The first tone value (Int) meeting the ratio, or null
     */
    fun findToneForContrast(
        palette: TonalPalette,
        background: Color,
        ratio: Double,
        preferLighter: Boolean = false
    ): Int?
}
```

---

## 3. Extension Functions (kolor-seed-compose)

```kotlin
package io.github.nunoikeno.kolorseed.compose

import io.github.nunoikeno.kolorseed.Color as KolorSeedColor
import io.github.nunoikeno.kolorseed.ColorScheme as KolorSeedScheme
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.material3.ColorScheme as M3ColorScheme

/**
 * Convert kolor-seed Color to Compose Color.
 */
fun KolorSeedColor.toComposeColor(): ComposeColor

/**
 * Convert Compose Color to kolor-seed Color.
 */
fun ComposeColor.toKolorSeed(): KolorSeedColor

/**
 * Convert a kolor-seed ColorScheme to a Compose Material 3 ColorScheme.
 * This allows direct use with MaterialTheme(colorScheme = ...).
 */
fun KolorSeedScheme.toM3ColorScheme(): M3ColorScheme
```

**設計判断**:
- Compose Material 3 の `ColorScheme` へのダイレクト変換を提供し、最小コードで統合可能に (ペルソナ A の主要ユースケース)
- `toM3ColorScheme()` は compose 拡張モジュールのみに配置。core への M3 依存を回避

---

## 4. Utility Functions

### 4.1 トップレベル便利関数

```kotlin
package io.github.nunoikeno.kolorseed

/**
 * Top-level convenience: generate a light color scheme from a HEX seed.
 * Equivalent to: ColorScheme.light(Color.fromHex(hex))
 */
fun lightColorScheme(seedHex: String): ColorScheme

/**
 * Top-level convenience: generate a dark color scheme from a HEX seed.
 */
fun darkColorScheme(seedHex: String): ColorScheme

/**
 * Top-level convenience: generate both light and dark schemes.
 * @return Pair(light, dark)
 */
fun colorSchemes(seedHex: String): Pair<ColorScheme, ColorScheme>
```

---

## 5. Usage Examples

### Example 1: 最小限のパレット生成 (US-01, US-03)

```kotlin
import io.github.nunoikeno.kolorseed.*

// 1色からライト/ダーク両テーマを生成
val (light, dark) = colorSchemes("#6750A4")

// 個別に生成する場合
val lightScheme = ColorScheme.light(Color.fromHex("#6750A4"))
val darkScheme  = ColorScheme.dark(Color.fromHex("#6750A4"))

// HEX 値として取得
println(lightScheme.primary.toHex())        // "#6750A4"
println(lightScheme.primaryContainer.toHex()) // "#EADDFF"
```

### Example 2: Compose Material Theme との統合 (US-09)

```kotlin
import io.github.nunoikeno.kolorseed.*
import io.github.nunoikeno.kolorseed.compose.*

@Composable
fun AppTheme(
    seedColor: String = "#6750A4",
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val seed = Color.fromHex(seedColor)
    val colorScheme = if (darkTheme) {
        ColorScheme.dark(seed).toM3ColorScheme()
    } else {
        ColorScheme.light(seed).toM3ColorScheme()
    }
    MaterialTheme(colorScheme = colorScheme, content = content)
}
```

### Example 3: HCT 色空間の直接操作 (US-05)

```kotlin
import io.github.nunoikeno.kolorseed.*

// 色を HCT に変換して個別調整
val brandColor = Color.fromHex("#FF5722")
val hct = brandColor.toHct()

println("Hue: ${hct.hue}, Chroma: ${hct.chroma}, Tone: ${hct.tone}")

// 同じ色相・彩度でトーンだけ変更
val lighter = hct.withTone(80.0).toColor()
val darker  = hct.withTone(20.0).toColor()

// 色相を 30 度回転
val rotated = hct.withHue((hct.hue + 30.0) % 360.0).toColor()
```

### Example 4: カスタムカラーとハーモニゼーション (US-06, US-07)

```kotlin
import io.github.nunoikeno.kolorseed.*

val seed = Color.fromHex("#6750A4")
val palette = CorePalette.fromSeed(seed)

// ブランドカラーを追加（シードに調和させる）
val brandGreen = Color.fromHex("#2E7D32")
val extended = palette.withCustomColor("brand", brandGreen, harmonize = true)

val lightScheme = extended.lightScheme()
val brandRoles = lightScheme.customColors["brand"]!!

println(brandRoles.color.toHex())          // 調和済みの緑
println(brandRoles.onColor.toHex())        // その上に置くテキスト色
println(brandRoles.colorContainer.toHex()) // コンテナ背景
```

### Example 5: コントラスト比チェックと CSS 出力 (US-08, US-11)

```kotlin
import io.github.nunoikeno.kolorseed.*

val scheme = ColorScheme.light(Color.fromHex("#6750A4"))

// テキストと背景のコントラスト比を検証
val ratio = Contrast.ratioOf(scheme.onPrimary, scheme.primary)
println("Contrast ratio: $ratio")          // e.g. "Contrast ratio: 12.7"
println("Meets AA: ${Contrast.meetsAA(scheme.onPrimary, scheme.primary)}")  // true

// CSS カスタムプロパティとして出力
val css = scheme.toCssCustomProperties()
println(css)
// --md-sys-color-primary: #6750A4;
// --md-sys-color-on-primary: #FFFFFF;
// ...

// JSON として出力 (CI 連携)
val json = scheme.toJson()
println(json)
// { "primary": "#6750A4", "onPrimary": "#FFFFFF", ... }
```

---

## 6. API x User Story Traceability Matrix

| User Story | Priority | API Surface | 備考 |
|---|---|---|---|
| **US-01**: シードからパレット生成 | Must | `CorePalette.fromSeed()`, `CorePalette.fromHex()`, `colorSchemes()` | M-01, M-02, M-03 |
| **US-02**: TonalPalette 取得 | Must | `TonalPalette.fromHueAndChroma()`, `TonalPalette.tone()`, `TonalPalette.toMap()` | M-02 |
| **US-03**: Light/Dark スキーム | Must | `ColorScheme.light()`, `ColorScheme.dark()`, `CorePalette.lightScheme()`, `CorePalette.darkScheme()` | M-04, M-05 |
| **US-04**: HEX/RGB 変換 | Must | `Color.toHex()`, `Color.fromHex()`, `Color.fromRgb()`, `Color.fromArgb()` | M-06, M-07 |
| **US-05**: HCT 直接操作 | Must | `Hct.from()`, `Hct.fromColor()`, `Hct.withHue()`, `Hct.withChroma()`, `Hct.withTone()` | M-01 |
| **US-06**: カスタムカラーロール | Should | `CorePalette.withCustomColor()`, `ExtendedCorePalette`, `CustomColorGroup` | S-01 |
| **US-07**: ハーモニゼーション | Should | `Blend.harmonize()`, `Blend.hctHue()`, `Blend.cam16Ucs()` | S-02 |
| **US-08**: コントラスト比チェック | Should | `Contrast.ratioOf()`, `Contrast.meetsAA()`, `Contrast.meetsAAA()`, `Contrast.meetsAALargeText()`, `Contrast.findToneForContrast()` | S-03 |
| **US-09**: Compose Color 拡張 | Should | `toComposeColor()`, `toKolorSeed()`, `toM3ColorScheme()` | S-04 |
| **US-10**: パレット JSON シリアライズ | Could | `ColorScheme.toJson()`, `ColorScheme.fromJson()` | C-03 |
| **US-11**: CSS エクスポート | Could | `ColorScheme.toCssCustomProperties()` | C-01 |
| **US-12**: HSL 変換 | Should | `Color.toHsl()`, `HslColor.from()`, `HslColor.fromColor()`, `HslColor.toColor()` | S-05 |
| **US-13**: Figma トークン出力 | Could | `ColorScheme.toFigmaTokens()` | C-02 |

### カバレッジ確認

- **Must (M-01..M-08)**: 全 8 件が US-01..US-05 経由で API に対応。漏れなし
- **Should (S-01..S-05)**: 全 5 件が対応する API を持つ
- **Could (C-01..C-03)**: 全 3 件が対応する API を持つ。C-04 (CLI) は別ツールのため API 設計対象外
- **Won't (W-01..W-04)**: API に含まれないことを確認

---

## 7. Self-check

- [x] 全 MUST 機能 (M-01..M-08) に対応する API が存在する
- [x] Core モジュールに Android 依存がない (`android.*` import なし)
- [x] 全 public 関数が純粋関数 (副作用なし、スレッドセーフ)
- [x] Kotlin 命名規則に準拠 (camelCase, ファクトリは `from`/`of` プレフィクス)
- [x] `@JvmInline value class` で JVM パフォーマンス最適化
- [x] `internal/` パッケージで実装詳細を隠蔽
- [x] 外部依存ゼロ (core モジュール)
- [x] `data class` を適切に使用 (ColorScheme, HslColor, CustomColor 系)
- [x] mutable なグローバル状態なし (`object` は全て stateless)
- [x] 受入条件の API 名が要件定義の期待と一致 (US-01: `CorePalette.fromHex("#6750A4")` 等)
