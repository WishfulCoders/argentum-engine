package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.conflux.cards.NobleHierarch
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Noble Hierarch (Conflux) — the first card that *prints* exalted (CR 702.83a): the engine derives
 * one "attacks alone" trigger per instance from the projected keyword, so two Hierarchs pump a lone
 * attacker twice, and a two-creature attack triggers neither.
 */
class NobleHierarchScenarioTest : FunSpec({
    fun driver(): GameTestDriver = GameTestDriver().also {
        it.registerCards(TestCards.all + NobleHierarch)
        it.initMirrorMatch(Deck.of("Forest" to 40), skipMulligans = true, startingPlayer = 0)
        it.passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    test("each Hierarch's exalted pumps a creature that attacks alone +1/+1 until end of turn") {
        val d = driver()
        d.putCreatureOnBattlefield(d.player1, "Noble Hierarch")
        d.putCreatureOnBattlefield(d.player1, "Noble Hierarch")
        val bears = d.putCreatureOnBattlefield(d.player1, "Grizzly Bears")
        d.removeSummoningSickness(bears)

        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(d.player1, listOf(bears), d.player2).error shouldBe null
        withClue("one exalted trigger per Hierarch") { d.state.stack.size shouldBe 2 }
        while (d.state.stack.isNotEmpty()) d.bothPass()

        d.state.projectedState.getPower(bears) shouldBe 4
        d.state.projectedState.getToughness(bears) shouldBe 4
    }

    test("attacking with two creatures doesn't trigger exalted") {
        val d = driver()
        val hierarch = d.putCreatureOnBattlefield(d.player1, "Noble Hierarch")
        val bears = d.putCreatureOnBattlefield(d.player1, "Grizzly Bears")
        d.removeSummoningSickness(bears)
        d.removeSummoningSickness(hierarch)

        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(d.player1, listOf(bears, hierarch), d.player2).error shouldBe null
        d.state.stack.size shouldBe 0
        d.state.projectedState.getPower(bears) shouldBe 2
    }
})
