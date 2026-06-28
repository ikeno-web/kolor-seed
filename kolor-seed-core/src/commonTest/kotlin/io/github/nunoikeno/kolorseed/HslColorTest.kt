package io.github.nunoikeno.kolorseed

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HslColorTest {

    @Test
    fun red_hsl() {
        val hsl = HslColor.fromColor(Color.fromHex("#FF0000"))
        assertTrue(abs(hsl.hue - 0.0) < 1.0 || abs(hsl.hue - 360.0) < 1.0, "Red hue should be 0/360, got ${hsl.hue}")
        assertTrue(abs(hsl.saturation - 1.0) < 0.01, "Red saturation should be 1.0, got ${hsl.saturation}")
        assertTrue(abs(hsl.lightness - 0.5) < 0.01, "Red lightness should be 0.5, got ${hsl.lightness}")
    }

    @Test
    fun green_hsl() {
        val hsl = HslColor.fromColor(Color.fromHex("#00FF00"))
        assertTrue(abs(hsl.hue - 120.0) < 1.0, "Green hue should be 120, got ${hsl.hue}")
        assertTrue(abs(hsl.saturation - 1.0) < 0.01)
        assertTrue(abs(hsl.lightness - 0.5) < 0.01)
    }

    @Test
    fun blue_hsl() {
        val hsl = HslColor.fromColor(Color.fromHex("#0000FF"))
        assertTrue(abs(hsl.hue - 240.0) < 1.0, "Blue hue should be 240, got ${hsl.hue}")
        assertTrue(abs(hsl.saturation - 1.0) < 0.01)
        assertTrue(abs(hsl.lightness - 0.5) < 0.01)
    }

    @Test
    fun white_hsl() {
        val hsl = HslColor.fromColor(Color.fromHex("#FFFFFF"))
        assertTrue(abs(hsl.lightness - 1.0) < 0.01, "White lightness should be 1.0")
        assertEquals(0.0, hsl.saturation, "White saturation should be 0")
    }

    @Test
    fun black_hsl() {
        val hsl = HslColor.fromColor(Color.fromHex("#000000"))
        assertTrue(abs(hsl.lightness) < 0.01, "Black lightness should be 0")
    }

    @Test
    fun gray_hsl() {
        val hsl = HslColor.fromColor(Color.fromHex("#808080"))
        assertTrue(abs(hsl.saturation) < 0.01, "Gray saturation should be 0, got ${hsl.saturation}")
        assertTrue(abs(hsl.lightness - 0.502) < 0.01, "Gray lightness should be ~0.5, got ${hsl.lightness}")
    }

    @Test
    fun round_trip_rgb_to_hsl_and_back() {
        val testColors = listOf("#FF0000", "#00FF00", "#0000FF", "#6750A4", "#FF5722", "#808080", "#FFFFFF", "#000000")
        for (hex in testColors) {
            val original = Color.fromHex(hex)
            val hsl = original.toHsl()
            val roundTripped = hsl.toColor()
            assertTrue(
                abs(original.red - roundTripped.red) <= 1 &&
                    abs(original.green - roundTripped.green) <= 1 &&
                    abs(original.blue - roundTripped.blue) <= 1,
                "HSL round-trip failed for $hex: got ${roundTripped.toHex()}"
            )
        }
    }

    @Test
    fun from_clamps_values() {
        val hsl = HslColor.from(400.0, 1.5, -0.5)
        assertTrue(hsl.hue in 0.0..360.0)
        assertTrue(hsl.saturation in 0.0..1.0)
        assertTrue(hsl.lightness in 0.0..1.0)
    }

    @Test
    fun toColor_converts_correctly() {
        // HSL(0, 1.0, 0.5) should be red
        val red = HslColor.from(0.0, 1.0, 0.5).toColor()
        assertEquals(255, red.red)
        assertEquals(0, red.green)
        assertEquals(0, red.blue)
    }
}
