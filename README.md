# kolor-seed

[![JitPack](https://jitpack.io/v/ikeno-web/kolor-seed.svg)](https://jitpack.io/#ikeno-web/kolor-seed)
[![Kotlin](https://img.shields.io/badge/kotlin-2.0-blue.svg?logo=kotlin)](https://kotlinlang.org)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
![KMP](https://img.shields.io/badge/KMP-JVM%20%7C%20JS%20%7C%20Native-blueviolet)

**One seed color. Full Material 3 palette. Pure Kotlin. Zero dependencies.**

kolor-seed generates complete [Material 3](https://m3.material.io/styles/color/overview) color palettes from a single seed color, using a pure Kotlin implementation of the HCT (Hue-Chroma-Tone) color space. It works everywhere Kotlin runs -- Android, iOS, Desktop, Web, and server-side -- with no platform dependencies.

## Why kolor-seed?

- **Truly multiplatform** -- JVM, JS (IR), macOS, iOS, Linux, Windows. No `android.graphics.Color` or any platform API.
- **Zero runtime dependencies** -- Only `kotlin-stdlib`. Nothing else.
- **Blazing fast** -- Seed to full light+dark scheme in under 1ms. `@JvmInline value class` Color for zero-cost abstraction on JVM.
- **Material 3 compliant** -- All 29+ color roles, 6 tonal palette groups, custom color harmonization.
- **Developer-friendly API** -- One line to generate a theme. Full HCT access when you need fine control.

## Installation

Add the JitPack repository to your `settings.gradle.kts`:

```kotlin
dependencyResolutionManagement {
    repositories {
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}
```

Then add the dependency:

```kotlin
// build.gradle.kts
dependencies {
    // Core: HCT, palettes, schemes (no UI dependency)
    implementation("com.github.ikeno-web.kolor-seed:kolor-seed-core:v0.1.1")

    // Optional: Compose Color extensions
    implementation("com.github.ikeno-web.kolor-seed:kolor-seed-compose:v0.1.1")
}
```

## Quick Start

### Generate a full theme in one line

```kotlin
import io.github.nunoikeno.kolorseed.*

val (light, dark) = colorSchemes("#6750A4")

// Access any of the 29+ color roles
println(light.primary.toHex())           // "#6750A4"
println(light.primaryContainer.toHex())  // "#EADDFF"
println(dark.primary.toHex())            // "#D0BCFF"
```

### Use with Compose / Compose Multiplatform

```kotlin
import io.github.nunoikeno.kolorseed.*
import io.github.nunoikeno.kolorseed.compose.*

@Composable
fun AppTheme(
    seedColor: String = "#6750A4",
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val seed = Color.fromHex(seedColor)
    val scheme = if (darkTheme) ColorScheme.dark(seed) else ColorScheme.light(seed)

    // Convert to Compose Colors and apply
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = scheme.primary.toComposeColor(),
            onPrimary = scheme.onPrimary.toComposeColor(),
            // ... map all roles
        ),
        content = content
    )
}
```

### Fine-tune with HCT

```kotlin
import io.github.nunoikeno.kolorseed.*

val brand = Color.fromHex("#FF5722")
val hct = brand.toHct()

println("Hue: ${hct.hue}, Chroma: ${hct.chroma}, Tone: ${hct.tone}")

// Adjust individual perceptual attributes
val lighter = hct.withTone(80.0).toColor()
val muted   = hct.withChroma(24.0).toColor()
val shifted = hct.withHue((hct.hue + 30.0) % 360.0).toColor()
```

### Harmonize brand colors

```kotlin
val seed  = Color.fromHex("#6750A4")
val brand = Color.fromHex("#2E7D32")

// Shift brand green's hue towards the seed -- keeps it green, but cohesive
val harmonized = Blend.harmonize(brand, seed)

// Or add it as a full custom color group
val palette = CorePalette.fromSeed(seed)
    .withCustomColor("brand", brand, harmonize = true)
val scheme = palette.lightScheme()
val brandRoles = scheme.customColors["brand"]!!
```

### Check accessibility

```kotlin
val scheme = ColorScheme.light(Color.fromHex("#6750A4"))

val ratio = Contrast.ratioOf(scheme.onPrimary, scheme.primary)
println("Contrast: $ratio")                                      // e.g. 12.7
println("WCAG AA: ${Contrast.meetsAA(scheme.onPrimary, scheme.primary)}")  // true
```

### Export to CSS or Figma

```kotlin
val scheme = ColorScheme.light(Color.fromHex("#6750A4"))

// CSS custom properties
println(scheme.toCssCustomProperties())
// --md-sys-color-primary: #6750A4;
// --md-sys-color-on-primary: #FFFFFF;
// ...

// JSON for CI pipelines
println(scheme.toJson())

// Figma Tokens
println(scheme.toFigmaTokens())
```

## API Overview

### Core Module (`kolor-seed-core`)

| Class / Object | Description |
|---|---|
| `Color` | Inline value class wrapping ARGB int. HEX/RGB/HSL conversion. |
| `Hct` | HCT color space. Perceptually uniform hue, chroma, tone manipulation. |
| `TonalPalette` | 101 tones (0..100) for a single hue/chroma. Cached for performance. |
| `CorePalette` | 6 tonal palettes (Primary, Secondary, Tertiary, Neutral, NeutralVariant, Error) from one seed. |
| `ColorScheme` | All Material 3 color roles (primary, onPrimary, surface, etc.) for light or dark theme. |
| `Blend` | Color harmonization and perceptual blending (HCT hue, CAM16 UCS). |
| `Contrast` | WCAG 2.1 contrast ratio calculation. AA/AAA level checks. |
| `HslColor` | HSL color space conversion. |

### Compose Extension (`kolor-seed-compose`)

| Function | Description |
|---|---|
| `Color.toComposeColor()` | kolor-seed Color to `androidx.compose.ui.graphics.Color` |
| `ComposeColor.toKolorSeed()` | Compose Color to kolor-seed Color |

### Top-level Convenience Functions

```kotlin
val light = lightColorScheme("#6750A4")
val dark  = darkColorScheme("#6750A4")
val (light, dark) = colorSchemes("#6750A4")
```

## Targets

| Platform | Target |
|---|---|
| JVM | `jvm` (Java 17+) |
| JavaScript | `js(IR)` -- browser + Node.js |
| macOS | `macosX64`, `macosArm64` |
| iOS | `iosX64`, `iosArm64`, `iosSimulatorArm64` |
| Linux | `linuxX64` |
| Windows | `mingwX64` |

## Building

```bash
# Build all targets
./gradlew build

# Run tests (all platforms)
./gradlew allTests

# Run tests (JVM only -- fast feedback)
./gradlew :kolor-seed-core:jvmTest

# Publish to local Maven
./gradlew publishToMavenLocal
```

## Design Principles

1. **Zero dependencies** -- Core module depends only on `kotlin-stdlib`. No reflection, no serialization libraries, no platform APIs.
2. **Pure functions** -- Every function is side-effect-free, thread-safe, and deterministic.
3. **Performance** -- `@JvmInline value class` for Color, tone caching in TonalPalette, `inline` functions on hot paths.
4. **Material 3 fidelity** -- Output matches Google's [material-color-utilities](https://github.com/material-foundation/material-color-utilities) within rounding tolerance.

## License

```
Copyright 2026 nunoikeno

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0
```

See [LICENSE](LICENSE) for the full text.
