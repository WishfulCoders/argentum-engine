package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Persist (MH2 #96) — "Return target nonlegendary creature card from your graveyard to the
 * battlefield with a -1/-1 counter on it."
 */
class PersistScenarioTest : ScenarioTestBase() {
    init {
        test("returns a nonlegendary creature card with a -1/-1 counter") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardInHand(1, "Persist")
                .withLandsOnBattlefield(1, "Swamp", 2)
                .withCardInGraveyard(1, "Grizzly Bears")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpellTargetingGraveyardCard(1, "Persist", 1, "Grizzly Bears").error shouldBe null
            game.resolveStack()

            val bears = game.findPermanent("Grizzly Bears")
            withClue("Bears returned") { bears shouldNotBe null }
            game.state.getEntity(bears!!)!!.get<CountersComponent>()!!
                .getCount(CounterType.MINUS_ONE_MINUS_ONE) shouldBe 1
            game.state.projectedState.getPower(bears) shouldBe 1
            game.state.projectedState.getToughness(bears) shouldBe 1
        }

        test("a legendary creature card is not a legal target") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardInHand(1, "Persist")
                .withLandsOnBattlefield(1, "Swamp", 2)
                .withCardInGraveyard(1, "Jadar, Ghoulcaller of Nephalia")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val result = game.castSpellTargetingGraveyardCard(1, "Persist", 1, "Jadar, Ghoulcaller of Nephalia")
            withClue("cast is rejected") { (result.error != null) shouldBe true }
            game.isInGraveyard(1, "Jadar, Ghoulcaller of Nephalia") shouldBe true
        }
    }
}
