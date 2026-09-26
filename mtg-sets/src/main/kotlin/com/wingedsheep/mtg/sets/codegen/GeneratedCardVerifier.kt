package com.wingedsheep.mtg.sets.codegen

import com.wingedsheep.mtg.sets.discovery.CardDiscovery
import com.wingedsheep.sdk.tooling.CardExporter
import java.io.File

/**
 * Compile-verification gate for the mtgish auto-generator.
 *
 * The `:mtgish-tooling` emitter writes draft `.kt` for a set into the
 * `generatedCards` source set; Gradle compiles them (so a draft that doesn't compile fails the
 * build before we get here). This tool then loads each compiled card via the same reflective
 * [CardDiscovery] the real sets use and serialises it with the same [CardExporter] that produces
 * the committed golden snapshots — writing the result in golden format
 * (`// Name` + compiled JSON) to an output file.
 *
 * A small Python step (`fidelity.py --gate`) then diffs those serialised trees against the golden
 * snapshot using the *same* capability function applied to both sides. Net effect: AUTO stops being
 * a static-tag prediction and becomes "this generated card actually compiles and serialises to the
 * capabilities the hand-authored card has".
 *
 * args: <generatedPackage> <outputJsonPath>
 */
fun main(args: Array<String>) {
    val pkg = args.getOrElse(0) { "com.wingedsheep.mtg.sets.generated.por.cards" }
    val outPath = args.getOrElse(1) { "build/generated-cards/generated.json" }

    val cards = (CardDiscovery.findIn(pkg) + CardDiscovery.findBasicLandsIn(pkg))
        .distinctBy { it.name }
        .sortedBy { it.name }

    val text = buildString {
        for (card in cards) {
            append("// ").append(card.name).append('\n')
            append(CardExporter.exportToJson(card)).append("\n\n")
        }
    }
    File(outPath).apply { parentFile?.mkdirs() }.writeText(text)
    System.err.println("verifyGeneratedCards: serialised ${cards.size} generated card(s) from $pkg -> $outPath")
}
