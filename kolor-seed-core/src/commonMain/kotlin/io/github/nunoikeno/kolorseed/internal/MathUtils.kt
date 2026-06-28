package io.github.nunoikeno.kolorseed.internal

import kotlin.math.abs
import kotlin.math.floor

/**
 * Internal math utilities for color space calculations.
 */
internal object MathUtils {

    fun clampInt(min: Int, max: Int, input: Int): Int {
        if (input < min) return min
        if (input > max) return max
        return input
    }

    fun clampDouble(min: Double, max: Double, input: Double): Double {
        if (input < min) return min
        if (input > max) return max
        return input
    }

    fun lerp(start: Double, stop: Double, amount: Double): Double {
        return (1.0 - amount) * start + amount * stop
    }

    fun sanitizeDegreesInt(degrees: Int): Int {
        var d = degrees % 360
        if (d < 0) d += 360
        return d
    }

    fun sanitizeDegreesDouble(degrees: Double): Double {
        var d = degrees % 360.0
        if (d < 0.0) d += 360.0
        return d
    }

    fun differenceDegrees(a: Double, b: Double): Double {
        return 180.0 - abs(abs(a - b) - 180.0)
    }

    fun rotationDirection(from: Double, to: Double): Double {
        val increasing = sanitizeDegreesDouble(to - from)
        return if (increasing <= 180.0) 1.0 else -1.0
    }

    /**
     * Multiply a 3x3 matrix by a 3-element vector.
     */
    fun matrixMultiply(row: DoubleArray, matrix: Array<DoubleArray>): DoubleArray {
        val a = row[0] * matrix[0][0] + row[1] * matrix[0][1] + row[2] * matrix[0][2]
        val b = row[0] * matrix[1][0] + row[1] * matrix[1][1] + row[2] * matrix[1][2]
        val c = row[0] * matrix[2][0] + row[1] * matrix[2][1] + row[2] * matrix[2][2]
        return doubleArrayOf(a, b, c)
    }

    /**
     * Sign of a double: 1.0, -1.0, or 0.0.
     */
    fun signum(num: Double): Double {
        return when {
            num < 0 -> -1.0
            num == 0.0 -> 0.0
            else -> 1.0
        }
    }
}
