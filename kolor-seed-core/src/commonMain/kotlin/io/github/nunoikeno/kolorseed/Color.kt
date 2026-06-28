package io.github.nunoikeno.kolorseed

/**
 * Immutable color representation wrapping a 32-bit ARGB integer.
 *
 * This is the primary interchange type across all kolor-seed APIs.
 * It is an inline class to avoid heap allocation on JVM.
 *
 * ```kotlin
 * val color = Color.fromHex("#6750A4")
 * println(color.red)    // 103
 * println(color.toHex()) // "#6750A4"
 * ```
 */
@kotlin.jvm.JvmInline
public value class Color(public val argb: Int) {

    /** Red component, 0..255. */
    public inline val red: Int get() = (argb shr 16) and 0xFF

    /** Green component, 0..255. */
    public inline val green: Int get() = (argb shr 8) and 0xFF

    /** Blue component, 0..255. */
    public inline val blue: Int get() = argb and 0xFF

    /** Alpha component, 0..255. */
    public inline val alpha: Int get() = (argb ushr 24) and 0xFF

    /**
     * Convert to HCT color space.
     * @return [Hct] representation of this color
     */
    public fun toHct(): Hct = Hct.fromArgb(argb)

    /**
     * Convert to HEX string.
     * @return "#RRGGBB" (opaque) or "#AARRGGBB" (if alpha != 255)
     */
    public fun toHex(): String {
        return if (alpha == 255) {
            buildString(7) {
                append('#')
                appendHexByte(red)
                appendHexByte(green)
                appendHexByte(blue)
            }
        } else {
            buildString(9) {
                append('#')
                appendHexByte(alpha)
                appendHexByte(red)
                appendHexByte(green)
                appendHexByte(blue)
            }
        }
    }

    /**
     * Convert to HSL.
     * @return [HslColor] with h in 0..360, s in 0..1, l in 0..1
     */
    public fun toHsl(): HslColor = HslColor.fromColor(this)

    override fun toString(): String = toHex()

    public companion object {
        /**
         * Create from RGB components.
         * @param r Red 0..255
         * @param g Green 0..255
         * @param b Blue 0..255
         * @param a Alpha 0..255, default 255 (opaque)
         * @throws IllegalArgumentException if any component is out of range
         */
        public fun fromRgb(r: Int, g: Int, b: Int, a: Int = 255): Color {
            require(r in 0..255) { "Red must be 0..255, got $r" }
            require(g in 0..255) { "Green must be 0..255, got $g" }
            require(b in 0..255) { "Blue must be 0..255, got $b" }
            require(a in 0..255) { "Alpha must be 0..255, got $a" }
            return Color((a shl 24) or (r shl 16) or (g shl 8) or b)
        }

        /**
         * Create from HEX string.
         * Accepts "#RGB", "#RRGGBB", "#AARRGGBB" (# prefix optional).
         * @throws IllegalArgumentException if format is invalid
         */
        public fun fromHex(hex: String): Color {
            val h = if (hex.startsWith('#')) hex.substring(1) else hex
            return when (h.length) {
                3 -> {
                    val r = h[0].digitToInt(16)
                    val g = h[1].digitToInt(16)
                    val b = h[2].digitToInt(16)
                    fromRgb(r * 17, g * 17, b * 17)
                }
                6 -> {
                    val r = h.substring(0, 2).toInt(16)
                    val g = h.substring(2, 4).toInt(16)
                    val b = h.substring(4, 6).toInt(16)
                    fromRgb(r, g, b)
                }
                8 -> {
                    val a = h.substring(0, 2).toInt(16)
                    val r = h.substring(2, 4).toInt(16)
                    val g = h.substring(4, 6).toInt(16)
                    val b = h.substring(6, 8).toInt(16)
                    fromRgb(r, g, b, a)
                }
                else -> throw IllegalArgumentException(
                    "Invalid HEX color format: \"$hex\". Expected #RGB, #RRGGBB, or #AARRGGBB"
                )
            }
        }

        /**
         * Create from ARGB integer (0xAARRGGBB).
         */
        public fun fromArgb(argb: Int): Color = Color(argb)
    }
}

private fun StringBuilder.appendHexByte(value: Int) {
    val hi = value shr 4
    val lo = value and 0x0F
    append(HEX_CHARS[hi])
    append(HEX_CHARS[lo])
}

private val HEX_CHARS = charArrayOf(
    '0', '1', '2', '3', '4', '5', '6', '7',
    '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'
)
