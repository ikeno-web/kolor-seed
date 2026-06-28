package io.github.nunoikeno.kolorseed

import io.github.nunoikeno.kolorseed.internal.Cam16
import io.github.nunoikeno.kolorseed.internal.ColorUtils
import io.github.nunoikeno.kolorseed.internal.HctSolver

/**
 * A color represented in the HCT (Hue-Chroma-Tone) color space.
 *
 * HCT combines the hue and chroma of CIE CAM16 with the lightness (L*)
 * of CIELAB, providing perceptually uniform color manipulation.
 * This is the color space used by Material 3 for palette generation.
 *
 * Instances are created via [from] or [fromColor]. Direct construction
 * is intentionally prohibited to enforce the solver's validation.
 *
 * ```kotlin
 * val hct = Hct.from(hue = 270.0, chroma = 48.0, tone = 50.0)
 * val color = hct.toColor()
 * ```
 */
public class Hct private constructor(
    /** Hue in degrees, 0.0..360.0 (exclusive). */
    public val hue: Double,
    /** Chroma (colorfulness), >= 0.0. Maximum depends on hue and tone. */
    public val chroma: Double,
    /** Tone (perceptual lightness), 0.0..100.0. */
    public val tone: Double,
    /** Pre-computed ARGB for this HCT triplet. */
    private val argb: Int
) {

    /** Convert to [Color]. */
    public fun toColor(): Color = Color(argb)

    /**
     * Create a new Hct with the specified [hue], keeping chroma and tone.
     * @param hue 0.0..360.0
     */
    public fun withHue(hue: Double): Hct = from(hue, chroma, tone)

    /**
     * Create a new Hct with the specified [chroma], keeping hue and tone.
     * Chroma is clamped to the maximum achievable for the given hue and tone.
     */
    public fun withChroma(chroma: Double): Hct = from(hue, chroma, tone)

    /**
     * Create a new Hct with the specified [tone], keeping hue and chroma.
     * @param tone 0.0..100.0
     */
    public fun withTone(tone: Double): Hct = from(hue, chroma, tone)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Hct) return false
        return argb == other.argb
    }

    override fun hashCode(): Int = argb

    override fun toString(): String = "Hct(h=$hue, c=$chroma, t=$tone)"

    public companion object {
        /**
         * Create an HCT color from the given components.
         * The solver finds the closest ARGB color that matches these perceptual attributes.
         *
         * @param hue Hue in degrees, 0.0..360.0
         * @param chroma Chroma (colorfulness), >= 0.0
         * @param tone Tone (perceptual lightness), 0.0..100.0
         */
        public fun from(hue: Double, chroma: Double, tone: Double): Hct {
            val argb = HctSolver.solveToInt(hue, chroma, tone)
            return fromArgb(argb)
        }

        /** Create from an existing [Color]. */
        public fun fromColor(color: Color): Hct = fromArgb(color.argb)

        /** Create from an ARGB integer. */
        public fun fromArgb(argb: Int): Hct {
            val cam = Cam16.fromArgb(argb)
            val tone = ColorUtils.lstarFromArgb(argb)
            return Hct(
                hue = cam.hue,
                chroma = cam.chroma,
                tone = tone,
                argb = argb
            )
        }
    }
}
