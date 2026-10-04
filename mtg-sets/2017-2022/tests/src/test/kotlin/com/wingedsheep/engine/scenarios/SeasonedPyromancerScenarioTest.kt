package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.mh1.cards.SeasonedPyromancer
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Seasoned Pyromancer (MH1 #145) — {1}{R}{R} Creature — Human Shaman 2/2.
 *
 *   When this creature enters, discard two cards, then draw two cards. For each nonland card
 *   discarded this way, create a 1/1 red Elemental creature token.
 *   {3}{R}{R}, Exile this card from your graveyard: Create two 1/1 red Elemental creature tokens.
 *
 * Pins the count: only the nonland discards make tokens, and the draw is a flat two; plus the
 * graveyard ability's exile-self cost.
 */
class SeasonedPyromancerScenarioTest : ScenarioTestBase() {

    init {
        cardRegistry.register(SeasonedPyromancer)

        test("discarding a land and a nonland draws two and makes one Elemental") {
            val game = scenario()
                .withPlayers("Pyro", "Opponent")
                .withCardInHand(1, "Seasoned Pyromancer")
                .withCardInHand(1, "Hill Giant")
                .withCardInHand(1, "Mountain")
                .withCardInLibrary(1, "Grizzly Bears")
                .withCardInLibrary(1, "Grizzly Bears")
                .withCardInLibrary(1, "Grizzly Bears")
                .withLandsOnBattlefield(1, "Mountain", 3)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Seasoned Pyromancer").error shouldBe null
            game.resolveStack()
            if (game.getPendingDecision() is SelectCardsDecision) {
                val toDiscard = game.findCardsInHand(1, "Hill Giant") + game.findCardsInHand(1, "Mountain")
                game.selectCards(toDiscard).error shouldBe null
                game.resolveStack()
            }

            withClue("both cards were discarded and two were drawn") {
                game.isInGraveyard(1, "Hill Giant") shouldBe true
                game.isInGraveyard(1, "Mountain") shouldBe true
                game.findCardsInHand(1, "Grizzly Bears").size shouldBe 2
            }
            withClue("one nonland card discarded — one Elemental token") {
                game.findPermanents("Elemental Token").size shouldBe 1
            }
        }

        test("the graveyard ability exiles the Pyromancer and makes two Elementals") {
            val game = scenario()
                .withPlayers("Pyro", "Opponent")
                .withCardInGraveyard(1, "Seasoned Pyromancer")
                .withLandsOnBattlefield(1, "Mountain", 5)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val pyro = game.findCardsInGraveyard(1, "Seasoned Pyromancer").single()
            val abilityId = cardRegistry.getCard("Seasoned Pyromancer")!!.script.activatedAbilities[0].id

            game.execute(ActivateAbility(game.player1Id, pyro, abilityId)).error shouldBe null
            game.isInExile(1, "Seasoned Pyromancer") shouldBe true
            game.resolveStack()

            game.findPermanents("Elemental Token").size shouldBe 2
        }
    }
}
