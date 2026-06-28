package io.github.nunoikeno.kolorseed

import kotlin.math.roundToInt

/**
 * A color represented in HSL (Hue-Saturation-Lightness).
 *
 * ```kotlin
 * val hsl = HslColor.from(h = 270.0, s = 0.5, l = 0.48)
 * val color = hsl.toColor()
 * ```
 */
public data class HslColor(
    /** Hue in degrees, 0.0..360.0. */
    val hue: Double,
    /** Saturation, 0.0..1.0. */
    val saturation: Double,
    /** Lightness, 0.0..1.0. */
    val lightness: Double
) {
    /** Convert to [Color]. */
    public fun toColor(): Color {
        val h = hue / 360.0
        val s = saturation
        val l = lightness

        if (s == 0.0) {
            val gray = (l * 255.0).roundToInt().coerceIn(0, 255)
            return Color.fromRgb(gray, gray, gray)
        }

        val q = if (l < 0.5) l * (1.0 + s) else l + s - l * s
        val p = 2.0 * l - q

        val r = (hueToRgb(p, q, h + 1.0 / 3.0) * 255.0).roundToInt().coerceIn(0, 255)
        val g = (hueToRgb(p, q, h) * 255.0).roundToInt().coerceIn(0, 255)
        val b = (hueToRgb(p, q, h - 1.0 / 3.0) * 255.0).roundToInt().coerceIn(0, 255)

        return Color.fromRgb(r, g, b)
    }

    public companion object {
        /**
         * Create from HSL components.
         * @param h Hue 0.0..360.0
         * @param s Saturation 0.0..1.0
         * @param l Lightness 0.0..1.0
         */
        public fun from(h: Double, s: Double, l: Double): HslColor {
            return HslColor(
                hue = (h % 360.0).let { if (it < 0.0) it + 360.0 else it },
                saturation = s.coerceIn(0.0, 1.0),
                lightness = l.coerceIn(0.0, 1.0)
            )
        }

        /** Create from an existing [Color]. */
        public fun fromColor(color: Color): HslColor {
            val r = color.red / 255.0
            val g = color.green / 255.0
            val b = color.blue / 255.0

            val max = maxOf(r, g, b)
            val min = minOf(r, g, b)
            val l = (max + min) / 2.0

            if (max == min) {
                return HslColor(0.0, 0.0, l)
            }

            val d = max - min
            val s = if (l > 0.5) d / (2.0 - max - min) else d / (max + min)

            val h = when (max) {
                r -> ((g - b) / d + (if (g < b) 6.0 else 0.0)) * 60.0
                g -> ((b - r) / d + 2.0) * 60.0
                else -> ((r - g) / d + 4.0) * 60.0
            }

            return HslColor(h, s, l)
        }
    }
}

private fun hueToRgb(p: Double, q: Double, t: Double): Double {
    var tt = t
    if (tt < 0.0) tt += 1.0
    if (tt > 1.0) tt -= 1.0
    return when {
        tt < 1.0 / 6.0 -> p + (q - p) * 6.0 * tt
        tt < 1.0 / 2.0 -> q
        tt < 2.0 / 3.0 -> p + (q - p) * (2.0 / 3.0 - tt) * 6.0
        else -> p
    }
}
