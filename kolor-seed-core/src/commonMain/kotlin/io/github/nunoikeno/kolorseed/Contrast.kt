package io.github.nunoikeno.kolorseed

/**
 * WCAG 2.1 contrast ratio calculation and level determination.
 *
 * Provides utilities for evaluating text-background color pairs
 * against accessibility standards.
 *
 * ```kotlin
 * val ratio = Contrast.ratioOf(Color.fromHex("#000000"), Color.fromHex("#FFFFFF"))
 * println(ratio) // 21.0
 * println(Contrast.meetsAAA(Color.fromHex("#000000"), Color.fromHex("#FFFFFF"))) // true
 * ```
 */
public object Contrast {

    /**
     * Calculate the contrast ratio between two colors.
     * @return Ratio >= 1.0 (e.g. 4.5, 7.0). Higher = more contrast.
     */
    public fun ratioOf(color1: Color, color2: Color): Double {
        val lum1 = relativeLuminance(color1)
        val lum2 = relativeLuminance(color2)
        val lighter = maxOf(lum1, lum2)
        val darker = minOf(lum1, lum2)
        return (lighter + 0.05) / (darker + 0.05)
    }

    /**
     * Check if the contrast ratio meets WCAG AA for normal text (>= 4.5:1).
     */
    public fun meetsAA(color1: Color, color2: Color): Boolean =
        ratioOf(color1, color2) >= 4.5

    /**
     * Check if the contrast ratio meets WCAG AAA for normal text (>= 7.0:1).
     */
    public fun meetsAAA(color1: Color, color2: Color): Boolean =
        ratioOf(color1, color2) >= 7.0

    /**
     * Check if the contrast ratio meets WCAG AA for large text (>= 3.0:1).
     */
    public fun meetsAALargeText(color1: Color, color2: Color): Boolean =
        ratioOf(color1, color2) >= 3.0

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
    public fun findToneForContrast(
        palette: TonalPalette,
        background: Color,
        ratio: Double,
        preferLighter: Boolean = false
    ): Int? {
        val range = if (preferLighter) (100 downTo 0) else (0..100)
        for (tone in range) {
            val candidate = palette.tone(tone)
            if (ratioOf(candidate, background) >= ratio) {
                return tone
            }
        }
        return null
    }

    /**
     * Calculate the relative luminance of a color per WCAG 2.1.
     * @return Luminance value between 0.0 (black) and 1.0 (white)
     */
    private fun relativeLuminance(color: Color): Double {
        val r = linearize(color.red / 255.0)
        val g = linearize(color.green / 255.0)
        val b = linearize(color.blue / 255.0)
        return 0.2126 * r + 0.7152 * g + 0.0722 * b
    }

    /**
     * Convert sRGB component to linear RGB.
     */
    private fun linearize(component: Double): Double {
        return if (component <= 0.04045) {
            component / 12.92
        } else {
            pow((component + 0.055) / 1.055, 2.4)
        }
    }

    /** Platform-independent power function using exp/ln. */
    private fun pow(base: Double, exponent: Double): Double =
        kotlin.math.exp(exponent * kotlin.math.ln(base))
}
