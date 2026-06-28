package io.github.nunoikeno.kolorseed.internal

import kotlin.math.cbrt
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * Internal color conversion utilities.
 * sRGB <-> Linear RGB <-> XYZ <-> L*a*b*
 */
internal object ColorUtils {

    /** D65 white point XYZ values. */
    val WHITE_POINT_D65: DoubleArray = doubleArrayOf(95.047, 100.0, 108.883)

    /**
     * sRGB to XYZ conversion matrix.
     * From IEC 61966-2-1:1999 (sRGB).
     */
    private val SRGB_TO_XYZ = arrayOf(
        doubleArrayOf(0.41233895, 0.35762064, 0.18051042),
        doubleArrayOf(0.2126, 0.7152, 0.0722),
        doubleArrayOf(0.01932141, 0.11916382, 0.95034478)
    )

    /**
     * XYZ to sRGB conversion matrix (inverse of above).
     */
    private val XYZ_TO_SRGB = arrayOf(
        doubleArrayOf(3.2413774792388685, -1.5376652402851851, -0.49885366846268053),
        doubleArrayOf(-0.9691452513005321, 1.8758853451067872, 0.04156585616912061),
        doubleArrayOf(0.05562093689691305, -0.20395524564742123, 1.0571799111220335)
    )

    /**
     * ARGB integer to component arrays.
     */
    fun argbFromRgb(red: Int, green: Int, blue: Int): Int {
        return (255 shl 24) or ((red and 255) shl 16) or ((green and 255) shl 8) or (blue and 255)
    }

    fun redFromArgb(argb: Int): Int = (argb shr 16) and 255
    fun greenFromArgb(argb: Int): Int = (argb shr 8) and 255
    fun blueFromArgb(argb: Int): Int = argb and 255
    fun alphaFromArgb(argb: Int): Int = (argb ushr 24) and 255

    /**
     * Convert a single sRGB component (0..255) to linear [0..100].
     */
    fun linearized(rgbComponent: Int): Double {
        val normalized = rgbComponent.toDouble() / 255.0
        return if (normalized <= 0.040449936) {
            normalized / 12.92 * 100.0
        } else {
            ((normalized + 0.055) / 1.055).pow(2.4) * 100.0
        }
    }

    /**
     * Convert a linear component [0..100] to sRGB [0..255].
     */
    fun delinearized(rgbComponent: Double): Int {
        val normalized = rgbComponent / 100.0
        val delinearized = if (normalized <= 0.0031308) {
            normalized * 12.92
        } else {
            1.055 * normalized.pow(1.0 / 2.4) - 0.055
        }
        return (delinearized * 255.0).roundToInt().coerceIn(0, 255)
    }

    /**
     * Convert ARGB to XYZ.
     */
    fun xyzFromArgb(argb: Int): DoubleArray {
        val r = linearized(redFromArgb(argb))
        val g = linearized(greenFromArgb(argb))
        val b = linearized(blueFromArgb(argb))
        return doubleArrayOf(
            SRGB_TO_XYZ[0][0] * r + SRGB_TO_XYZ[0][1] * g + SRGB_TO_XYZ[0][2] * b,
            SRGB_TO_XYZ[1][0] * r + SRGB_TO_XYZ[1][1] * g + SRGB_TO_XYZ[1][2] * b,
            SRGB_TO_XYZ[2][0] * r + SRGB_TO_XYZ[2][1] * g + SRGB_TO_XYZ[2][2] * b
        )
    }

    /**
     * Convert XYZ to ARGB.
     */
    fun argbFromXyz(x: Double, y: Double, z: Double): Int {
        val linearR = XYZ_TO_SRGB[0][0] * x + XYZ_TO_SRGB[0][1] * y + XYZ_TO_SRGB[0][2] * z
        val linearG = XYZ_TO_SRGB[1][0] * x + XYZ_TO_SRGB[1][1] * y + XYZ_TO_SRGB[1][2] * z
        val linearB = XYZ_TO_SRGB[2][0] * x + XYZ_TO_SRGB[2][1] * y + XYZ_TO_SRGB[2][2] * z
        val r = delinearized(linearR)
        val g = delinearized(linearG)
        val b = delinearized(linearB)
        return argbFromRgb(r, g, b)
    }

    /**
     * Convert ARGB to L*a*b*.
     */
    fun labFromArgb(argb: Int): DoubleArray {
        val linearR = linearized(redFromArgb(argb))
        val linearG = linearized(greenFromArgb(argb))
        val linearB = linearized(blueFromArgb(argb))
        val matrix = SRGB_TO_XYZ
        val x = matrix[0][0] * linearR + matrix[0][1] * linearG + matrix[0][2] * linearB
        val y = matrix[1][0] * linearR + matrix[1][1] * linearG + matrix[1][2] * linearB
        val z = matrix[2][0] * linearR + matrix[2][1] * linearG + matrix[2][2] * linearB
        val xNormalized = x / WHITE_POINT_D65[0]
        val yNormalized = y / WHITE_POINT_D65[1]
        val zNormalized = z / WHITE_POINT_D65[2]
        val fx = labF(xNormalized)
        val fy = labF(yNormalized)
        val fz = labF(zNormalized)
        val l = 116.0 * fy - 16.0
        val a = 500.0 * (fx - fy)
        val b = 200.0 * (fy - fz)
        return doubleArrayOf(l, a, b)
    }

    /**
     * Convert ARGB to L* (CIE lightness).
     */
    fun lstarFromArgb(argb: Int): Double {
        val y = xyzFromArgb(argb)[1]
        return 116.0 * labF(y / 100.0) - 16.0
    }

    /**
     * Convert L* to Y (relative luminance, 0..100).
     */
    fun yFromLstar(lstar: Double): Double {
        return 100.0 * labInvf((lstar + 16.0) / 116.0)
    }

    /**
     * Convert ARGB to ARGB from L*a*b*.
     */
    fun argbFromLstar(lstar: Double): Int {
        val y = yFromLstar(lstar)
        val component = delinearized(y)
        return argbFromRgb(component, component, component)
    }

    /**
     * Linearize an sRGB component for WCAG relative luminance.
     * Input and output in [0..1] range.
     */
    fun linearizedWcag(component: Double): Double {
        return if (component <= 0.04045) {
            component / 12.92
        } else {
            ((component + 0.055) / 1.055).pow(2.4)
        }
    }

    // L*a*b* forward transform helper
    private fun labF(t: Double): Double {
        val e = 216.0 / 24389.0
        val kappa = 24389.0 / 27.0
        return if (t > e) {
            cbrt(t)
        } else {
            (kappa * t + 16.0) / 116.0
        }
    }

    // L*a*b* inverse transform helper
    private fun labInvf(ft: Double): Double {
        val e = 216.0 / 24389.0
        val kappa = 24389.0 / 27.0
        val ft3 = ft * ft * ft
        return if (ft3 > e) {
            ft3
        } else {
            (116.0 * ft - 16.0) / kappa
        }
    }
}
