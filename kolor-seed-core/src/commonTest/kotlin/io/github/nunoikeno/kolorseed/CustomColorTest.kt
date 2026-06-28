package io.github.nunoikeno.kolorseed

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class CustomColorTest {

    @Test
    fun withCustomColor_creates_extended_palette() {
        val palette = CorePalette.fromHex("#6750A4")
        val extended = palette.withCustomColor("brand", Color.fromHex("#2E7D32"))
        assertTrue(extended.customColors.containsKey("brand"))
    }

    @Test
    fun withCustomColor_harmonized_differs_from_original() {
        val palette = CorePalette.fromHex("#6750A4")
        val brandGreen = Color.fromHex("#2E7D32")
        val extended = palette.withCustomColor("brand", brandGreen, harmonize = true)
        val group = extended.customColors["brand"]!!
        // Original color should be preserved
        assertEquals(brandGreen.argb, group.color.argb)
        // Harmonized color should be shifted
        assertNotEquals(brandGreen.argb, group.harmonizedColor.argb)
    }

    @Test
    fun withCustomColor_no_harmonize_preserves_color() {
        val palette = CorePalette.fromHex("#6750A4")
        val brandGreen = Color.fromHex("#2E7D32")
        val extended = palette.withCustomColor("brand", brandGreen, harmonize = false)
        val group = extended.customColors["brand"]!!
        assertEquals(brandGreen.argb, group.harmonizedColor.argb)
    }

    @Test
    fun custom_color_has_four_roles() {
        val palette = CorePalette.fromHex("#6750A4")
        val extended = palette.withCustomColor("brand", Color.fromHex("#2E7D32"))
        val group = extended.customColors["brand"]!!
        // Light roles
        assertTrue(group.light.color.argb != 0)
        assertTrue(group.light.onColor.argb != 0)
        assertTrue(group.light.colorContainer.argb != 0)
        assertTrue(group.light.onColorContainer.argb != 0)
        // Dark roles
        assertTrue(group.dark.color.argb != 0)
        assertTrue(group.dark.onColor.argb != 0)
        assertTrue(group.dark.colorContainer.argb != 0)
        assertTrue(group.dark.onColorContainer.argb != 0)
    }

    @Test
    fun extended_palette_chains_custom_colors() {
        val palette = CorePalette.fromHex("#6750A4")
        val extended = palette
            .withCustomColor("brand", Color.fromHex("#2E7D32"))
            .withCustomColor("warning", Color.fromHex("#F57C00"), harmonize = false)
        assertEquals(2, extended.customColors.size)
        assertTrue(extended.customColors.containsKey("brand"))
        assertTrue(extended.customColors.containsKey("warning"))
    }

    @Test
    fun extended_lightScheme_includes_custom_roles() {
        val palette = CorePalette.fromHex("#6750A4")
        val extended = palette.withCustomColor("brand", Color.fromHex("#2E7D32"))
        val scheme = extended.lightScheme()
        assertTrue(scheme.customColors.containsKey("brand"))
        // Light custom role should use tone(40) for color
        val brandRoles = scheme.customColors["brand"]!!
        assertTrue(brandRoles.color.argb != 0)
    }

    @Test
    fun extended_darkScheme_includes_custom_roles() {
        val palette = CorePalette.fromHex("#6750A4")
        val extended = palette.withCustomColor("brand", Color.fromHex("#2E7D32"))
        val scheme = extended.darkScheme()
        assertTrue(scheme.customColors.containsKey("brand"))
        val brandRoles = scheme.customColors["brand"]!!
        assertTrue(brandRoles.color.argb != 0)
    }

    @Test
    fun light_and_dark_custom_roles_differ() {
        val palette = CorePalette.fromHex("#6750A4")
        val extended = palette.withCustomColor("brand", Color.fromHex("#2E7D32"))
        val light = extended.lightScheme().customColors["brand"]!!
        val dark = extended.darkScheme().customColors["brand"]!!
        assertNotEquals(light.color.argb, dark.color.argb, "Light and dark custom color should differ")
    }
}
