package io.github.nunoikeno.kolorseed

import io.github.nunoikeno.kolorseed.internal.Cam16
import io.github.nunoikeno.kolorseed.internal.MathUtils
import kotlin.math.sqrt

/**
 * Color blending and harmonization utilities.
 *
 * Provides algorithms for shifting colors to be more visually
 * cohesive with a seed palette, using perceptually uniform color spaces.
 *
 * ```kotlin
 * val brand = Color.fromHex("#2E7D32")
 * val seed = Color.fromHex("#6750A4")
 * val harmonized = Blend.harmonize(brand, seed)
 * ```
 */
public object Blend {

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
    public fun harmonize(designColor: Color, sourceColor: Color): Color {
        val fromHct = designColor.toHct()
        val toHct = sourceColor.toHct()
        val diffDegrees = MathUtils.differenceDegrees(fromHct.hue, toHct.hue)
        val rotationDegrees = minOf(diffDegrees * 0.5, 15.0)
        val outputHue = MathUtils.sanitizeDegreesDouble(
            fromHct.hue + rotationDegrees * MathUtils.rotationDirection(fromHct.hue, toHct.hue)
        )
        return Hct.from(outputHue, fromHct.chroma, fromHct.tone).toColor()
    }

    /**
     * Blend two colors in HCT hue space.
     *
     * @param from Starting color
     * @param to Target color
     * @param amount Blend ratio, 0.0 = 100% [from], 1.0 = 100% [to]
     * @return Blended color
     */
    public fun hctHue(from: Color, to: Color, amount: Double): Color {
        val ucs = cam16Ucs(from, to, amount)
        val ucsCam = Cam16.fromArgb(ucs.argb)
        val fromCam = Cam16.fromArgb(from.argb)
        val blendedHue = ucsCam.hue
        return Hct.from(blendedHue, fromCam.chroma, io.github.nunoikeno.kolorseed.internal.ColorUtils.lstarFromArgb(from.argb)).toColor()
    }

    /**
     * Blend two colors in CAM16 UCS space for perceptual interpolation.
     *
     * @param from Starting color
     * @param to Target color
     * @param amount Blend ratio, 0.0 = 100% [from], 1.0 = 100% [to]
     * @return Blended color
     */
    public fun cam16Ucs(from: Color, to: Color, amount: Double): Color {
        val fromCam = Cam16.fromArgb(from.argb)
        val toCam = Cam16.fromArgb(to.argb)

        val jstar = fromCam.jstar + (toCam.jstar - fromCam.jstar) * amount
        val astar = fromCam.astar + (toCam.astar - fromCam.astar) * amount
        val bstar = fromCam.bstar + (toCam.bstar - fromCam.bstar) * amount

        val blended = Cam16.fromUcs(jstar, astar, bstar)
        // Convert back to ARGB via HCT (to ensure correct L*)
        val argb = io.github.nunoikeno.kolorseed.internal.HctSolver.solveToInt(
            blended.hue, blended.chroma,
            io.github.nunoikeno.kolorseed.internal.ColorUtils.lstarFromArgb(from.argb) +
                (io.github.nunoikeno.kolorseed.internal.ColorUtils.lstarFromArgb(to.argb) -
                    io.github.nunoikeno.kolorseed.internal.ColorUtils.lstarFromArgb(from.argb)) * amount
        )
        return Color(argb)
    }
}
