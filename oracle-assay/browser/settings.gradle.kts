pluginManagement { repositories { gradlePluginPortal(); mavenCentral() } }
dependencyResolutionManagement {
    repositories { mavenCentral() }
    // Same Kotlin and serialization versions as the JVM build that owns the shared sources.
    versionCatalogs { create("libs") { from(files("../../gradle/libs.versions.toml")) } }
}
rootProject.name = "assay-oracle"
