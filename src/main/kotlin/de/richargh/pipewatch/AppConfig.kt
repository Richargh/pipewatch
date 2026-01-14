package de.richargh.pipewatch

class AppConfig {
    val version: String = javaClass.`package`?.implementationVersion ?: "dev"
}
