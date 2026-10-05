package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.AlternativeCostType
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.all.cards.Pyrokinesis
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Pyrokinesis (ALL #78) — {4}{R}{R} Instant.
 *
 *   You may exile a red card from your hand rather than pay this spell's mana cost.
 *   Pyrokinesis deals 4 damage divided as you choose among any number of target creatures.
 *
 * Pins the pitch cost (a red card, not any card, on any turn) and the cast-time division.
 */
class PyrokinesisScenarioTest : ScenarioTestBase() {

    private fun board(pitch: String): TestGame = scenario()
        .withPlayers("Caster", "Opponent")
        .withCardInHand(1, "Pyrokinesis")
        .withCardInHand(1, pitch)
        .withCardOnBattlefield(2, "Llanowar Elves")
        .withCardOnBattlefield(2, "Centaur Courser")
        .withCardOnBattlefield(2, "Savannah Lions")
        .withActivePlayer(2)
        .withPriorityPlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        cardRegistry.register(Pyrokinesis)

        test("exiling a red card casts it for free; 1 and 3 damage kill a 1/1 and a 3/3") {
            val game = board(pitch = "Lightning Bolt")
            val spell = game.findCardsInHand(1, "Pyrokinesis").single()
            val bolt = game.findCardsInHand(1, "Lightning Bolt").single()
            val elves = game.findPermanent("Llanowar Elves")!!
            val courser = game.findPermanent("Centaur Courser")!!

            game.execute(
                CastSpell(
                    game.player1Id, spell,
                    targets = listOf(ChosenTarget.Permanent(elves), ChosenTarget.Permanent(courser)),
                    useAlternativeCost = true,
                    alternativeCostType = AlternativeCostType.SELF_ALTERNATIVE,
                    additionalCostPayment = AdditionalCostPayment(exiledCards = listOf(bolt)),
                    damageDistribution = mapOf(elves to 1, courser to 3),
                )
            ).error shouldBe null

            withClue("the red card is exiled as the cost") {
                game.isInExile(1, "Lightning Bolt") shouldBe true
            }

            game.resolveStack()

            withClue("both targets die; the untargeted Lions survive") {
                game.isInGraveyard(2, "Llanowar Elves") shouldBe true
                game.isInGraveyard(2, "Centaur Courser") shouldBe true
                game.isOnBattlefield("Savannah Lions") shouldBe true
            }
        }

        test("a non-red card can't pay the alternative cost") {
            val game = board(pitch = "Giant Growth")
            val spell = game.findCardsInHand(1, "Pyrokinesis").single()
            val growth = game.findCardsInHand(1, "Giant Growth").single()
            val elves = game.findPermanent("Llanowar Elves")!!

            game.execute(
                CastSpell(
                    game.player1Id, spell,
                    targets = listOf(ChosenTarget.Permanent(elves)),
                    useAlternativeCost = true,
                    alternativeCostType = AlternativeCostType.SELF_ALTERNATIVE,
                    additionalCostPayment = AdditionalCostPayment(exiledCards = listOf(growth)),
                    damageDistribution = mapOf(elves to 4),
                )
            ).error shouldNotBe null

            game.isInHand(1, "Giant Growth") shouldBe true
            game.isOnBattlefield("Llanowar Elves") shouldBe true
        }
    }
}
