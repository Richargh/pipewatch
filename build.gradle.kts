import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ktlint)
}

group = "com.gitlab.pipeline.monitor"
version = "1.0.0"

dependencies {
    // Compose Multiplatform (includes built-in Tray support)
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)

    // Serialization
    implementation(libs.kotlinx.serialization.json)

    // Coroutines
    implementation(libs.kotlinx.coroutines.core)

    // Testing
    testImplementation(kotlin("test"))
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.kotlinx.coroutines.test)
}

kotlin {
    jvmToolchain(17)
}

tasks.test {
    useJUnitPlatform()
}

ktlint {
    version.set(libs.versions.ktlint.runtime)
    android.set(false)
    outputToConsole.set(true)
    ignoreFailures.set(false)
}

compose.desktop {
    application {
        mainClass = "de.richargh.pipeline.monitor.MainKt"

        // Hide dock icon during development on macOS
        jvmArgs += "-Dapple.awt.UIElement=true"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "GitLab Pipeline Monitor"
            packageVersion = "1.0.0"

            macOS {
                iconFile.set(project.file("src/main/resources/icon.icns"))
                // Hide dock icon - this is a menu bar only app
                infoPlist {
                    extraKeysRawXml = """
                        <key>LSUIElement</key>
                        <true/>
                    """
                }
            }
            windows {
                iconFile.set(project.file("src/main/resources/icon.ico"))
            }
        }
    }
}
