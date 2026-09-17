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
    // C1 (mtg-draft-ai docs/27 §5): a broken game played on with the arena's game loop and pilot profiles.
    implementation(project(":arena"))

    implementation(libs.bundles.kotlinxEcosystem)
    runtimeOnly(libs.slf4jApi)

    testImplementation(libs.kotestRunner)
    testImplementation(libs.kotestAssertions)
}

application {
    mainClass.set("com.wingedsheep.replay.MainKt")
    applicationDefaultJvmArgs = listOf("-Xmx8g")
}
