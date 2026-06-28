package io.github.nunoikeno.kolorseed

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class TonalPaletteTest {

    @Test
    fun tone_0_is_black() {
        val palette = TonalPalette.fromHueAndChroma(270.0, 48.0)
        assertEquals("#000000", palette.tone(0).toHex())
    }

    @Test
    fun tone_100_is_white() {
        val palette = TonalPalette.fromHueAndChroma(270.0, 48.0)
        assertEquals("#FFFFFF", palette.tone(100).toHex())
    }

    @Test
    fun tone_rejects_out_of_range() {
        val palette = TonalPalette.fromHueAndChroma(270.0, 48.0)
        assertFailsWith<IllegalArgumentException> { palette.tone(-1) }
        assertFailsWith<IllegalArgumentException> { palette.tone(101) }
    }

    @Test
    fun tones_increase_in_lightness() {
        val palette = TonalPalette.fromHueAndChroma(120.0, 40.0)
        var prevLuminance = -1.0
        for (t in listOf(0, 10, 20, 30, 40, 50, 60, 70, 80, 90, 100)) {
            val color = palette.tone(t)
            // Approximate luminance from RGB
            val lum = 0.2126 * color.red + 0.7152 * color.green + 0.0722 * color.blue
            assertTrue(lum >= prevLuminance - 1.0, "Tone $t should be lighter than previous tone")
            prevLuminance = lum
        }
    }

    @Test
    fun tone_is_cached() {
        val palette = TonalPalette.fromHueAndChroma(270.0, 48.0)
        val first = palette.tone(40)
        val second = palette.tone(40)
        // Same object (cached)
        assertTrue(first.argb == second.argb, "Cached tone should return same color")
    }

    @Test
    fun toMap_contains_standard_tones() {
        val palette = TonalPalette.fromHueAndChroma(270.0, 48.0)
        val map = palette.toMap()
        for (t in TonalPalette.STANDARD_TONES) {
            assertTrue(t in map, "Standard tone $t should be in map")
        }
        assertEquals(TonalPalette.STANDARD_TONES.size, map.size)
    }

    @Test
    fun fromColor_extracts_hue_and_chroma() {
        val color = Color.fromHex("#6750A4")
        val palette = TonalPalette.fromColor(color)
        assertTrue(palette.hue > 0.0, "Hue should be positive")
        assertTrue(palette.chroma > 0.0, "Chroma should be positive")
    }

    @Test
    fun different_hues_produce_different_colors() {
        val p1 = TonalPalette.fromHueAndChroma(0.0, 48.0)
        val p2 = TonalPalette.fromHueAndChroma(180.0, 48.0)
        // At tone 50, different hues should produce different colors
        assertTrue(p1.tone(50).argb != p2.tone(50).argb, "Different hues should produce different colors at same tone")
    }

    // --- M3 reference palette tests ---

    @Test
    fun m3_primary_palette_tone_40() {
        // CorePalette for #6750A4 uses max(chroma, 48)
        val hct = Hct.fromArgb(0xFF6750A4.toInt())
        val palette = TonalPalette.fromHueAndChroma(hct.hue, maxOf(hct.chroma, 48.0))
        // Reference: tone(40) = #6750A4 or very close
        val tone40 = palette.tone(40)
        // Should be very close to the original seed
        val rDiff = kotlin.math.abs(tone40.red - 103)
        val gDiff = kotlin.math.abs(tone40.green - 80)
        val bDiff = kotlin.math.abs(tone40.blue - 164)
        assertTrue(rDiff <= 2 && gDiff <= 2 && bDiff <= 2,
            "Primary tone(40) should be ~#6750A4, got ${tone40.toHex()}")
    }

    @Test
    fun m3_primary_palette_key_tones() {
        // Reference values from material-color-utilities for seed #6750A4
        val hct = Hct.fromArgb(0xFF6750A4.toInt())
        val palette = TonalPalette.fromHueAndChroma(hct.hue, maxOf(hct.chroma, 48.0))

        // tone(0) is always black
        assertEquals("#000000", palette.tone(0).toHex())
        // tone(100) is always white
        assertEquals("#FFFFFF", palette.tone(100).toHex())

        // Reference tone(10) = #22005D
        val tone10 = palette.tone(10)
        assertTrue(isCloseColor(tone10, Color.fromHex("#22005D"), 5),
            "Primary tone(10) should be ~#22005D, got ${tone10.toHex()}")
    }

    private fun isCloseColor(a: Color, b: Color, tolerance: Int): Boolean {
        return kotlin.math.abs(a.red - b.red) <= tolerance &&
            kotlin.math.abs(a.green - b.green) <= tolerance &&
            kotlin.math.abs(a.blue - b.blue) <= tolerance
    }
}
