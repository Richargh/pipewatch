import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.conveyor)
    alias(libs.plugins.ktlint)
}

group = "de.richargh.pipewatch"
version = System.getenv("VERSION")?.trimStart('v') ?: "SNAPSHOT"

// Workaround for Compose Desktop skiko variant resolution
// https://github.com/JetBrains/compose-jb/issues/1404
configurations.matching { it.isCanBeResolved }.configureEach {
    attributes {
        attribute(Attribute.of("ui", String::class.java), "awt")
    }
}

dependencies {
    // Compose Multiplatform (includes built-in Tray support)
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    implementation(compose.materialIconsExtended)

    // Conveyor cross-platform dependencies (configurations created by Conveyor plugin)
    "macAmd64"(compose.desktop.macos_x64)
    "macAarch64"(compose.desktop.macos_arm64)
    "windowsAmd64"(compose.desktop.windows_x64)
    "linuxAmd64"(compose.desktop.linux_x64)

    // Serialization
    implementation(libs.kotlinx.serialization.json)

    // Coroutines
    implementation(libs.kotlinx.coroutines.core)

    // Ktor Client for GitLab API
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)

    // Settings persistence
    implementation(libs.multiplatform.settings)

    // Testing
    testImplementation(kotlin("test"))
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.ktor.client.mock)
    testImplementation(libs.multiplatform.settings.test)
}

kotlin {
    jvmToolchain(21)
}

tasks.test {
    useJUnitPlatform()
    dependsOn("ktlintCheck")
}

tasks.withType<Jar> {
    manifest {
        attributes(
            "Implementation-Version" to project.version,
        )
    }
}

ktlint {
    version.set(libs.versions.ktlint.runtime)
    android.set(false)
    outputToConsole.set(true)
    ignoreFailures.set(false)
}

compose.desktop {
    application {
        mainClass = "de.richargh.pipewatch.MainKt"

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
