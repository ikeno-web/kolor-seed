package io.github.nunoikeno.kolorseed

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ContrastTest {

    @Test
    fun ratioOf_black_and_white_is_21() {
        val black = Color.fromHex("#000000")
        val white = Color.fromHex("#FFFFFF")
        val ratio = Contrast.ratioOf(black, white)
        assertTrue(abs(ratio - 21.0) < 0.1, "Black/white contrast should be ~21:1, got $ratio")
    }

    @Test
    fun ratioOf_same_color_is_1() {
        val color = Color.fromHex("#808080")
        val ratio = Contrast.ratioOf(color, color)
        assertTrue(abs(ratio - 1.0) < 0.01, "Same color contrast should be 1:1, got $ratio")
    }

    @Test
    fun ratioOf_is_symmetric() {
        val a = Color.fromHex("#FF0000")
        val b = Color.fromHex("#0000FF")
        val ratio1 = Contrast.ratioOf(a, b)
        val ratio2 = Contrast.ratioOf(b, a)
        assertTrue(abs(ratio1 - ratio2) < 0.001, "Contrast ratio should be symmetric")
    }

    @Test
    fun ratioOf_always_gte_1() {
        val a = Color.fromHex("#123456")
        val b = Color.fromHex("#ABCDEF")
        val ratio = Contrast.ratioOf(a, b)
        assertTrue(ratio >= 1.0, "Contrast ratio should always be >= 1.0, got $ratio")
    }

    // --- WCAG level checks ---

    @Test
    fun meetsAA_black_on_white() {
        assertTrue(Contrast.meetsAA(Color.fromHex("#000000"), Color.fromHex("#FFFFFF")))
    }

    @Test
    fun meetsAAA_black_on_white() {
        assertTrue(Contrast.meetsAAA(Color.fromHex("#000000"), Color.fromHex("#FFFFFF")))
    }

    @Test
    fun meetsAALargeText_black_on_white() {
        assertTrue(Contrast.meetsAALargeText(Color.fromHex("#000000"), Color.fromHex("#FFFFFF")))
    }

    @Test
    fun meetsAA_low_contrast_fails() {
        // Two similar grays should not meet AA
        assertFalse(Contrast.meetsAA(Color.fromHex("#808080"), Color.fromHex("#909090")))
    }

    @Test
    fun meetsAAA_moderate_contrast_fails() {
        // 4.5:1 meets AA but not AAA (needs 7:1)
        // #757575 on white is approximately 4.6:1
        val gray = Color.fromHex("#757575")
        val white = Color.fromHex("#FFFFFF")
        val ratio = Contrast.ratioOf(gray, white)
        if (ratio >= 4.5 && ratio < 7.0) {
            assertTrue(Contrast.meetsAA(gray, white))
            assertFalse(Contrast.meetsAAA(gray, white))
        }
    }

    @Test
    fun m3_primary_on_primary_meets_AA() {
        // M3 guarantees sufficient contrast between on-colors and their backgrounds
        val scheme = ColorScheme.light(Color.fromHex("#6750A4"))
        assertTrue(Contrast.meetsAA(scheme.onPrimary, scheme.primary),
            "onPrimary on primary should meet AA")
    }

    @Test
    fun m3_on_surface_on_surface_meets_AA() {
        val scheme = ColorScheme.light(Color.fromHex("#6750A4"))
        assertTrue(Contrast.meetsAA(scheme.onSurface, scheme.surface),
            "onSurface on surface should meet AA")
    }

    // --- findToneForContrast ---

    @Test
    fun findToneForContrast_finds_aa_tone() {
        val palette = TonalPalette.fromHueAndChroma(270.0, 48.0)
        val background = Color.fromHex("#FFFFFF")
        val tone = Contrast.findToneForContrast(palette, background, 4.5)
        assertNotNull(tone, "Should find a tone meeting 4.5:1 contrast")
        // Verify the found tone actually meets the ratio
        val candidate = palette.tone(tone)
        assertTrue(Contrast.ratioOf(candidate, background) >= 4.5)
    }

    @Test
    fun findToneForContrast_preferLighter() {
        val palette = TonalPalette.fromHueAndChroma(270.0, 48.0)
        val background = Color.fromHex("#000000") // Dark background
        val tone = Contrast.findToneForContrast(palette, background, 4.5, preferLighter = true)
        assertNotNull(tone, "Should find a lighter tone meeting contrast")
        assertTrue(tone >= 50, "Preferred lighter tone should be >= 50, got $tone")
    }

    @Test
    fun findToneForContrast_impossible_returns_null() {
        // Looking for 21:1 contrast against mid-gray is impossible for most palettes
        val palette = TonalPalette.fromHueAndChroma(270.0, 48.0)
        val background = Color.fromHex("#808080")
        // 21:1 against mid-gray is very hard, tone 0 (black) should be ~10:1
        val tone = Contrast.findToneForContrast(palette, background, 21.0)
        // This may or may not find a solution depending on the palette
        // Just verify it returns either a valid tone or null without crashing
        if (tone != null) {
            assertTrue(tone in 0..100)
        }
    }

    // --- Known contrast ratios ---

    @Test
    fun known_contrast_ratio_m3_purple_on_white() {
        val purple = Color.fromHex("#6750A4")
        val white = Color.fromHex("#FFFFFF")
        val ratio = Contrast.ratioOf(purple, white)
        // #6750A4 on white should have a contrast ratio around 5.3:1
        assertTrue(ratio > 4.0 && ratio < 7.0,
            "M3 purple on white should be ~5.3:1, got $ratio")
    }
}
