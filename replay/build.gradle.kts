plugins {
    id("buildsrc.convention.kotlin-jvm")
    alias(libs.plugins.kotlinPluginSerialization)
    application
}

dependencies {
    // Rebuilds real games from 17Lands per-turn replay summaries by searching engine action
    // sequences that reproduce each recorded end-of-turn snapshot.
    implementation(project(":rules-engine"))
    implementation(project(":mtg-sdk"))
    implementation(project(":mtg-sets"))
    implementation(project(":ai"))
    implementation(project(":gym"))

    implementation(libs.bundles.kotlinxEcosystem)
    runtimeOnly(libs.slf4jApi)

    testImplementation(libs.kotestRunner)
    testImplementation(libs.kotestAssertions)
}

application {
    mainClass.set("com.wingedsheep.replay.MainKt")
    applicationDefaultJvmArgs = listOf("-Xmx8g")
}
