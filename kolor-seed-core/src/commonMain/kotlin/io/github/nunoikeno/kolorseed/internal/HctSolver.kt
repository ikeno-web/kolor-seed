package io.github.nunoikeno.kolorseed.internal

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cbrt
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sign
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * HCT Solver: finds the ARGB color for given Hue, Chroma, and Tone (L*).
 *
 * Ported from material-color-utilities.
 *
 * The key insight: Tone = L* (CIELAB lightness). Given a target tone,
 * we know Y. We then need to find an sRGB color with that Y whose
 * CAM16 hue and chroma match the targets.
 *
 * Strategy:
 * 1. For achromatic colors (chroma ~0), use L* -> gray directly.
 * 2. For chromatic colors, use CAM16 inverse with iterative J refinement
 *    to match the target L*.
 */
internal object HctSolver {

    /**
     * Solve for the ARGB color matching (hue, chroma, tone).
     */
    fun solveToInt(hueDegrees: Double, chroma: Double, tone: Double): Int {
        if (tone < 0.0001) return 0xFF000000.toInt()
        if (tone > 99.9999) return 0xFFFFFFFF.toInt()
        if (chroma < 0.0001) return ColorUtils.argbFromLstar(tone)

        val hue = MathUtils.sanitizeDegreesDouble(hueDegrees)
        val targetChroma = chroma.coerceAtLeast(0.0)

        // Approach: do CAM16 inverse at the target hue and chroma,
        // iteratively finding the J value that produces the target L*.
        return solveByBisectingJ(hue, targetChroma, tone)
    }

    /**
     * Binary search on J (CAM16 lightness) to find an ARGB color
     * whose L* matches the target tone, at the given hue and chroma.
     *
     * If the target chroma exceeds the maximum achievable in sRGB,
     * the result will have the maximum achievable chroma (colors are
     * clamped to gamut).
     */
    private fun solveByBisectingJ(hue: Double, chroma: Double, tone: Double): Int {
        // Step 1: Find J that gives the target L* for an achromatic color.
        // Under default viewing conditions, J and L* are close but not identical.
        // We use the relationship: for achromatic, L* is determined by Y, and
        // J = 100 * (A/Aw)^(c*z) where A is the achromatic response.

        // Quick estimate: J is very close to L* under default VC
        var jLow = 0.0
        var jHigh = 100.0
        var jMid: Double
        var bestArgb = ColorUtils.argbFromLstar(tone)
        var bestDeltaE = Double.MAX_VALUE

        // Pre-compute the viewing conditions values we need for the inverse transform
        val vc = ViewingConditions.DEFAULT
        val hueRadians = hue * PI / 180.0

        // Eccentricity factor
        val huePrime = if (hue < 20.14) hue + 360.0 else hue
        val eHue = 0.25 * (cos(huePrime * PI / 180.0 + 2.0) + 3.8)
        val p1 = 50000.0 / 13.0 * eHue * vc.nc * vc.ncb

        val hSin = sin(hueRadians)
        val hCos = cos(hueRadians)

        for (round in 0 until 40) {
            jMid = (jLow + jHigh) / 2.0

            // Compute alpha from J and chroma: C = alpha * sqrt(J/100)
            val sqrtJ = sqrt(jMid / 100.0)
            if (sqrtJ < 0.0001) {
                jLow = jMid
                continue
            }
            val alpha = chroma / sqrtJ

            // Compute t from alpha (inverse of alpha = t^0.9 * (1.64 - 0.29^n)^0.73)
            val factor = pow164(vc.n)
            val t = if (alpha <= 0.0) 0.0 else {
                val alphaOverFactor = alpha / factor
                pow(alphaOverFactor, 1.0 / 0.9)
            }

            // Achromatic response from J
            val ac = vc.aw * pow(jMid / 100.0, 1.0 / (vc.c * vc.z))
            val p2 = ac / vc.nbb

            // Compute a, b from t, p2, hue
            val gamma = if (t == 0.0) 0.0 else {
                23.0 * (p2 + 0.305) * t / (23.0 * p1 + 11.0 * t * hCos + 108.0 * t * hSin)
            }

            val a = gamma * hCos
            val b = gamma * hSin

            // Compute adapted RGB from a, b, p2
            val rA = (460.0 * p2 + 451.0 * a + 288.0 * b) / 1403.0
            val gA = (460.0 * p2 - 891.0 * a - 261.0 * b) / 1403.0
            val bA = (460.0 * p2 - 220.0 * a - 6300.0 * b) / 1403.0

            // Inverse nonlinear compression
            val rCScaled = Cam16.inverseAdaptedComponent(rA, vc.fl)
            val gCScaled = Cam16.inverseAdaptedComponent(gA, vc.fl)
            val bCScaled = Cam16.inverseAdaptedComponent(bA, vc.fl)

            // Undo chromatic adaptation
            val rF = rCScaled / vc.rgbD[0]
            val gF = gCScaled / vc.rgbD[1]
            val bF = bCScaled / vc.rgbD[2]

            // Inverse M16 to XYZ
            val x = 1.8620678 * rF - 1.0112547 * gF + 0.14918678 * bF
            val y = 0.38752654 * rF + 0.62144744 * gF - 0.00897398 * bF
            val z = -0.01584150 * rF - 0.03412294 * gF + 1.0499644 * bF

            // XYZ to linear sRGB
            val linR = 3.2413774792388685 * x + -1.5376652402851851 * y + -0.49885366846268053 * z
            val linG = -0.9691452513005321 * x + 1.8758853451067872 * y + 0.04156585616912061 * z
            val linB = 0.05562093689691305 * x + -0.20395524564742123 * y + 1.0571799111220335 * z

            // Clamp to sRGB gamut
            val rInt = ColorUtils.delinearized(linR)
            val gInt = ColorUtils.delinearized(linG)
            val bInt = ColorUtils.delinearized(linB)

            val argb = ColorUtils.argbFromRgb(rInt, gInt, bInt)
            val actualLstar = ColorUtils.lstarFromArgb(argb)

            val deltaLstar = actualLstar - tone
            if (abs(deltaLstar) < 0.2) {
                // Check if hue is also close enough
                val cam = Cam16.fromArgb(argb)
                val hueDiff = MathUtils.differenceDegrees(hue, cam.hue)
                if (hueDiff < 2.0 || chroma < 2.0) {
                    return argb
                }
            }

            if (deltaLstar < 0.0) {
                jLow = jMid
            } else {
                jHigh = jMid
            }

            // Track best result
            val deltaE = abs(deltaLstar)
            if (deltaE < bestDeltaE) {
                bestDeltaE = deltaE
                bestArgb = argb
            }
        }

        return bestArgb
    }

    /**
     * Compute (1.64 - 0.29^n)^0.73
     */
    private fun pow164(n: Double): Double {
        return pow(1.64 - pow(0.29, n), 0.73)
    }

    /**
     * Safe power function for non-negative base.
     */
    private fun pow(base: Double, exp: Double): Double {
        if (base <= 0.0) return 0.0
        return base.pow(exp)
    }
}
