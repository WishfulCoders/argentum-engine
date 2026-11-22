package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.leg.cards.SwordOfTheAges
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Sword of the Ages (LEG #296) — "{T}, Sacrifice this artifact and any number of creatures you
 * control: This artifact deals X damage to any target, where X is the total power of the creatures
 * sacrificed this way, then exile this artifact and those creature cards."
 */
class SwordOfTheAgesScenarioTest : ScenarioTestBase() {

    init {
        val abilityId = SwordOfTheAges.activatedAbilities.single().id

        fun board() = scenario()
            .withPlayers("P1", "P2")
            .withCardOnBattlefield(1, "Sword of the Ages")
            .withCardOnBattlefield(1, "Grizzly Bears")
            .withCardOnBattlefield(1, "Hill Giant")
            .withCardInHand(1, "Giant Growth")
            .withLandsOnBattlefield(1, "Forest", 1)
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()

        fun TestGame.swing(picks: List<EntityId>) {
            val result = execute(
                ActivateAbility(
                    player1Id, findPermanent("Sword of the Ages")!!, abilityId,
                    targets = listOf(ChosenTarget.Player(player2Id)),
                    costPayment = AdditionalCostPayment(variableCostPermanents = picks)
                )
            )
            withClue("${result.error}") { result.error shouldBe null }
            resolveStack()
        }

        test("deals the sacrificed creatures' total power, then exiles the Sword and those cards") {
            val game = board()
            game.swing(listOf(game.findPermanent("Grizzly Bears")!!, game.findPermanent("Hill Giant")!!))
            game.getLifeTotal(2) shouldBe 15
            game.isInExile(1, "Sword of the Ages") shouldBe true
            game.isInExile(1, "Grizzly Bears") shouldBe true
            game.isInExile(1, "Hill Giant") shouldBe true
            game.isInGraveyard(1, "Grizzly Bears") shouldBe false
        }

        test("X is the power the creatures last had on the battlefield") {
            val game = board()
            val bears = game.findPermanent("Grizzly Bears")!!
            game.castSpell(1, "Giant Growth", bears)
            game.resolveStack()
            game.swing(listOf(bears))
            game.getLifeTotal(2) shouldBe 15
            game.isInExile(1, "Grizzly Bears") shouldBe true
            game.findPermanent("Hill Giant") shouldNotBe null
        }

        test("sacrificing no creatures deals 0 and still exiles the Sword") {
            val game = board()
            game.swing(emptyList())
            game.getLifeTotal(2) shouldBe 20
            game.isInExile(1, "Sword of the Ages") shouldBe true
            game.findPermanent("Grizzly Bears") shouldNotBe null
        }

        test("choosing no creatures at the prompt is accepted once, then asks for the target") {
            val game = board()
            game.execute(ActivateAbility(game.player1Id, game.findPermanent("Sword of the Ages")!!, abilityId))
                .error shouldBe null
            game.selectCards(emptyList()).error shouldBe null
            // The answer "none" sticks: the next prompt is the target, not the same picker again.
            game.state.pendingDecision shouldNotBe null
            game.selectTargets(listOf(game.player2Id)).error shouldBe null
            game.resolveStack()
            game.isInExile(1, "Sword of the Ages") shouldBe true
            game.getLifeTotal(2) shouldBe 20
        }

        test("enters tapped") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardInHand(1, "Sword of the Ages")
                .withLandsOnBattlefield(1, "Forest", 6)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            game.castSpell(1, "Sword of the Ages")
            game.resolveStack()
            val sword = game.findPermanent("Sword of the Ages")!!
            game.state.getEntity(sword)?.get<TappedComponent>() shouldNotBe null
        }
    }
}
