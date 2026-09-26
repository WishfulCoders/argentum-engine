package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Null Summoner (Reality Fracture #142) — {2}{U}{B} Creature — Human Warlock 4/2:
 *   When this creature enters, if you cast it, target opponent reveals their hand. You choose a
 *   nonland card from it. Exile that card.
 *   Threshold — As long as there are seven or more cards in your graveyard, you may cast the exiled
 *   card, and mana of any type can be spent to cast that spell.
 */
class NullSummonerScenarioTest : ScenarioTestBase() {

    /** Cast Null Summoner, resolve it, and pick [chosen] from the opponent's revealed hand. */
    private fun TestGame.castSummonerAndExile(chosen: EntityId) {
        castSpell(1, "Null Summoner").error shouldBe null
        resolveStack()
        if (state.pendingDecision is ChooseTargetsDecision) {
            selectTargets(listOf(player2Id)).error shouldBe null
        }
        resolveStack()
        val decision = state.pendingDecision
        decision.shouldBeInstanceOf<SelectCardsDecision>()
        withClue("you choose among the nonland cards only") {
            decision.playerId shouldBe player1Id
            decision.options.toSet() shouldBe findCardsInHand(2, "Grizzly Bears").toSet() +
                findCardsInHand(2, "Giant Growth").toSet()
        }
        selectCards(listOf(chosen)).error shouldBe null
        resolveStack()
    }

    private fun TestGame.canCastFromExile(cardId: EntityId): Boolean =
        getLegalActions(1).any { (it.action as? CastSpell)?.cardId == cardId && it.isAffordable }

    init {
        test("the cast trigger exiles the chosen nonland card; below threshold it can't be cast") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Null Summoner")
                .withLandsOnBattlefield(1, "Island", 3)
                .withLandsOnBattlefield(1, "Swamp", 3)
                .withCardInHand(2, "Grizzly Bears")
                .withCardInHand(2, "Giant Growth")
                .withCardInHand(2, "Forest")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bears = game.findCardsInHand(2, "Grizzly Bears").first()
            game.castSummonerAndExile(bears)

            game.isOnBattlefield("Null Summoner") shouldBe true
            game.state.getExile(game.player2Id) shouldContain bears
            game.findCardsInHand(2, "Forest").size shouldBe 1
            game.findCardsInHand(2, "Giant Growth").size shouldBe 1

            withClue("no threshold yet: the exiled card is not castable") {
                game.canCastFromExile(bears) shouldBe false
            }
        }

        test("with threshold, the exiled card is cast with mana of any type and joins your battlefield") {
            val builder = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Null Summoner")
                .withLandsOnBattlefield(1, "Island", 3)
                .withLandsOnBattlefield(1, "Swamp", 3)
                .withCardInHand(2, "Grizzly Bears")
                .withCardInHand(2, "Giant Growth")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            repeat(7) { builder.withCardInGraveyard(1, "Swamp") }
            val game = builder.build()

            val bears = game.findCardsInHand(2, "Grizzly Bears").first()
            game.castSummonerAndExile(bears)

            withClue("threshold on: the {1}{G} card is castable from Islands and Swamps") {
                game.canCastFromExile(bears) shouldBe true
            }
            val result = game.execute(CastSpell(game.player1Id, bears))
            withClue("paying {1}{G} with blue/black mana: ${result.error}") {
                result.error shouldBe null
            }
            game.resolveStack()

            game.state.getBattlefield(game.player1Id) shouldContain bears
        }

        test("the permission ends when Null Summoner leaves the battlefield") {
            val builder = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Null Summoner")
                .withCardInHand(1, "Unsummon")
                .withLandsOnBattlefield(1, "Island", 4)
                .withLandsOnBattlefield(1, "Swamp", 3)
                .withCardInHand(2, "Grizzly Bears")
                .withCardInHand(2, "Giant Growth")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            repeat(7) { builder.withCardInGraveyard(1, "Swamp") }
            val game = builder.build()

            val bears = game.findCardsInHand(2, "Grizzly Bears").first()
            game.castSummonerAndExile(bears)
            game.canCastFromExile(bears) shouldBe true

            val summoner = game.findPermanent("Null Summoner")!!
            game.castSpell(1, "Unsummon", summoner).error shouldBe null
            game.resolveStack()

            game.isOnBattlefield("Null Summoner") shouldBe false
            game.state.getExile(game.player2Id) shouldContain bears
            game.canCastFromExile(bears) shouldBe false
        }
    }
}
