package com.wingedsheep.ai.engine

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.eld.cards.RunAwayTogether
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * A single requirement for several targets with `differentControllers` ("two target creatures
 * controlled by different players" — Run Away Together) must be filled with that many targets,
 * each under a different controller. The AI used to pick one target per requirement, which the
 * engine rejects, so it could never cast the spell.
 */
class DifferentControllersTargetFillAiTest : FunSpec({

    test("the AI fills both targets from different controllers") {
        val registry = CardRegistry().apply { register(TestCards.all + RunAwayTogether) }
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + RunAwayTogether)
        val players = driver.initMultiplayer(decks = List(3) { Deck.of("Island" to 40) })
        val (player, opponentA, opponentB) = players
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        // Opponent A holds the two best targets, so a plain best-first pick would take both.
        driver.putCreatureOnBattlefield(opponentA, "Hill Giant")
        driver.putCreatureOnBattlefield(opponentA, "Hill Giant")
        val bearsB = driver.putCreatureOnBattlefield(opponentB, "Grizzly Bears")
        val spell = driver.putCardInHand(player, "Run Away Together")
        driver.giveMana(player, Color.BLUE, 2)

        val cast = GameSimulator(registry).getLegalActions(driver.state, player)
            .first { (it.action as? CastSpell)?.cardId == spell }
        val filled = TargetSelection.fillHeuristically(driver.state, cast, player, fillPartialRequirements = true)

        val targets = (filled as CastSpell).targets.map { (it as ChosenTarget.Permanent).entityId }
        targets.size shouldBe 2
        targets.contains(bearsB) shouldBe true
        targets.count { driver.state.projectedState.getController(it) == opponentA } shouldBe 1
        driver.submit(filled).outcome shouldBe Outcome.Done
    }
})
