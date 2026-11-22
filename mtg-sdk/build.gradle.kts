plugins {
    id("buildsrc.convention.kotlin-jvm")
    alias(libs.plugins.kotlinPluginSerialization)
    alias(libs.plugins.kover)
}

dependencies {
    implementation(libs.bundles.kotlinxEcosystem)

    testImplementation(libs.kotestRunner)
    testImplementation(libs.kotestAssertions)
    testImplementation(libs.kotestProperty)
    // CostAtomSerializationTest enumerates sealed subclasses via kotlin-reflect (sealedSubclasses).
    testImplementation(kotlin("reflect"))
}

// Production engine code drops Kotlin's runtime null checks on parameters, receivers and platform
// call results. Kotlin callers are null-checked at compile time, so the checks only ever fire for a
// Java caller passing null, and they cost ~3 % of an AI game: GameState's constructor alone checks
// ~100 arguments on every copy (mtg-draft-ai `docs/56` §3 item 7). Tests keep them.
tasks.named<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>("compileKotlin") {
    compilerOptions.freeCompilerArgs.addAll("-Xno-param-assertions", "-Xno-call-assertions", "-Xno-receiver-assertions")
}

// `just sdk-tail` runs SdkTailReport, which is skipped unless `sdkTail` reaches the test JVM;
// `just sdk-index` rewrites docs/sdk-index.md through SdkIndexTest with `updateSdkIndex`.
tasks.withType<Test>().configureEach {
    for (prop in listOf("sdkTail", "updateSdkIndex")) {
        System.getProperty(prop)?.let { systemProperty(prop, it) }
    }
    // SdkIndexTest compares the KDoc in the source against the checked-in index; neither reaches the
    // compiled classes, so declare both or a KDoc-only edit leaves the test up to date and green.
    inputs.dir("src/main/kotlin").withPropertyName("sdkSource").withPathSensitivity(PathSensitivity.RELATIVE)
    inputs.file("../docs/sdk-index.md").withPropertyName("sdkIndex").withPathSensitivity(PathSensitivity.RELATIVE)
}
