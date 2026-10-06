package com.wingedsheep.engine.mechanics.zones

import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * `Effects.PutIntoLibraryBeneathTop(target, cardsAbove)` — "put target permanent into its owner's
 * library just beneath the top X cards of that library" (`MoveToZoneEffect.positionFromTopAmount`).
 * The position is evaluated on resolution; 0 is the top, and more than the library holds is the
 * bottom (Unexpectedly Absent rulings, CR 401.4 for "a specific position").
 */
class PutIntoLibraryBeneathTopTest : ScenarioTestBase() {
    init {
        cardRegistry.register(card("Beneath Test Spell") {
            manaCost = "{X}"
            typeLine = "Instant"
            spell {
                effect = Effects.PutIntoLibraryBeneathTop(target(TargetFilter.NonlandPermanent), DynamicAmounts.xValue())
            }
        })

        fun TestGame.library2() = state.getLibrary(player2Id).map {
            state.getEntity(it)!!.get<CardComponent>()!!.name
        }

        fun board() = scenario().withPlayers()
            .withCardInHand(1, "Beneath Test Spell")
            .withLandsOnBattlefield(1, "Plains", 5)
            .withCardOnBattlefield(2, "Hill Giant")
            .withCardInLibrary(2, "Forest")
            .withCardInLibrary(2, "Island")
            .withCardInLibrary(2, "Swamp")
            .withCardInLibrary(1, "Plains")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)

        for ((x, expectedIndex) in listOf(0 to 0, 1 to 1, 2 to 2, 3 to 3, 5 to 3)) {
            test("X = $x puts it at index $expectedIndex of its owner's three-card library") {
                val game = board().build()
                val before = game.library2()
                val r = game.castXSpell(1, "Beneath Test Spell", x, game.findPermanent("Hill Giant")!!)
                withClue("${r.error}") { r.error shouldBe null }
                game.resolveStack()
                val after = game.library2()
                after.size shouldBe 4
                after[expectedIndex] shouldBe "Hill Giant"
                after.filter { it != "Hill Giant" } shouldBe before
            }
        }
    }
}
