package io.github.nunoikeno.kolorseed

/**
 * A [CorePalette] extended with one or more custom color groups.
 *
 * ```kotlin
 * val palette = CorePalette.fromHex("#6750A4")
 * val extended = palette
 *     .withCustomColor("brand", Color.fromHex("#2E7D32"))
 *     .withCustomColor("warning", Color.fromHex("#F57C00"), harmonize = false)
 * val light = extended.lightScheme()
 * ```
 */
public class ExtendedCorePalette(
    /** The base 6-group palette. */
    public val base: CorePalette,
    /** Custom color groups, keyed by name. */
    public val customColors: Map<String, CustomColorGroup>
) {
    /**
     * Add another custom color.
     */
    public fun withCustomColor(
        name: String,
        color: Color,
        harmonize: Boolean = true
    ): ExtendedCorePalette {
        val harmonizedColor = if (harmonize) {
            Blend.harmonize(color, base.primary.tone(40))
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
            base = base,
            customColors = customColors + (name to group)
        )
    }

    /**
     * Generate a light [ColorScheme] with custom color roles.
     */
    public fun lightScheme(): ExtendedColorScheme = ExtendedColorScheme(
        scheme = base.lightScheme(),
        customColors = customColors.mapValues { it.value.light }
    )

    /**
     * Generate a dark [ColorScheme] with custom color roles.
     */
    public fun darkScheme(): ExtendedColorScheme = ExtendedColorScheme(
        scheme = base.darkScheme(),
        customColors = customColors.mapValues { it.value.dark }
    )
}

/**
 * A custom color group with its tonal palette and derived roles.
 */
public data class CustomColorGroup(
    val name: String,
    val color: Color,
    val harmonizedColor: Color,
    val palette: TonalPalette,
    /** The 4 standard roles derived from this custom color for light theme. */
    val light: CustomColorRoles,
    /** The 4 standard roles derived from this custom color for dark theme. */
    val dark: CustomColorRoles
)

/**
 * The four color roles for a custom color (parallel to primary/onPrimary/etc.).
 */
public data class CustomColorRoles(
    val color: Color,
    val onColor: Color,
    val colorContainer: Color,
    val onColorContainer: Color
)

/**
 * A [ColorScheme] extended with custom color roles.
 */
public data class ExtendedColorScheme(
    val scheme: ColorScheme,
    val customColors: Map<String, CustomColorRoles>
)
