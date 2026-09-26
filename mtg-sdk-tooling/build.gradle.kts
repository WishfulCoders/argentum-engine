plugins {
    id("buildsrc.convention.kotlin-jvm")
    alias(libs.plugins.kotlinPluginSerialization)
    alias(libs.plugins.kover)
}

// Tooling *over* the SDK's data, kept out of the SDK itself so `:mtg-sdk` stays the pure card
// vocabulary: the card-JSON file format (export, load, the compact transform and its filter query
// language) and the static checks run against a finished definition (validator, linter). Nothing
// here is on the path a game takes — the engine never depends on it.
dependencies {
    api(project(":mtg-sdk"))
    implementation(libs.bundles.kotlinxEcosystem)

    testImplementation(libs.kotestRunner)
    testImplementation(libs.kotestAssertions)
}
