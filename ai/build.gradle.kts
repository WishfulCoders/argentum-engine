plugins {
    id("buildsrc.convention.kotlin-jvm")
    alias(libs.plugins.kotlinPluginSerialization)
}

dependencies {
    implementation(project(":rules-engine"))
    implementation(project(":mtg-sdk"))

    implementation(libs.bundles.kotlinxEcosystem)
    compileOnly(libs.slf4jApi)

    // slf4j is compileOnly above (the consuming runtime — game-server — supplies a binding); the
    // module's own tests exercise code paths that initialize loggers, so they need the API present.
    testImplementation(libs.slf4jApi)
    testImplementation(testFixtures(project(":rules-engine")))
    testImplementation(project(":mtg-sets"))
    testImplementation(libs.kotestRunner)
    testImplementation(libs.kotestAssertions)
    testImplementation(libs.kotestProperty)
    testImplementation(kotlin("reflect"))
}

// Production engine code drops Kotlin's runtime null checks on parameters, receivers and platform
// call results. Kotlin callers are null-checked at compile time, so the checks only ever fire for a
// Java caller passing null, and they cost ~3 % of an AI game: GameState's constructor alone checks
// ~100 arguments on every copy (mtg-draft-ai `docs/56` §3 item 7). Tests keep them.
tasks.named<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>("compileKotlin") {
    compilerOptions.freeCompilerArgs.addAll("-Xno-param-assertions", "-Xno-call-assertions", "-Xno-receiver-assertions")
}
