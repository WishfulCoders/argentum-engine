plugins {
    id("buildsrc.convention.kotlin-jvm")
    alias(libs.plugins.kotlinPluginSerialization)
    application
}

dependencies {
    // mtg-draft-ai docs/65 §3 (E1): the search loop, featurizer and policy net inside the JVM, over :gym.
    implementation(project(":gym"))
    implementation(project(":rules-engine"))
    implementation(project(":mtg-sdk"))
    implementation(project(":mtg-sets"))
    implementation(project(":ai"))

    implementation(libs.bundles.kotlinxEcosystem)
    implementation("com.microsoft.onnxruntime:onnxruntime:1.30.0")
    runtimeOnly(libs.slf4jApi)

    testImplementation(libs.kotestRunner)
    testImplementation(libs.kotestAssertions)
}

application {
    mainClass.set("com.wingedsheep.gym.exit.MainKt")
    applicationDefaultJvmArgs = listOf("-Xss16m")
}

tasks.withType<Test>().configureEach {
    // Parity fixtures live outside the repo (mtg-draft-ai records them); tests skip when absent.
    for (prop in listOf("e1.parity", "e1.model", "e1.deals", "argentum.ai.apprentice.dir")) System.getProperty(prop)?.let { systemProperty(prop, it) }
    maxHeapSize = "4g"
}
