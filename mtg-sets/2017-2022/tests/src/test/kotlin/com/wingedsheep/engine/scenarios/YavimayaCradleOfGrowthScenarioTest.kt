package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Yavimaya, Cradle of Growth — "Each land is a Forest in addition to its other land types."
 *
 * Proves the layer-4 grant reaches every land on the battlefield (both players', Yavimaya itself)
 * without stripping their other types, and that the derived Forest mana ability actually pays
 * for green mana — an Island alone can't cast a green spell.
 */
class YavimayaCradleOfGrowthScenarioTest : ScenarioTestBase() {

    init {
        context("Yavimaya, Cradle of Growth") {
            test("every land on the battlefield is a Forest in addition to its other types") {
                val game = scenario()
                    .withPlayers("Player1", "Opponent")
                    .withCardOnBattlefield(1, "Yavimaya, Cradle of Growth")
                    .withCardOnBattlefield(1, "Island")
                    .withCardOnBattlefield(2, "Mountain")
                    .withCardInGraveyard(1, "Swamp")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val projected = game.state.projectedState
                val yavimaya = game.findPermanent("Yavimaya, Cradle of Growth")!!
                val island = game.findPermanent("Island")!!
                val mountain = game.findPermanent("Mountain")!!

                withClue("Yavimaya is itself a Forest") { projected.hasSubtype(yavimaya, "Forest") shouldBe true }
                withClue("my Island is a Forest and still an Island") {
                    projected.hasSubtype(island, "Forest") shouldBe true
                    projected.hasSubtype(island, "Island") shouldBe true
                }
                withClue("the opponent's Mountain is a Forest too") {
                    projected.hasSubtype(mountain, "Forest") shouldBe true
                    projected.hasSubtype(mountain, "Mountain") shouldBe true
                }
            }

            test("Yavimaya and an Island together pay {G}{G}") {
                val game = scenario()
                    .withPlayers("Player1", "Opponent")
                    .withCardOnBattlefield(1, "Yavimaya, Cradle of Growth")
                    .withCardOnBattlefield(1, "Island")
                    .withCardInHand(1, "People of the Woods")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val cast = game.castSpell(1, "People of the Woods")
                withClue("Island taps for {G} as a Forest: ${cast.error}") { cast.error shouldBe null }
                game.resolveStack()
                game.isOnBattlefield("People of the Woods") shouldBe true
            }
        }
    }
}
