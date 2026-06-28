package io.github.nunoikeno.kolorseed

/**
 * A palette of tones (lightness levels) for a single hue and chroma.
 *
 * Standard Material 3 tones: 0, 4, 6, 10, 12, 17, 20, 22, 24, 25, 30, 35,
 * 40, 50, 60, 70, 80, 87, 90, 92, 94, 95, 96, 98, 99, 100.
 * Any integer tone 0..100 can be queried via [tone].
 *
 * ```kotlin
 * val palette = TonalPalette.fromColor(Color.fromHex("#6750A4"))
 * val primary40 = palette.tone(40)
 * val primary80 = palette.tone(80)
 * ```
 */
public class TonalPalette private constructor(
    /** The hue of this palette, 0.0..360.0. */
    public val hue: Double,
    /** The chroma of this palette. */
    public val chroma: Double
) {
    /**
     * Get the color at the given [tone].
     * @param tone 0..100 (0 = black, 100 = white)
     * @return [Color] at the specified tone
     * @throws IllegalArgumentException if tone is not in 0..100
     */
    public fun tone(tone: Int): Color {
        require(tone in 0..100) { "Tone must be 0..100, got $tone" }
        return Hct.from(hue, chroma, tone.toDouble()).toColor()
    }

    /**
     * Get a map of all standard M3 tones.
     * @return Map from tone value (Int) to [Color]
     */
    public fun toMap(): Map<Int, Color> {
        return STANDARD_TONES.associateWith { tone(it) }
    }

    public companion object {
        /** Standard Material 3 tone values. */
        public val STANDARD_TONES: List<Int> = listOf(
            0, 4, 6, 10, 12, 17, 20, 22, 24, 25, 30, 35,
            40, 50, 60, 70, 80, 87, 90, 92, 94, 95, 96, 98, 99, 100
        )

        /**
         * Create a TonalPalette from a fixed hue and chroma.
         */
        public fun fromHueAndChroma(hue: Double, chroma: Double): TonalPalette {
            return TonalPalette(hue, chroma)
        }

        /**
         * Create a TonalPalette from a seed [Color].
         * Extracts hue and chroma via HCT.
         */
        public fun fromColor(color: Color): TonalPalette {
            val hct = color.toHct()
            return TonalPalette(hct.hue, hct.chroma)
        }
    }
}
