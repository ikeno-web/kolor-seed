package io.github.nunoikeno.kolorseed

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ColorTest {

    @Test
    fun fromRgb_creates_correct_argb() {
        val color = Color.fromRgb(103, 80, 164)
        assertEquals(103, color.red)
        assertEquals(80, color.green)
        assertEquals(164, color.blue)
        assertEquals(255, color.alpha)
    }

    @Test
    fun fromRgb_with_alpha() {
        val color = Color.fromRgb(255, 0, 0, 128)
        assertEquals(255, color.red)
        assertEquals(0, color.green)
        assertEquals(0, color.blue)
        assertEquals(128, color.alpha)
    }

    @Test
    fun fromRgb_rejects_out_of_range() {
        assertFailsWith<IllegalArgumentException> {
            Color.fromRgb(256, 0, 0)
        }
        assertFailsWith<IllegalArgumentException> {
            Color.fromRgb(0, -1, 0)
        }
    }

    @Test
    fun fromHex_6digit() {
        val color = Color.fromHex("#6750A4")
        assertEquals(103, color.red)
        assertEquals(80, color.green)
        assertEquals(164, color.blue)
        assertEquals(255, color.alpha)
    }

    @Test
    fun fromHex_3digit() {
        val color = Color.fromHex("#F00")
        assertEquals(255, color.red)
        assertEquals(0, color.green)
        assertEquals(0, color.blue)
    }

    @Test
    fun fromHex_8digit_with_alpha() {
        val color = Color.fromHex("#80FF0000")
        assertEquals(128, color.alpha)
        assertEquals(255, color.red)
        assertEquals(0, color.green)
        assertEquals(0, color.blue)
    }

    @Test
    fun fromHex_without_hash() {
        val color = Color.fromHex("6750A4")
        assertEquals(103, color.red)
        assertEquals(80, color.green)
        assertEquals(164, color.blue)
    }

    @Test
    fun fromHex_rejects_invalid_format() {
        assertFailsWith<IllegalArgumentException> {
            Color.fromHex("#GGGGG")
        }
    }

    @Test
    fun toHex_opaque() {
        val color = Color.fromRgb(103, 80, 164)
        assertEquals("#6750A4", color.toHex())
    }

    @Test
    fun toHex_with_alpha() {
        val color = Color.fromRgb(255, 0, 0, 128)
        assertEquals("#80FF0000", color.toHex())
    }

    @Test
    fun fromArgb_round_trip() {
        val argb = 0xFF6750A4.toInt()
        val color = Color.fromArgb(argb)
        assertEquals(argb, color.argb)
        assertEquals(103, color.red)
    }

    @Test
    fun toHex_fromHex_round_trip() {
        val hex = "#6750A4"
        val color = Color.fromHex(hex)
        assertEquals(hex, color.toHex())
    }

    @Test
    fun black_and_white() {
        val black = Color.fromRgb(0, 0, 0)
        assertEquals("#000000", black.toHex())

        val white = Color.fromRgb(255, 255, 255)
        assertEquals("#FFFFFF", white.toHex())
    }

    @Test
    fun colorSchemes_convenience_produces_pair() {
        val (light, dark) = colorSchemes("#6750A4")
        // Light and dark schemes should differ
        assert(light.surface != dark.surface)
    }
}
