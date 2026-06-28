package io.github.nunoikeno.kolorseed.internal

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cbrt
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.sign
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * CAM16 color appearance model.
 *
 * Implements the forward and inverse transforms of CIECAM16
 * as used by Material Design's HCT color system.
 *
 * Reference: Li et al., "Comprehensive color solutions: CAM16, CAT16, and S-decoupling" (2017).
 */
internal class Cam16 private constructor(
    /** Hue in degrees, 0..360. */
    val hue: Double,
    /** Chroma. */
    val chroma: Double,
    /** Lightness (J). */
    val j: Double,
    /** Colorfulness (M). */
    val m: Double,
    /** Saturation (s). */
    val s: Double,
    /** Brightness (Q). */
    val q: Double,
    // CAM16-UCS coordinates
    val jstar: Double,
    val astar: Double,
    val bstar: Double
) {
    companion object {
        /**
         * Forward transform: ARGB -> CAM16 under default viewing conditions.
         */
        fun fromArgb(argb: Int): Cam16 {
            return fromArgbInViewingConditions(argb, ViewingConditions.DEFAULT)
        }

        fun fromArgbInViewingConditions(argb: Int, vc: ViewingConditions): Cam16 {
            val red = ColorUtils.linearized(ColorUtils.redFromArgb(argb))
            val green = ColorUtils.linearized(ColorUtils.greenFromArgb(argb))
            val blue = ColorUtils.linearized(ColorUtils.blueFromArgb(argb))

            // XYZ from linearRGB
            val x = 0.41233895 * red + 0.35762064 * green + 0.18051042 * blue
            val y = 0.2126 * red + 0.7152 * green + 0.0722 * blue
            val z = 0.01932141 * red + 0.11916382 * green + 0.95034478 * blue

            // Convert XYZ to sharpened RGB via M16 (Hunt-Pointer-Estevez adapted)
            val rC = 0.401288 * x + 0.650173 * y - 0.051461 * z
            val gC = -0.250268 * x + 1.204414 * y + 0.045854 * z
            val bC = -0.002079 * x + 0.048952 * y + 0.953127 * z

            // Chromatic adaptation (degree of adaptation D applied to white)
            val rD = vc.rgbD[0] * rC
            val gD = vc.rgbD[1] * gC
            val bD = vc.rgbD[2] * bC

            // Post-adaptation nonlinear compression
            val rAF = adaptedComponent(rD, vc.fl)
            val gAF = adaptedComponent(gD, vc.fl)
            val bAF = adaptedComponent(bD, vc.fl)

            // Opponent color dimensions
            val a = rAF + (-12.0 * gAF + bAF) / 11.0
            val b = (rAF + gAF - 2.0 * bAF) / 9.0

            val u = (20.0 * rAF + 20.0 * gAF + 21.0 * bAF) / 20.0
            val p2 = (40.0 * rAF + 20.0 * gAF + bAF) / 20.0

            val atan2Val = atan2(b, a)
            val atanDegrees = atan2Val * 180.0 / PI
            val hue = when {
                atanDegrees < 0 -> atanDegrees + 360.0
                atanDegrees >= 360.0 -> atanDegrees - 360.0
                else -> atanDegrees
            }
            val hueRadians = hue * PI / 180.0

            // Achromatic response
            val ac = p2 * vc.nbb

            // Lightness J
            val jVal = 100.0 * pow(ac / vc.aw, vc.c * vc.z)

            // Brightness Q
            val qVal = (4.0 / vc.c) * sqrt(jVal / 100.0) * (vc.aw + 4.0) * vc.fLRoot

            // Eccentricity factor for hue
            val huePrime = if (hue < 20.14) hue + 360.0 else hue
            val eHue = 0.25 * (cos(huePrime * PI / 180.0 + 2.0) + 3.8)
            val p1 = 50000.0 / 13.0 * eHue * vc.nc * vc.ncb
            val t = p1 * sqrt(a * a + b * b) / (u + 0.305)
            val alpha = if (t < 0.0 || t.isNaN()) {
                0.0
            } else {
                pow(t, 0.9) * pow(1.64 - pow(0.29, vc.n), 0.73)
            }

            // Chroma C, Colorfulness M, Saturation s
            val cVal = alpha * sqrt(jVal / 100.0)
            val mVal = cVal * vc.fLRoot
            val sVal = 50.0 * sqrt((alpha * vc.c) / (vc.aw + 4.0))

            // CAM16-UCS coordinates
            val jstar = (1.0 + 100.0 * 0.007) * jVal / (1.0 + 0.007 * jVal)
            val mstar = 1.0 / 0.0228 * ln(1.0 + 0.0228 * mVal)
            val astar = mstar * cos(hueRadians)
            val bstar = mstar * sin(hueRadians)

            return Cam16(hue, cVal, jVal, mVal, sVal, qVal, jstar, astar, bstar)
        }

        /**
         * Create from CAM16-UCS coordinates (Jstar, astar, bstar).
         */
        fun fromUcs(jstar: Double, astar: Double, bstar: Double): Cam16 {
            return fromUcsInViewingConditions(jstar, astar, bstar, ViewingConditions.DEFAULT)
        }

        fun fromUcsInViewingConditions(
            jstar: Double, astar: Double, bstar: Double,
            vc: ViewingConditions
        ): Cam16 {
            val m = sqrt(astar * astar + bstar * bstar)
            val mVal = (exp(m * 0.0228) - 1.0) / 0.0228
            val c = mVal / vc.fLRoot
            var h = atan2(bstar, astar) * (180.0 / PI)
            if (h < 0.0) h += 360.0
            val j = jstar / (1.0 - (jstar - 100.0) * 0.007)
            val q = (4.0 / vc.c) * sqrt(j / 100.0) * (vc.aw + 4.0) * vc.fLRoot
            val alpha = if (j == 0.0) 0.0 else c / sqrt(j / 100.0)
            val s = 50.0 * sqrt((alpha * vc.c) / (vc.aw + 4.0))
            return Cam16(h, c, j, mVal, s, q, jstar, astar, bstar)
        }

        /**
         * Nonlinear post-adaptation compression.
         */
        private fun adaptedComponent(component: Double, fl: Double): Double {
            val af = pow(fl * abs(component) / 100.0, 0.42)
            return sign(component) * 400.0 * af / (af + 27.13)
        }

        /**
         * Inverse of the post-adaptation compression.
         */
        internal fun inverseAdaptedComponent(adapted: Double, fl: Double): Double {
            val adaptedAbs = abs(adapted)
            val base = maxOf(0.0, 27.13 * adaptedAbs / (400.0 - adaptedAbs))
            return sign(adapted) * (100.0 / fl) * pow(base, 1.0 / 0.42)
        }

        /**
         * Platform-independent power function.
         */
        private fun pow(base: Double, exp: Double): Double {
            if (base <= 0.0) return 0.0
            return base.pow(exp)
        }
    }
}

/**
 * Pre-computed viewing conditions for CAM16.
 *
 * Default conditions match material-color-utilities default:
 * sRGB display, average surround, ~200 cd/m2.
 */
internal class ViewingConditions private constructor(
    val n: Double,
    val aw: Double,
    val nbb: Double,
    val ncb: Double,
    val c: Double,
    val nc: Double,
    val rgbD: DoubleArray,
    val fl: Double,
    val fLRoot: Double,
    val z: Double
) {
    companion object {
        val DEFAULT: ViewingConditions = make(
            whitePoint = ColorUtils.WHITE_POINT_D65,
            adaptingLuminance = (200.0 / PI) * ColorUtils.yFromLstar(50.0) / 100.0,
            backgroundLstar = 50.0,
            surround = 2.0,
            discountingIlluminant = false
        )

        fun make(
            whitePoint: DoubleArray,
            adaptingLuminance: Double,
            backgroundLstar: Double,
            surround: Double,
            discountingIlluminant: Boolean
        ): ViewingConditions {
            val c = when {
                surround >= 1.0 -> MathUtils.lerp(0.59, 0.69, (surround - 1.0).coerceIn(0.0, 1.0))
                else -> MathUtils.lerp(0.525, 0.59, surround.coerceIn(0.0, 1.0))
            }
            val nc = when {
                surround >= 1.0 -> MathUtils.lerp(0.9, 1.0, (surround - 1.0).coerceIn(0.0, 1.0))
                else -> MathUtils.lerp(0.8, 0.9, surround.coerceIn(0.0, 1.0))
            }

            // M16 * whitePoint
            val rW = 0.401288 * whitePoint[0] + 0.650173 * whitePoint[1] - 0.051461 * whitePoint[2]
            val gW = -0.250268 * whitePoint[0] + 1.204414 * whitePoint[1] + 0.045854 * whitePoint[2]
            val bW = -0.002079 * whitePoint[0] + 0.048952 * whitePoint[1] + 0.953127 * whitePoint[2]

            val f = 0.8 + surround / 10.0
            val d = if (discountingIlluminant) {
                1.0
            } else {
                f * (1.0 - (1.0 / 3.6) * exp((-adaptingLuminance - 42.0) / 92.0))
            }.coerceIn(0.0, 1.0)

            val rgbD = doubleArrayOf(
                d * (100.0 / rW) + 1.0 - d,
                d * (100.0 / gW) + 1.0 - d,
                d * (100.0 / bW) + 1.0 - d
            )

            val k = 1.0 / (5.0 * adaptingLuminance + 1.0)
            val k4 = k * k * k * k
            val k4F = 1.0 - k4
            val fl = k4 * adaptingLuminance + 0.1 * k4F * k4F * cbrt(5.0 * adaptingLuminance)
            val fLRoot = fl.pow(0.25)

            val n = ColorUtils.yFromLstar(backgroundLstar) / whitePoint[1]
            val z = 1.48 + sqrt(n)
            val nbb = 0.725 / n.pow(0.2)
            val ncb = nbb

            // Adapted white point
            val rAW = adaptedComponentStatic(rgbD[0] * rW, fl)
            val gAW = adaptedComponentStatic(rgbD[1] * gW, fl)
            val bAW = adaptedComponentStatic(rgbD[2] * bW, fl)

            val aw = (2.0 * rAW + gAW + 0.05 * bAW - 0.305) * nbb

            return ViewingConditions(n, aw, nbb, ncb, c, nc, rgbD, fl, fLRoot, z)
        }

        private fun adaptedComponentStatic(component: Double, fl: Double): Double {
            val af = (fl * abs(component) / 100.0).pow(0.42)
            return sign(component) * 400.0 * af / (af + 27.13)
        }
    }
}
