package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SubgameEndedEvent
import com.wingedsheep.engine.core.SubgameStartedEvent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.arn.cards.Shahrazad
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Shahrazad (ARN #10) — {W}{W} Sorcery.
 *
 *   "Players play a Magic subgame, using their libraries as their decks. Each player who doesn't
 *    win the subgame loses half their life, rounded up."
 *
 * The subgame machinery itself is pinned by the engine's `SubgameTest`; this proves the printed
 * card is wired to it: the subgame starts with Shahrazad waiting on the stack, and when it is over
 * the player who lost it — not the winner — loses half their life, rounded up.
 */
class ShahrazadScenarioTest : FunSpec({

    test("the loser of the subgame loses half their life, rounded up, and Shahrazad then resolves") {
        val d = GameTestDriver().apply {
            registerCards(TestCards.all + Shahrazad)
            initMirrorMatch(deck = Deck.of("Plains" to 40), startingLife = 20)
            passPriorityUntil(Step.PRECOMBAT_MAIN)
        }
        val me = d.activePlayer!!
        val opponent = d.getOpponent(me)
        d.setLifeTotal(opponent, 17)

        val shahrazad = d.putCardInHand(me, "Shahrazad")
        d.giveMana(me, Color.WHITE, 2)
        d.castSpell(me, shahrazad).error shouldBe null
        d.bothPass()

        d.events.filterIsInstance<SubgameStartedEvent>().single().sourceName shouldBe "Shahrazad"
        d.state.mainGame shouldNotBe null
        d.state.mainGame!!.stack shouldBe listOf(shahrazad)

        d.concede(opponent)

        d.events.filterIsInstance<SubgameEndedEvent>().single().winnerIds shouldBe listOf(me)
        d.state.mainGame shouldBe null
        d.getLifeTotal(opponent) shouldBe 8
        d.getLifeTotal(me) shouldBe 20
        d.state.stack.shouldBeEmpty()
        d.getGraveyardCardNames(me) shouldBe listOf("Shahrazad")
    }
})
