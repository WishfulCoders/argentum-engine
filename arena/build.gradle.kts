plugins {
    id("buildsrc.convention.kotlin-jvm")
    alias(libs.plugins.kotlinPluginSerialization)
    application
}

dependencies {
    // Plays real 17Lands decks against each other with the engine's default AI, many games in
    // parallel, one JSON line per game. The engine acceptance test (RL-MTG-drafts docs/21).
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
    mainClass.set("com.wingedsheep.arena.MainKt")
    applicationDefaultJvmArgs = listOf("-Xmx8g")
}
