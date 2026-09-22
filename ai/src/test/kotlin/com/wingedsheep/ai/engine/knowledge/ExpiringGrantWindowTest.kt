package com.wingedsheep.ai.engine.knowledge

import com.wingedsheep.ai.puzzles.advanceToDeclaration
import com.wingedsheep.ai.puzzles.advanceToPriority
import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.ecl.cards.Kithkeeper
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.matchers.shouldBe

/**
 * `expiringGrantsNeedACombat`'s blocker floor and its lethal exception, read off the verdict
 * directly.
 *
 * `PuzzleSuiteTest` cannot show the exception: at `instants-22` the floor lets the lethal pump
 * through and the one-ply leaf then declines it on its own (it scores the state before combat
 * damage — `instants-05`'s shape). So the claim "the floor does not stand in the way of lethal" is
 * pinned here, where no leaf is involved.
 *
 * One position, three life totals: Kithkeeper attacking and unblocked, exactly three Bears at home
 * to pay the cost with, a Hill Giant across the table.
 */
class ExpiringGrantWindowTest : ScenarioTestBase() {

    private val intents by lazy { IntentCatalog.of(cardRegistry) }
    private val ability = Kithkeeper.activatedAbilities.single()

    private fun holdsAt(opponentLife: Int): Boolean {
        val game = scenario()
            .withPlayers()
            .withTurnNumber(16)
            .withLifeTotal(2, opponentLife)
            .withCardOnBattlefield(1, "Kithkeeper")
            .withCardOnBattlefield(1, "Grizzly Bears")
            .withCardOnBattlefield(1, "Grizzly Bears")
            .withCardOnBattlefield(1, "Grizzly Bears")
            .withCardOnBattlefield(2, "Hill Giant")
            .build()
            .advanceToDeclaration(1, Step.DECLARE_ATTACKERS)
            .also { it.declareAttackers(mapOf("Kithkeeper" to 2)) }
            .advanceToDeclaration(2, Step.DECLARE_BLOCKERS)
            .also { it.declareBlockers(mapOf()) }
            .advanceToPriority(1, Step.DECLARE_BLOCKERS)

        val activation = ActivateAbility(
            playerId = game.player1Id,
            sourceId = game.findPermanent("Kithkeeper")!!,
            abilityId = ability.id,
            costPayment = AdditionalCostPayment(tappedPermanents = game.findAllPermanents("Grizzly Bears")),
        )
        return ExpiringGrantWindow.holds(
            game.state, game.player1Id, ability, intents, activation, needsACombat = true,
        )
    }

    init {
        test("tapping every blocker for damage that isn't lethal is floored") {
            holdsAt(opponentLife = 20) shouldBe true
        }

        test("tapping every blocker for exactly lethal is let through") {
            // 3 + 3 = 6 unblocked into 6.
            holdsAt(opponentLife = 6) shouldBe false
        }

        test("one short of lethal is still floored") {
            holdsAt(opponentLife = 7) shouldBe true
        }
    }
}
