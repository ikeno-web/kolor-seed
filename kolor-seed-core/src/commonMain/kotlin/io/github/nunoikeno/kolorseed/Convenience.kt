package io.github.nunoikeno.kolorseed

/**
 * Top-level convenience: generate a light color scheme from a HEX seed.
 * Equivalent to: `ColorScheme.light(Color.fromHex(hex))`
 *
 * ```kotlin
 * val light = lightColorScheme("#6750A4")
 * ```
 */
public fun lightColorScheme(seedHex: String): ColorScheme =
    ColorScheme.light(Color.fromHex(seedHex))

/**
 * Top-level convenience: generate a dark color scheme from a HEX seed.
 * Equivalent to: `ColorScheme.dark(Color.fromHex(hex))`
 *
 * ```kotlin
 * val dark = darkColorScheme("#6750A4")
 * ```
 */
public fun darkColorScheme(seedHex: String): ColorScheme =
    ColorScheme.dark(Color.fromHex(seedHex))

/**
 * Top-level convenience: generate both light and dark schemes.
 *
 * ```kotlin
 * val (light, dark) = colorSchemes("#6750A4")
 * ```
 *
 * @return Pair(light, dark)
 */
public fun colorSchemes(seedHex: String): Pair<ColorScheme, ColorScheme> {
    val seed = Color.fromHex(seedHex)
    val palette = CorePalette.fromSeed(seed)
    return Pair(palette.lightScheme(), palette.darkScheme())
}
