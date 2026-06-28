package io.github.nunoikeno.kolorseed

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HctTest {

    // --- Basic HCT forward transform tests (verified against material-color-utilities v0.2.7) ---

    @Test
    fun fromArgb_red_hue() {
        val hct = Hct.fromArgb(0xFFFF0000.toInt())
        assertTrue(abs(hct.hue - 27.41) < 1.0, "Red hue should be ~27.4, got ${hct.hue}")
        assertTrue(hct.chroma > 100.0, "Red chroma should be high, got ${hct.chroma}")
        assertTrue(abs(hct.tone - 53.23) < 1.0, "Red tone should be ~53.2, got ${hct.tone}")
    }

    @Test
    fun fromArgb_blue_hue() {
        val hct = Hct.fromArgb(0xFF0000FF.toInt())
        assertTrue(abs(hct.hue - 282.79) < 1.0, "Blue hue should be ~282.8, got ${hct.hue}")
        assertTrue(hct.chroma > 80.0, "Blue chroma should be high, got ${hct.chroma}")
        assertTrue(abs(hct.tone - 32.30) < 1.0, "Blue tone should be ~32.3, got ${hct.tone}")
    }

    @Test
    fun fromArgb_green_hue() {
        val hct = Hct.fromArgb(0xFF00FF00.toInt())
        assertTrue(abs(hct.hue - 142.14) < 1.0, "Green hue should be ~142.1, got ${hct.hue}")
        assertTrue(hct.chroma > 100.0, "Green chroma should be high, got ${hct.chroma}")
        assertTrue(abs(hct.tone - 87.74) < 1.0, "Green tone should be ~87.7, got ${hct.tone}")
    }

    @Test
    fun fromArgb_m3_purple() {
        val hct = Hct.fromArgb(0xFF6750A4.toInt())
        // Reference: H=298.98, C=47.86, T=40.08
        assertTrue(abs(hct.hue - 298.98) < 1.0, "M3 purple hue should be ~299.0, got ${hct.hue}")
        assertTrue(abs(hct.chroma - 48.0) < 2.0, "M3 purple chroma should be ~48, got ${hct.chroma}")
        assertTrue(abs(hct.tone - 40.08) < 1.0, "M3 purple tone should be ~40.1, got ${hct.tone}")
    }

    @Test
    fun fromArgb_black() {
        val hct = Hct.fromArgb(0xFF000000.toInt())
        assertTrue(abs(hct.tone) < 1.0, "Black tone should be ~0, got ${hct.tone}")
        assertTrue(hct.chroma < 1.0, "Black chroma should be ~0, got ${hct.chroma}")
    }

    @Test
    fun fromArgb_white() {
        val hct = Hct.fromArgb(0xFFFFFFFF.toInt())
        assertTrue(abs(hct.tone - 100.0) < 1.0, "White tone should be ~100, got ${hct.tone}")
        assertTrue(hct.chroma < 3.0, "White chroma should be ~0, got ${hct.chroma}")
    }

    // --- HCT solver tests ---

    @Test
    fun from_preserves_tone() {
        for (tone in listOf(0.0, 10.0, 20.0, 30.0, 40.0, 50.0, 60.0, 70.0, 80.0, 90.0, 100.0)) {
            val hct = Hct.from(120.0, 40.0, tone)
            assertTrue(
                abs(hct.tone - tone) < 1.5,
                "Tone should be ~$tone, got ${hct.tone}"
            )
        }
    }

    @Test
    fun from_achromatic_produces_gray() {
        val hct = Hct.from(0.0, 0.0, 50.0)
        val color = hct.toColor()
        val maxDiff = maxOf(
            abs(color.red - color.green),
            abs(color.green - color.blue),
            abs(color.red - color.blue)
        )
        assertTrue(maxDiff < 5, "Achromatic HCT should produce gray, got ${color.toHex()}")
    }

    @Test
    fun tone_0_is_black() {
        val hct = Hct.from(270.0, 48.0, 0.0)
        assertEquals("#000000", hct.toColor().toHex(), "Tone 0 should be black")
    }

    @Test
    fun tone_100_is_white() {
        val hct = Hct.from(270.0, 48.0, 100.0)
        assertEquals("#FFFFFF", hct.toColor().toHex(), "Tone 100 should be white")
    }

    // --- withXxx tests ---

    @Test
    fun withHue_changes_hue_preserves_tone() {
        val original = Hct.from(120.0, 40.0, 50.0)
        val modified = original.withHue(240.0)
        assertTrue(abs(modified.tone - 50.0) < 2.0, "Tone should be preserved after hue change")
    }

    @Test
    fun withTone_changes_tone() {
        val original = Hct.from(120.0, 40.0, 50.0)
        val lighter = original.withTone(80.0)
        assertTrue(abs(lighter.tone - 80.0) < 2.0, "Tone should be ~80, got ${lighter.tone}")
    }

    @Test
    fun withChroma_reduces_chroma() {
        val original = Hct.from(120.0, 40.0, 50.0)
        val lessChromatic = original.withChroma(10.0)
        assertTrue(lessChromatic.chroma <= 15.0, "Chroma should be reduced, got ${lessChromatic.chroma}")
    }

    // --- Round-trip tests ---

    @Test
    fun round_trip_non_boundary_colors() {
        // These colors are well inside the sRGB gamut and should round-trip precisely
        val testColors = listOf(
            0xFF6750A4.toInt(), // M3 purple (inside gamut)
            0xFF808080.toInt(), // Gray
            0xFFFF5722.toInt(), // Deep Orange
            0xFF0000FF.toInt(), // Blue
        )
        for (argb in testColors) {
            val hct = Hct.fromArgb(argb)
            val reconstructed = Hct.from(hct.hue, hct.chroma, hct.tone).toColor()
            val original = Color(argb)
            val rDiff = abs(original.red - reconstructed.red)
            val gDiff = abs(original.green - reconstructed.green)
            val bDiff = abs(original.blue - reconstructed.blue)
            assertTrue(
                rDiff <= 3 && gDiff <= 3 && bDiff <= 3,
                "Round-trip failed for ${original.toHex()}: got ${reconstructed.toHex()}"
            )
        }
    }

    @Test
    fun round_trip_gamut_boundary_colors() {
        // Red and Green are at the sRGB gamut boundary — tolerance is wider
        val testColors = listOf(
            0xFFFF0000.toInt(), // Red (at gamut boundary)
            0xFF00FF00.toInt(), // Green (at gamut boundary)
        )
        for (argb in testColors) {
            val hct = Hct.fromArgb(argb)
            val reconstructed = Hct.from(hct.hue, hct.chroma, hct.tone).toColor()
            val original = Color(argb)
            val rDiff = abs(original.red - reconstructed.red)
            val gDiff = abs(original.green - reconstructed.green)
            val bDiff = abs(original.blue - reconstructed.blue)
            assertTrue(
                rDiff <= 12 && gDiff <= 12 && bDiff <= 12,
                "Round-trip (boundary) failed for ${original.toHex()}: got ${reconstructed.toHex()}"
            )
        }
    }

    @Test
    fun hct_equals_uses_argb() {
        val a = Hct.from(270.0, 48.0, 50.0)
        val b = Hct.from(270.0, 48.0, 50.0)
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
    }

    @Test
    fun hct_toString_format() {
        val hct = Hct.from(270.0, 48.0, 50.0)
        val str = hct.toString()
        assertTrue(str.startsWith("Hct("), "toString should start with 'Hct(', got: $str")
    }
}
