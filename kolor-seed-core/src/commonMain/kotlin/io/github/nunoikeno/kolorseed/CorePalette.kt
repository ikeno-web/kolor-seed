package io.github.nunoikeno.kolorseed

/**
 * The six tonal palettes derived from a single seed color,
 * following Material 3 color system specifications.
 *
 * ```kotlin
 * val palette = CorePalette.fromSeed(Color.fromHex("#6750A4"))
 * val lightScheme = palette.lightScheme()
 * val darkScheme = palette.darkScheme()
 * ```
 */
public class CorePalette(
    /** Primary tonal palette. Derived from seed color's hue. */
    public val primary: TonalPalette,
    /** Secondary tonal palette. Derived with reduced chroma. */
    public val secondary: TonalPalette,
    /** Tertiary tonal palette. Derived with shifted hue (+60 degrees). */
    public val tertiary: TonalPalette,
    /** Neutral tonal palette. Minimal chroma for surfaces. */
    public val neutral: TonalPalette,
    /** Neutral variant tonal palette. Slightly more chroma than neutral. */
    public val neutralVariant: TonalPalette,
    /** Error tonal palette. Fixed red hue (25.0). */
    public val error: TonalPalette
) {

    /**
     * Generate a light [ColorScheme] from this palette.
     */
    public fun lightScheme(): ColorScheme = ColorScheme.fromCorePalette(this, isDark = false)

    /**
     * Generate a dark [ColorScheme] from this palette.
     */
    public fun darkScheme(): ColorScheme = ColorScheme.fromCorePalette(this, isDark = true)

    /**
     * Generate a [ColorScheme] from this palette.
     * @param isDark true for dark theme, false for light theme
     */
    public fun toScheme(isDark: Boolean): ColorScheme = ColorScheme.fromCorePalette(this, isDark)

    /**
     * Create an extended palette with a custom color group.
     * @param name Identifier for the custom color
     * @param color The custom color to add
     * @param harmonize If true, harmonize the color with the primary hue
     * @return [ExtendedCorePalette] with the additional custom group
     */
    public fun withCustomColor(
        name: String,
        color: Color,
        harmonize: Boolean = true
    ): ExtendedCorePalette {
        // TODO: Implement in custom color phase
        val harmonizedColor = if (harmonize) {
            Blend.harmonize(color, primary.tone(40))
        } else {
            color
        }
        val palette = TonalPalette.fromColor(harmonizedColor)
        val group = CustomColorGroup(
            name = name,
            color = color,
            harmonizedColor = harmonizedColor,
            palette = palette,
            light = CustomColorRoles(
                color = palette.tone(40),
                onColor = palette.tone(100),
                colorContainer = palette.tone(90),
                onColorContainer = palette.tone(10)
            ),
            dark = CustomColorRoles(
                color = palette.tone(80),
                onColor = palette.tone(20),
                colorContainer = palette.tone(30),
                onColorContainer = palette.tone(90)
            )
        )
        return ExtendedCorePalette(
            base = this,
            customColors = mapOf(name to group)
        )
    }

    public companion object {
        /**
         * Create a CorePalette from a seed color.
         * This is the primary entry point for most use cases.
         * @param seed The seed color
         */
        public fun fromSeed(seed: Color): CorePalette {
            val hct = seed.toHct()
            val hue = hct.hue
            val chroma = hct.chroma
            return CorePalette(
                primary = TonalPalette.fromHueAndChroma(hue, maxOf(chroma, 48.0)),
                secondary = TonalPalette.fromHueAndChroma(hue, 16.0),
                tertiary = TonalPalette.fromHueAndChroma((hue + 60.0) % 360.0, 24.0),
                neutral = TonalPalette.fromHueAndChroma(hue, 4.0),
                neutralVariant = TonalPalette.fromHueAndChroma(hue, 8.0),
                error = TonalPalette.fromHueAndChroma(25.0, 84.0)
            )
        }

        /**
         * Create a CorePalette from a seed color given as HEX string.
         * Convenience method combining [Color.fromHex] and [fromSeed].
         */
        public fun fromHex(hex: String): CorePalette = fromSeed(Color.fromHex(hex))
    }
}
