package io.github.nunoikeno.kolorseed

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class ColorSchemeTest {

    // --- Light scheme tests ---

    @Test
    fun light_scheme_primary_from_tone_40() {
        val scheme = ColorScheme.light(Color.fromHex("#6750A4"))
        val palette = CorePalette.fromHex("#6750A4")
        assertEquals(palette.primary.tone(40).argb, scheme.primary.argb)
    }

    @Test
    fun light_scheme_on_primary_from_tone_100() {
        val scheme = ColorScheme.light(Color.fromHex("#6750A4"))
        val palette = CorePalette.fromHex("#6750A4")
        assertEquals(palette.primary.tone(100).argb, scheme.onPrimary.argb)
    }

    @Test
    fun light_scheme_primary_container_from_tone_90() {
        val scheme = ColorScheme.light(Color.fromHex("#6750A4"))
        val palette = CorePalette.fromHex("#6750A4")
        assertEquals(palette.primary.tone(90).argb, scheme.primaryContainer.argb)
    }

    @Test
    fun light_scheme_surface_is_light() {
        val scheme = ColorScheme.light(Color.fromHex("#6750A4"))
        // Surface from neutral tone(98) — should be very light
        val lum = 0.2126 * scheme.surface.red + 0.7152 * scheme.surface.green + 0.0722 * scheme.surface.blue
        assertTrue(lum > 200, "Light scheme surface should be light, luminance=$lum")
    }

    @Test
    fun light_scheme_scrim_is_black() {
        val scheme = ColorScheme.light(Color.fromHex("#6750A4"))
        assertEquals("#000000", scheme.scrim.toHex())
    }

    @Test
    fun light_scheme_shadow_is_black() {
        val scheme = ColorScheme.light(Color.fromHex("#6750A4"))
        assertEquals("#000000", scheme.shadow.toHex())
    }

    // --- Dark scheme tests ---

    @Test
    fun dark_scheme_primary_from_tone_80() {
        val scheme = ColorScheme.dark(Color.fromHex("#6750A4"))
        val palette = CorePalette.fromHex("#6750A4")
        assertEquals(palette.primary.tone(80).argb, scheme.primary.argb)
    }

    @Test
    fun dark_scheme_on_primary_from_tone_20() {
        val scheme = ColorScheme.dark(Color.fromHex("#6750A4"))
        val palette = CorePalette.fromHex("#6750A4")
        assertEquals(palette.primary.tone(20).argb, scheme.onPrimary.argb)
    }

    @Test
    fun dark_scheme_surface_is_dark() {
        val scheme = ColorScheme.dark(Color.fromHex("#6750A4"))
        // Surface from neutral tone(6) — should be very dark
        val lum = 0.2126 * scheme.surface.red + 0.7152 * scheme.surface.green + 0.0722 * scheme.surface.blue
        assertTrue(lum < 50, "Dark scheme surface should be dark, luminance=$lum")
    }

    @Test
    fun dark_scheme_scrim_is_black() {
        val scheme = ColorScheme.dark(Color.fromHex("#6750A4"))
        assertEquals("#000000", scheme.scrim.toHex())
    }

    // --- Light vs Dark comparison ---

    @Test
    fun light_primary_differs_from_dark_primary() {
        val light = ColorScheme.light(Color.fromHex("#6750A4"))
        val dark = ColorScheme.dark(Color.fromHex("#6750A4"))
        assertNotEquals(light.primary.argb, dark.primary.argb)
    }

    @Test
    fun light_surface_is_brighter_than_dark_surface() {
        val light = ColorScheme.light(Color.fromHex("#6750A4"))
        val dark = ColorScheme.dark(Color.fromHex("#6750A4"))
        val lightLum = light.surface.red + light.surface.green + light.surface.blue
        val darkLum = dark.surface.red + dark.surface.green + dark.surface.blue
        assertTrue(lightLum > darkLum, "Light surface should be brighter than dark surface")
    }

    @Test
    fun inverse_primary_swaps_light_dark() {
        val light = ColorScheme.light(Color.fromHex("#6750A4"))
        val dark = ColorScheme.dark(Color.fromHex("#6750A4"))
        // In light scheme, inversePrimary uses tone 80 (same as dark primary)
        assertEquals(dark.primary.argb, light.inversePrimary.argb)
        // In dark scheme, inversePrimary uses tone 40 (same as light primary)
        assertEquals(light.primary.argb, dark.inversePrimary.argb)
    }

    // --- fromCorePalette ---

    @Test
    fun fromCorePalette_light() {
        val palette = CorePalette.fromHex("#6750A4")
        val scheme = ColorScheme.fromCorePalette(palette, isDark = false)
        assertEquals(palette.primary.tone(40).argb, scheme.primary.argb)
    }

    @Test
    fun fromCorePalette_dark() {
        val palette = CorePalette.fromHex("#6750A4")
        val scheme = ColorScheme.fromCorePalette(palette, isDark = true)
        assertEquals(palette.primary.tone(80).argb, scheme.primary.argb)
    }

    // --- Serialization ---

    @Test
    fun toJson_contains_all_roles() {
        val scheme = ColorScheme.light(Color.fromHex("#6750A4"))
        val json = scheme.toJson()
        assertTrue(json.contains("\"primary\""))
        assertTrue(json.contains("\"onPrimary\""))
        assertTrue(json.contains("\"surface\""))
        assertTrue(json.contains("\"scrim\""))
        assertTrue(json.contains("\"background\""))
    }

    @Test
    fun toCssCustomProperties_format() {
        val scheme = ColorScheme.light(Color.fromHex("#6750A4"))
        val css = scheme.toCssCustomProperties()
        assertTrue(css.contains("--md-sys-color-primary:"))
        assertTrue(css.contains("--md-sys-color-on-primary:"))
        assertTrue(css.contains("#"))
    }

    @Test
    fun toFigmaTokens_format() {
        val scheme = ColorScheme.light(Color.fromHex("#6750A4"))
        val tokens = scheme.toFigmaTokens()
        assertTrue(tokens.contains("\"primary\""))
        assertTrue(tokens.contains("\"type\": \"color\""))
    }

    // --- Surface container hierarchy ---

    @Test
    fun light_surface_containers_are_ordered() {
        val scheme = ColorScheme.light(Color.fromHex("#6750A4"))
        // surfaceContainerLowest is lightest, surfaceContainerHighest is darkest (in light mode)
        val containers = listOf(
            scheme.surfaceContainerLowest,
            scheme.surfaceContainerLow,
            scheme.surfaceContainer,
            scheme.surfaceContainerHigh,
            scheme.surfaceContainerHighest
        )
        for (i in 0 until containers.size - 1) {
            val lum1 = containers[i].red + containers[i].green + containers[i].blue
            val lum2 = containers[i + 1].red + containers[i + 1].green + containers[i + 1].blue
            assertTrue(lum1 >= lum2 - 5,
                "Container at index $i should be lighter or equal to index ${i + 1}")
        }
    }

    // --- data class features ---

    @Test
    fun colorScheme_equals() {
        val a = ColorScheme.light(Color.fromHex("#6750A4"))
        val b = ColorScheme.light(Color.fromHex("#6750A4"))
        assertEquals(a, b)
    }

    @Test
    fun colorScheme_copy() {
        val original = ColorScheme.light(Color.fromHex("#6750A4"))
        val modified = original.copy(primary = Color.fromHex("#FF0000"))
        assertEquals(Color.fromHex("#FF0000").argb, modified.primary.argb)
        // Other fields unchanged
        assertEquals(original.secondary.argb, modified.secondary.argb)
    }

    @Test
    fun background_aliases_surface_in_light() {
        val scheme = ColorScheme.light(Color.fromHex("#6750A4"))
        // In M3, background = surface (both from neutral tone 98 in light)
        assertEquals(scheme.surface.argb, scheme.background.argb)
    }

    @Test
    fun background_aliases_surface_in_dark() {
        val scheme = ColorScheme.dark(Color.fromHex("#6750A4"))
        // In M3, background = surface (both from neutral tone 6 in dark)
        assertEquals(scheme.surface.argb, scheme.background.argb)
    }
}
