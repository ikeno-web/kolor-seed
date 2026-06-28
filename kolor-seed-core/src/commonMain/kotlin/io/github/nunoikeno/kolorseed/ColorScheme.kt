package io.github.nunoikeno.kolorseed

/**
 * A complete Material 3 color scheme with all standard color roles.
 *
 * All properties are derived deterministically from a [CorePalette].
 * See: [Material 3 Color System](https://m3.material.io/styles/color/static/baseline)
 *
 * ```kotlin
 * val light = ColorScheme.light(Color.fromHex("#6750A4"))
 * val dark  = ColorScheme.dark(Color.fromHex("#6750A4"))
 * println(light.primary.toHex())
 * ```
 */
public data class ColorScheme(
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

    /**
     * Serialize to JSON string. No external dependencies.
     * Format: `{ "primary": "#6750A4", "onPrimary": "#FFFFFF", ... }`
     */
    public fun toJson(): String {
        // TODO: Implement hand-written JSON serializer in Phase 6-x
        return buildString {
            append("{\n")
            val entries = toRoleMap()
            entries.entries.forEachIndexed { index, (key, value) ->
                append("  \"$key\": \"${value.toHex()}\"")
                if (index < entries.size - 1) append(",")
                append("\n")
            }
            append("}")
        }
    }

    /**
     * Export as CSS custom properties.
     * Format: `--md-sys-color-primary: #6750A4;`
     */
    public fun toCssCustomProperties(): String {
        // TODO: Implement in export phase
        return toRoleMap().entries.joinToString("\n") { (key, value) ->
            "--md-sys-color-${key.camelToKebab()}: ${value.toHex()};"
        }
    }

    /**
     * Export as Figma Tokens JSON.
     * Compatible with Figma Variables import.
     */
    public fun toFigmaTokens(): String {
        // TODO: Implement in export phase
        return buildString {
            append("{\n")
            val entries = toRoleMap()
            entries.entries.forEachIndexed { index, (key, value) ->
                append("  \"$key\": {\n")
                append("    \"value\": \"${value.toHex()}\",\n")
                append("    \"type\": \"color\"\n")
                append("  }")
                if (index < entries.size - 1) append(",")
                append("\n")
            }
            append("}")
        }
    }

    private fun toRoleMap(): Map<String, Color> = linkedMapOf(
        "primary" to primary,
        "onPrimary" to onPrimary,
        "primaryContainer" to primaryContainer,
        "onPrimaryContainer" to onPrimaryContainer,
        "secondary" to secondary,
        "onSecondary" to onSecondary,
        "secondaryContainer" to secondaryContainer,
        "onSecondaryContainer" to onSecondaryContainer,
        "tertiary" to tertiary,
        "onTertiary" to onTertiary,
        "tertiaryContainer" to tertiaryContainer,
        "onTertiaryContainer" to onTertiaryContainer,
        "error" to error,
        "onError" to onError,
        "errorContainer" to errorContainer,
        "onErrorContainer" to onErrorContainer,
        "surface" to surface,
        "onSurface" to onSurface,
        "surfaceVariant" to surfaceVariant,
        "onSurfaceVariant" to onSurfaceVariant,
        "surfaceDim" to surfaceDim,
        "surfaceBright" to surfaceBright,
        "surfaceContainerLowest" to surfaceContainerLowest,
        "surfaceContainerLow" to surfaceContainerLow,
        "surfaceContainer" to surfaceContainer,
        "surfaceContainerHigh" to surfaceContainerHigh,
        "surfaceContainerHighest" to surfaceContainerHighest,
        "outline" to outline,
        "outlineVariant" to outlineVariant,
        "inverseSurface" to inverseSurface,
        "inverseOnSurface" to inverseOnSurface,
        "inversePrimary" to inversePrimary,
        "scrim" to scrim,
        "shadow" to shadow,
        "background" to background,
        "onBackground" to onBackground
    )

    public companion object {
        /**
         * Create a light ColorScheme from a seed color.
         */
        public fun light(seed: Color): ColorScheme =
            fromCorePalette(CorePalette.fromSeed(seed), isDark = false)

        /**
         * Create a dark ColorScheme from a seed color.
         */
        public fun dark(seed: Color): ColorScheme =
            fromCorePalette(CorePalette.fromSeed(seed), isDark = true)

        /**
         * Create a ColorScheme from a [CorePalette].
         * @param palette The source palette
         * @param isDark true for dark theme
         */
        public fun fromCorePalette(palette: CorePalette, isDark: Boolean): ColorScheme {
            return if (isDark) darkFromPalette(palette) else lightFromPalette(palette)
        }

        /**
         * Deserialize from JSON string.
         * @throws IllegalArgumentException if JSON is malformed
         */
        public fun fromJson(json: String): ColorScheme {
            // TODO: Implement hand-written JSON parser in Phase 6-x
            throw NotImplementedError("JSON deserialization will be implemented in a later phase")
        }

        private fun lightFromPalette(p: CorePalette): ColorScheme = ColorScheme(
            primary = p.primary.tone(40),
            onPrimary = p.primary.tone(100),
            primaryContainer = p.primary.tone(90),
            onPrimaryContainer = p.primary.tone(10),
            secondary = p.secondary.tone(40),
            onSecondary = p.secondary.tone(100),
            secondaryContainer = p.secondary.tone(90),
            onSecondaryContainer = p.secondary.tone(10),
            tertiary = p.tertiary.tone(40),
            onTertiary = p.tertiary.tone(100),
            tertiaryContainer = p.tertiary.tone(90),
            onTertiaryContainer = p.tertiary.tone(10),
            error = p.error.tone(40),
            onError = p.error.tone(100),
            errorContainer = p.error.tone(90),
            onErrorContainer = p.error.tone(10),
            surface = p.neutral.tone(98),
            onSurface = p.neutral.tone(10),
            surfaceVariant = p.neutralVariant.tone(90),
            onSurfaceVariant = p.neutralVariant.tone(30),
            surfaceDim = p.neutral.tone(87),
            surfaceBright = p.neutral.tone(98),
            surfaceContainerLowest = p.neutral.tone(100),
            surfaceContainerLow = p.neutral.tone(96),
            surfaceContainer = p.neutral.tone(94),
            surfaceContainerHigh = p.neutral.tone(92),
            surfaceContainerHighest = p.neutral.tone(90),
            outline = p.neutralVariant.tone(50),
            outlineVariant = p.neutralVariant.tone(80),
            inverseSurface = p.neutral.tone(20),
            inverseOnSurface = p.neutral.tone(95),
            inversePrimary = p.primary.tone(80),
            scrim = p.neutral.tone(0),
            shadow = p.neutral.tone(0),
            background = p.neutral.tone(98),
            onBackground = p.neutral.tone(10)
        )

        private fun darkFromPalette(p: CorePalette): ColorScheme = ColorScheme(
            primary = p.primary.tone(80),
            onPrimary = p.primary.tone(20),
            primaryContainer = p.primary.tone(30),
            onPrimaryContainer = p.primary.tone(90),
            secondary = p.secondary.tone(80),
            onSecondary = p.secondary.tone(20),
            secondaryContainer = p.secondary.tone(30),
            onSecondaryContainer = p.secondary.tone(90),
            tertiary = p.tertiary.tone(80),
            onTertiary = p.tertiary.tone(20),
            tertiaryContainer = p.tertiary.tone(30),
            onTertiaryContainer = p.tertiary.tone(90),
            error = p.error.tone(80),
            onError = p.error.tone(20),
            errorContainer = p.error.tone(30),
            onErrorContainer = p.error.tone(90),
            surface = p.neutral.tone(6),
            onSurface = p.neutral.tone(90),
            surfaceVariant = p.neutralVariant.tone(30),
            onSurfaceVariant = p.neutralVariant.tone(80),
            surfaceDim = p.neutral.tone(6),
            surfaceBright = p.neutral.tone(24),
            surfaceContainerLowest = p.neutral.tone(4),
            surfaceContainerLow = p.neutral.tone(10),
            surfaceContainer = p.neutral.tone(12),
            surfaceContainerHigh = p.neutral.tone(17),
            surfaceContainerHighest = p.neutral.tone(22),
            outline = p.neutralVariant.tone(60),
            outlineVariant = p.neutralVariant.tone(30),
            inverseSurface = p.neutral.tone(90),
            inverseOnSurface = p.neutral.tone(20),
            inversePrimary = p.primary.tone(40),
            scrim = p.neutral.tone(0),
            shadow = p.neutral.tone(0),
            background = p.neutral.tone(6),
            onBackground = p.neutral.tone(90)
        )
    }
}

private fun String.camelToKebab(): String = buildString {
    this@camelToKebab.forEach { c ->
        if (c.isUpperCase()) {
            append('-')
            append(c.lowercaseChar())
        } else {
            append(c)
        }
    }
}
