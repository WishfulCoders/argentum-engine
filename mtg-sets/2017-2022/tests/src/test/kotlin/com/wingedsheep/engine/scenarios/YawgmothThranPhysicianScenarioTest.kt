package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.mh1.cards.YawgmothThranPhysician
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Yawgmoth, Thran Physician (MH1 #116) — "Pay 1 life, Sacrifice another creature: Put a -1/-1
 * counter on up to one target creature and draw a card."
 */
class YawgmothThranPhysicianScenarioTest : ScenarioTestBase() {
    init {
        val abilityId = YawgmothThranPhysician.activatedAbilities.first().id

        fun board() = scenario()
            .withPlayers("P1", "P2")
            .withCardOnBattlefield(1, "Yawgmoth, Thran Physician")
            .withCardOnBattlefield(1, "Grizzly Bears")
            .withCardOnBattlefield(2, "Hill Giant")
            .withCardInLibrary(1, "Swamp")
            .withCardInLibrary(1, "Swamp")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()

        test("with a target: pays 1 life, sacrifices, puts a -1/-1 counter and draws") {
            val game = board()
            val hand = game.handSize(1)
            val giant = game.findPermanent("Hill Giant")!!
            val r = game.execute(
                ActivateAbility(
                    game.player1Id, game.findPermanent("Yawgmoth, Thran Physician")!!, abilityId,
                    targets = listOf(ChosenTarget.Permanent(giant)),
                    costPayment = AdditionalCostPayment(sacrificedPermanents = listOf(game.findPermanent("Grizzly Bears")!!)),
                )
            )
            withClue("${r.error}") { r.error shouldBe null }
            game.resolveStack()

            game.isInGraveyard(1, "Grizzly Bears") shouldBe true
            game.getLifeTotal(1) shouldBe 19
            game.state.getEntity(giant)!!.get<CountersComponent>()!!
                .getCount(CounterType.MINUS_ONE_MINUS_ONE) shouldBe 1
            game.handSize(1) shouldBe hand + 1
        }

        test("with no target chosen it still draws a card") {
            val game = board()
            val hand = game.handSize(1)
            val r = game.execute(
                ActivateAbility(
                    game.player1Id, game.findPermanent("Yawgmoth, Thran Physician")!!, abilityId,
                    costPayment = AdditionalCostPayment(sacrificedPermanents = listOf(game.findPermanent("Grizzly Bears")!!)),
                )
            )
            withClue("${r.error}") { r.error shouldBe null }
            game.resolveStack()

            game.getLifeTotal(1) shouldBe 19
            game.handSize(1) shouldBe hand + 1
        }
    }
}
