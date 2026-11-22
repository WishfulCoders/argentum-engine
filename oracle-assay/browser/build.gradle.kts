import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    kotlin("multiplatform") version libs.versions.kotlin.get()
    alias(libs.plugins.kotlinPluginSerialization)
}

// Standalone build: no JVM engine, web server, card catalogue, or corpus.
val upstream = projectDir.resolve("../..")
val generated = layout.buildDirectory.dir("generated/checker")
val portableSources = tasks.register("portableSources") {
    // Locals only: a script-level reference inside doLast breaks the configuration cache.
    val outDir = generated
    val sdk = fileTree(upstream.resolve("mtg-sdk/src/main/kotlin")) { include("**/*.kt") }
    val assay = fileTree(upstream.resolve("oracle-assay/src/main/kotlin")) {
        include("**/syntax/**", "**/grammar/**", "**/normalize/**", "**/compile/**")
        include("**/gate/Touchstone.kt", "**/corpus/OracleCard.kt", "**/corpus/ScryfallJson.kt")
    }
    val tooling = fileTree(upstream.resolve("mtg-sdk-tooling/src/main/kotlin")) {
        include("**/CardValidator.kt", "**/CardLinter.kt")
    }
    inputs.files(sdk, assay, tooling)
    outputs.dir(outDir)
    doLast {
        val out = outDir.get().asFile
        out.deleteRecursively()
        (sdk.files + assay.files + tooling.files).forEach { source ->
            // Adapt platform plumbing only; share grammar, normalization and validation verbatim.
            var text = source.readText().replace("@JvmInline", "")
            text = text.replace("import java.util.concurrent.atomic.AtomicInteger", "import com.wingedsheep.assay.browser.AtomicInteger")
            text = text.replace("import java.util.UUID", "import com.wingedsheep.assay.browser.UUID")
            if (text.contains("ThreadLocal")) {
                text = text.replaceFirst(Regex("(?m)^package .*$"), "$0\nimport com.wingedsheep.assay.browser.ThreadLocal")
            }
            require(!Regex("import java\\.|import javax\\.").containsMatchIn(text)) {
                "Unexpected platform dependency in ${source.name}; keep the browser boundary explicit."
            }
            val relative = source.invariantSeparatorsPath.substringAfter("/src/main/kotlin/")
            out.resolve(relative).apply { parentFile.mkdirs(); writeText(text) }
        }
    }
}

kotlin {
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs { browser(); binaries.executable() }
    sourceSets {
        wasmJsMain {
            kotlin.srcDir(generated)
            dependencies {
                implementation(libs.kotlinxSerialization)
            }
        }
    }
}
tasks.matching { it.name == "compileKotlinWasmJs" }.configureEach { dependsOn(portableSources) }
