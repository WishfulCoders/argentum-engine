package com.wingedsheep.mtg.sets

import com.wingedsheep.sdk.tooling.CardExporter
import com.wingedsheep.sdk.tooling.CardLoader
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Golden snapshot of every registered card's compiled effect tree (Lesson 2,
 * `backlog/phase-rs-lessons.md`).
 *
 * For each set we serialize every [com.wingedsheep.sdk.model.CardDefinition] to canonical JSON via
 * [CardExporter] and assert it against a committed golden at
 * `src/test/resources/snapshots/cards/<code>.json`. Any SDK change that alters a card's lowered tree
 * — even on a card with no scenario test — shows up as a precise per-card diff instead of shipping
 * silently. This is the corpus-wide regression net the FacadeBoundary / `Condition`-unification kind
 * of refactor lacked.
 *
 * **Re-blessing.** After an *intentional* SDK change, review the failing diff (the fresh output is
 * written under `build/snapshots-actual/`), then regenerate the goldens:
 *
 * ```
 * ./gradlew :mtg-sets:test --tests "*CardDefinitionSnapshotTest" -DupdateSnapshots=true
 * ```
 *
 * A green re-bless with an expected diff is the normal workflow — see [GoldenSnapshot].
 *
 * **Determinism.** Ability ids are minted per card by
 * [com.wingedsheep.sdk.scripting.AbilityIdScope] (`"Card Name:N"` in construction order), so the raw
 * tree is already stable across runs; cards are sorted by name so set iteration order never leaks
 * into the file either.
 *
 * The second test round-trips every card through [CardLoader.fromJsonPreservingIds] — a corpus-wide check of the
 * *decode* path (export → load → re-export) that the hand-picked round-trip tests only spot-check.
 * It is the acceptance net for `CompactJsonTransformer`'s schema-driven expand: any new polymorphic
 * SDK field that the compactor shrinks but the expander can't restore fails here for the whole
 * corpus, not silently on one card.
 */
class CardDefinitionSnapshotTest : FunSpec({

    MtgSetCatalog.all.forEach { set ->
        val sorted = set.cards.sortedBy { it.name }

        test("${set.code} (${set.displayName}): card trees match golden") {
            val actual = sorted.joinToString("\n\n") { card ->
                "// ${card.name}\n${CardExporter.exportToJson(card)}"
            }
            GoldenSnapshot.verify("snapshots/cards/${GoldenSnapshot.fileSafe(set.code)}.json", actual)
        }

        test("${set.code} (${set.displayName}): cards survive a JSON round-trip") {
            for (card in sorted) {
                val exported = CardExporter.exportToJson(card)
                val reExported = CardExporter.exportToJson(CardLoader.fromJsonPreservingIds(exported))
                reExported shouldBe exported
            }
        }
    }
})

