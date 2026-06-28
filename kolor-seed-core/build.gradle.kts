plugins {
    alias(libs.plugins.kotlin.multiplatform)
    `maven-publish`
}

group = "com.github.ikeno-web.kolor-seed"
version = "0.1.1"

kotlin {
    explicitApi()

    jvm {
        compilations.all {
            kotlinOptions.jvmTarget = "17"
        }
    }

    js(IR) {
        browser {
            testTask {
                useKarma {
                    useChromeHeadless()
                }
            }
        }
        nodejs()
    }

    // Native targets
    macosX64()
    macosArm64()
    iosX64()
    iosArm64()
    iosSimulatorArm64()
    linuxX64()
    mingwX64()

    sourceSets {
        commonMain {
            dependencies {
                // Zero external dependencies — only kotlin-stdlib (implicit)
            }
        }
        commonTest {
            dependencies {
                implementation(libs.kotlin.test)
            }
        }
    }
}
