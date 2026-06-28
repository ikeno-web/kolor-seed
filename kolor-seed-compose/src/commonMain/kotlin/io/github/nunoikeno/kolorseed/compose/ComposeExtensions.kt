package io.github.nunoikeno.kolorseed.compose

import io.github.nunoikeno.kolorseed.Color as KolorSeedColor
import io.github.nunoikeno.kolorseed.ColorScheme as KolorSeedScheme
import androidx.compose.ui.graphics.Color as ComposeColor

/**
 * Convert kolor-seed [KolorSeedColor] to Compose [ComposeColor].
 *
 * ```kotlin
 * val composeColor = Color.fromHex("#6750A4").toComposeColor()
 * ```
 */
public fun KolorSeedColor.toComposeColor(): ComposeColor {
    return ComposeColor(
        red = red / 255f,
        green = green / 255f,
        blue = blue / 255f,
        alpha = alpha / 255f
    )
}

/**
 * Convert Compose [ComposeColor] to kolor-seed [KolorSeedColor].
 *
 * ```kotlin
 * val kolorSeedColor = Color.Red.toKolorSeed()
 * ```
 */
public fun ComposeColor.toKolorSeed(): KolorSeedColor {
    return KolorSeedColor.fromRgb(
        r = (red * 255f).toInt().coerceIn(0, 255),
        g = (green * 255f).toInt().coerceIn(0, 255),
        b = (blue * 255f).toInt().coerceIn(0, 255),
        a = (alpha * 255f).toInt().coerceIn(0, 255)
    )
}

/**
 * Convert a kolor-seed [KolorSeedScheme] to a Compose Material 3 ColorScheme.
 * This allows direct use with `MaterialTheme(colorScheme = ...)`.
 *
 * Requires `androidx.compose.material3:material3` on the classpath.
 * This function creates a ColorScheme using the compose-ui Color type.
 *
 * ```kotlin
 * @Composable
 * fun AppTheme(content: @Composable () -> Unit) {
 *     val scheme = ColorScheme.light(Color.fromHex("#6750A4"))
 *     MaterialTheme(
 *         colorScheme = scheme.toM3ColorScheme(),
 *         content = content
 *     )
 * }
 * ```
 *
 * Note: This returns a data object with all color roles as [ComposeColor].
 * To use with `MaterialTheme`, pass it through
 * `androidx.compose.material3.lightColorScheme(...)` or map fields manually,
 * as the M3 ColorScheme constructor is in the material3 artifact.
 */
public fun KolorSeedScheme.toComposeColorMap(): Map<String, ComposeColor> {
    return mapOf(
        "primary" to primary.toComposeColor(),
        "onPrimary" to onPrimary.toComposeColor(),
        "primaryContainer" to primaryContainer.toComposeColor(),
        "onPrimaryContainer" to onPrimaryContainer.toComposeColor(),
        "secondary" to secondary.toComposeColor(),
        "onSecondary" to onSecondary.toComposeColor(),
        "secondaryContainer" to secondaryContainer.toComposeColor(),
        "onSecondaryContainer" to onSecondaryContainer.toComposeColor(),
        "tertiary" to tertiary.toComposeColor(),
        "onTertiary" to onTertiary.toComposeColor(),
        "tertiaryContainer" to tertiaryContainer.toComposeColor(),
        "onTertiaryContainer" to onTertiaryContainer.toComposeColor(),
        "error" to error.toComposeColor(),
        "onError" to onError.toComposeColor(),
        "errorContainer" to errorContainer.toComposeColor(),
        "onErrorContainer" to onErrorContainer.toComposeColor(),
        "surface" to surface.toComposeColor(),
        "onSurface" to onSurface.toComposeColor(),
        "surfaceVariant" to surfaceVariant.toComposeColor(),
        "onSurfaceVariant" to onSurfaceVariant.toComposeColor(),
        "outline" to outline.toComposeColor(),
        "outlineVariant" to outlineVariant.toComposeColor(),
        "inverseSurface" to inverseSurface.toComposeColor(),
        "inverseOnSurface" to inverseOnSurface.toComposeColor(),
        "inversePrimary" to inversePrimary.toComposeColor(),
        "scrim" to scrim.toComposeColor(),
        "background" to background.toComposeColor(),
        "onBackground" to onBackground.toComposeColor()
    )
}
