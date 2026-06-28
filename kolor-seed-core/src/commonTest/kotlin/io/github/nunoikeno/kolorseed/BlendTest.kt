package io.github.nunoikeno.kolorseed

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class BlendTest {

    @Test
    fun harmonize_shifts_hue_towards_source() {
        val design = Color.fromHex("#2E7D32") // Green
        val source = Color.fromHex("#6750A4") // Purple
        val harmonized = Blend.harmonize(design, source)

        val designHct = design.toHct()
        val sourceHct = source.toHct()
        val harmonizedHct = harmonized.toHct()

        // The harmonized hue should be closer to source than the original
        val originalDiff = hueDiff(designHct.hue, sourceHct.hue)
        val harmonizedDiff = hueDiff(harmonizedHct.hue, sourceHct.hue)
        assertTrue(harmonizedDiff <= originalDiff + 1.0,
            "Harmonized should be closer to source hue. Original diff=$originalDiff, harmonized diff=$harmonizedDiff")
    }

    @Test
    fun harmonize_limits_rotation_to_15_degrees() {
        val design = Color.fromHex("#FF0000") // Red (hue ~27)
        val source = Color.fromHex("#0000FF") // Blue (hue ~283)
        val harmonized = Blend.harmonize(design, source)

        val designHct = design.toHct()
        val harmonizedHct = harmonized.toHct()

        val shift = hueDiff(designHct.hue, harmonizedHct.hue)
        assertTrue(shift <= 16.0,
            "Harmonize should shift at most 15 degrees, shifted $shift")
    }

    @Test
    fun harmonize_preserves_tone() {
        val design = Color.fromHex("#2E7D32")
        val source = Color.fromHex("#6750A4")
        val harmonized = Blend.harmonize(design, source)

        val designHct = design.toHct()
        val harmonizedHct = harmonized.toHct()

        assertTrue(abs(designHct.tone - harmonizedHct.tone) < 3.0,
            "Harmonize should preserve tone. Original=${designHct.tone}, harmonized=${harmonizedHct.tone}")
    }

    @Test
    fun harmonize_preserves_chroma() {
        val design = Color.fromHex("#2E7D32")
        val source = Color.fromHex("#6750A4")
        val harmonized = Blend.harmonize(design, source)

        val designHct = design.toHct()
        val harmonizedHct = harmonized.toHct()

        assertTrue(abs(designHct.chroma - harmonizedHct.chroma) < 5.0,
            "Harmonize should preserve chroma. Original=${designHct.chroma}, harmonized=${harmonizedHct.chroma}")
    }

    @Test
    fun harmonize_changes_color() {
        val design = Color.fromHex("#2E7D32")
        val source = Color.fromHex("#6750A4")
        val harmonized = Blend.harmonize(design, source)
        assertNotEquals(design.argb, harmonized.argb, "Harmonized color should differ from input")
    }

    @Test
    fun harmonize_same_hue_no_change() {
        // If colors are already the same hue, harmonization should change nothing
        val a = Color.fromHex("#6750A4")
        val harmonized = Blend.harmonize(a, a)
        // Should be the same or very close
        val hctA = a.toHct()
        val hctH = harmonized.toHct()
        assertTrue(abs(hctA.hue - hctH.hue) < 1.0)
    }

    @Test
    fun hctHue_amount_0_returns_from_hue() {
        val from = Color.fromHex("#FF0000")
        val to = Color.fromHex("#0000FF")
        val result = Blend.hctHue(from, to, 0.0)
        val fromHct = from.toHct()
        val resultHct = result.toHct()
        assertTrue(abs(fromHct.hue - resultHct.hue) < 5.0,
            "Amount 0 should keep original hue")
    }

    @Test
    fun cam16Ucs_amount_0_returns_from() {
        val from = Color.fromHex("#FF0000")
        val to = Color.fromHex("#0000FF")
        val result = Blend.cam16Ucs(from, to, 0.0)
        val fromHct = from.toHct()
        val resultHct = result.toHct()
        // At amount 0, should be very close to 'from'
        assertTrue(abs(fromHct.tone - resultHct.tone) < 3.0)
    }

    @Test
    fun cam16Ucs_amount_1_returns_to() {
        val from = Color.fromHex("#FF0000")
        val to = Color.fromHex("#0000FF")
        val result = Blend.cam16Ucs(from, to, 1.0)
        val toHct = to.toHct()
        val resultHct = result.toHct()
        // At amount 1, should be very close to 'to'
        assertTrue(abs(toHct.tone - resultHct.tone) < 3.0)
    }

    private fun hueDiff(a: Double, b: Double): Double {
        return 180.0 - abs(abs(a - b) - 180.0)
    }
}
