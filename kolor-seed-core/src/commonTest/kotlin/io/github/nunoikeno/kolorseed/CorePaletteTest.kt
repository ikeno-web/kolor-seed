package io.github.nunoikeno.kolorseed

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CorePaletteTest {

    @Test
    fun fromSeed_creates_six_palettes() {
        val palette = CorePalette.fromSeed(Color.fromHex("#6750A4"))
        // All six palette groups should be non-null (guaranteed by constructor)
        // Verify they produce valid colors
        assertTrue(palette.primary.tone(40).argb != 0)
        assertTrue(palette.secondary.tone(40).argb != 0)
        assertTrue(palette.tertiary.tone(40).argb != 0)
        assertTrue(palette.neutral.tone(40).argb != 0)
        assertTrue(palette.neutralVariant.tone(40).argb != 0)
        assertTrue(palette.error.tone(40).argb != 0)
    }

    @Test
    fun fromSeed_primary_uses_seed_hue() {
        val seed = Color.fromHex("#6750A4")
        val palette = CorePalette.fromSeed(seed)
        val seedHct = seed.toHct()
        // Primary palette should use the seed hue
        assertTrue(abs(palette.primary.hue - seedHct.hue) < 1.0,
            "Primary hue should match seed hue")
    }

    @Test
    fun fromSeed_primary_chroma_at_least_48() {
        val seed = Color.fromHex("#6750A4")
        val palette = CorePalette.fromSeed(seed)
        assertTrue(palette.primary.chroma >= 48.0,
            "Primary chroma should be at least 48, got ${palette.primary.chroma}")
    }

    @Test
    fun fromSeed_secondary_chroma_is_16() {
        val seed = Color.fromHex("#6750A4")
        val palette = CorePalette.fromSeed(seed)
        assertEquals(16.0, palette.secondary.chroma, "Secondary chroma should be 16")
    }

    @Test
    fun fromSeed_tertiary_hue_shifted_60() {
        val seed = Color.fromHex("#6750A4")
        val palette = CorePalette.fromSeed(seed)
        val seedHct = seed.toHct()
        val expectedHue = (seedHct.hue + 60.0) % 360.0
        assertTrue(abs(palette.tertiary.hue - expectedHue) < 1.0,
            "Tertiary hue should be seed hue + 60, expected $expectedHue, got ${palette.tertiary.hue}")
    }

    @Test
    fun fromSeed_tertiary_chroma_is_24() {
        val seed = Color.fromHex("#6750A4")
        val palette = CorePalette.fromSeed(seed)
        assertEquals(24.0, palette.tertiary.chroma, "Tertiary chroma should be 24")
    }

    @Test
    fun fromSeed_neutral_chroma_is_4() {
        val seed = Color.fromHex("#6750A4")
        val palette = CorePalette.fromSeed(seed)
        assertEquals(4.0, palette.neutral.chroma, "Neutral chroma should be 4")
    }

    @Test
    fun fromSeed_neutral_variant_chroma_is_8() {
        val seed = Color.fromHex("#6750A4")
        val palette = CorePalette.fromSeed(seed)
        assertEquals(8.0, palette.neutralVariant.chroma, "NeutralVariant chroma should be 8")
    }

    @Test
    fun fromSeed_error_hue_is_25() {
        val seed = Color.fromHex("#6750A4")
        val palette = CorePalette.fromSeed(seed)
        assertEquals(25.0, palette.error.hue, "Error hue should be 25")
    }

    @Test
    fun fromSeed_error_chroma_is_84() {
        val seed = Color.fromHex("#6750A4")
        val palette = CorePalette.fromSeed(seed)
        assertEquals(84.0, palette.error.chroma, "Error chroma should be 84")
    }

    @Test
    fun fromHex_convenience() {
        val fromSeed = CorePalette.fromSeed(Color.fromHex("#6750A4"))
        val fromHex = CorePalette.fromHex("#6750A4")
        // Should produce identical palettes
        assertEquals(fromSeed.primary.tone(40).argb, fromHex.primary.tone(40).argb)
    }

    // --- Reference palette color comparison ---

    @Test
    fun m3_purple_primary_tone_40_exact() {
        val palette = CorePalette.fromHex("#6750A4")
        // Reference: primary.tone(40) = #6750A4
        val tone40 = palette.primary.tone(40)
        assertTrue(isClose(tone40, "#6750A4", 2),
            "Primary tone(40) should be #6750A4, got ${tone40.toHex()}")
    }

    @Test
    fun m3_purple_primary_tone_0_is_black() {
        val palette = CorePalette.fromHex("#6750A4")
        assertEquals("#000000", palette.primary.tone(0).toHex())
    }

    @Test
    fun m3_purple_primary_tone_100_is_white() {
        val palette = CorePalette.fromHex("#6750A4")
        assertEquals("#FFFFFF", palette.primary.tone(100).toHex())
    }

    @Test
    fun m3_purple_primary_tone_10() {
        val palette = CorePalette.fromHex("#6750A4")
        // Reference: #22005D
        assertTrue(isClose(palette.primary.tone(10), "#22005D", 5),
            "Primary tone(10) should be ~#22005D, got ${palette.primary.tone(10).toHex()}")
    }

    @Test
    fun m3_purple_primary_tone_80() {
        val palette = CorePalette.fromHex("#6750A4")
        // Reference: #CFBCFF — our solver may differ slightly
        assertTrue(isClose(palette.primary.tone(80), "#CFBCFF", 10),
            "Primary tone(80) should be ~#CFBCFF, got ${palette.primary.tone(80).toHex()}")
    }

    @Test
    fun m3_purple_error_tone_40() {
        val palette = CorePalette.fromHex("#6750A4")
        // Reference: #BA1A1A
        assertTrue(isClose(palette.error.tone(40), "#BA1A1A", 5),
            "Error tone(40) should be ~#BA1A1A, got ${palette.error.tone(40).toHex()}")
    }

    @Test
    fun m3_purple_neutral_tone_10() {
        val palette = CorePalette.fromHex("#6750A4")
        // Reference: #1C1B1E
        assertTrue(isClose(palette.neutral.tone(10), "#1C1B1E", 3),
            "Neutral tone(10) should be ~#1C1B1E, got ${palette.neutral.tone(10).toHex()}")
    }

    @Test
    fun m3_purple_secondary_tone_40() {
        val palette = CorePalette.fromHex("#6750A4")
        // Reference: #625B71
        assertTrue(isClose(palette.secondary.tone(40), "#625B71", 3),
            "Secondary tone(40) should be ~#625B71, got ${palette.secondary.tone(40).toHex()}")
    }

    @Test
    fun m3_purple_tertiary_tone_40() {
        val palette = CorePalette.fromHex("#6750A4")
        // Reference: #7E5260
        assertTrue(isClose(palette.tertiary.tone(40), "#7E5260", 3),
            "Tertiary tone(40) should be ~#7E5260, got ${palette.tertiary.tone(40).toHex()}")
    }

    // --- Different seed tests ---

    @Test
    fun red_seed_produces_different_palettes() {
        val purple = CorePalette.fromHex("#6750A4")
        val red = CorePalette.fromHex("#FF0000")
        assertTrue(purple.primary.tone(40).argb != red.primary.tone(40).argb,
            "Different seeds should produce different primary colors")
    }

    @Test
    fun low_chroma_seed_uses_minimum_48() {
        // Gray (#808080) has very low chroma
        val palette = CorePalette.fromSeed(Color.fromHex("#808080"))
        assertTrue(palette.primary.chroma >= 48.0,
            "Primary chroma should be at least 48 even for gray seed")
    }

    // --- Scheme generation ---

    @Test
    fun lightScheme_and_darkScheme_differ() {
        val palette = CorePalette.fromHex("#6750A4")
        val light = palette.lightScheme()
        val dark = palette.darkScheme()
        assertTrue(light.primary.argb != dark.primary.argb,
            "Light and dark primary should differ")
        assertTrue(light.surface.argb != dark.surface.argb,
            "Light and dark surface should differ")
    }

    @Test
    fun toScheme_matches_light_and_dark() {
        val palette = CorePalette.fromHex("#6750A4")
        assertEquals(palette.lightScheme(), palette.toScheme(isDark = false))
        assertEquals(palette.darkScheme(), palette.toScheme(isDark = true))
    }

    private fun isClose(color: Color, hexRef: String, tolerance: Int): Boolean {
        val ref = Color.fromHex(hexRef)
        return abs(color.red - ref.red) <= tolerance &&
            abs(color.green - ref.green) <= tolerance &&
            abs(color.blue - ref.blue) <= tolerance
    }
}
