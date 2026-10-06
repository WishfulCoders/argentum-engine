package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.HandLookedAtEvent
import com.wingedsheep.engine.state.components.identity.RevealedToComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.engine.view.ClientEventTransformer
import com.wingedsheep.mtg.sets.definitions.ice.cards.UrzasBauble
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Urza's Bauble (ICE #343) — "{T}, Sacrifice this artifact: Look at a card at random in target
 * player's hand. You draw a card at the beginning of the next turn's upkeep."
 */
class UrzasBaubleScenarioTest : FunSpec({

    fun driver(): GameTestDriver = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(UrzasBauble))
        initMirrorMatch(deck = Deck.of("Grizzly Bears" to 40), startingLife = 20)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun GameTestDriver.revealedTo(cardId: EntityId): Set<EntityId> =
        state.getEntity(cardId)?.get<RevealedToComponent>()?.playerIds.orEmpty()

    test("looks at one random card of the target player's hand, privately; draws at the next turn's upkeep") {
        val d = driver()
        val you = d.activePlayer!!
        val opponent = d.getOpponent(you)
        val bauble = d.putPermanentOnBattlefield(you, "Urza's Bauble")
        val theirHand = d.getHand(opponent)

        d.submit(ActivateAbility(
            playerId = you,
            sourceId = bauble,
            abilityId = UrzasBauble.script.activatedAbilities.single().id,
            targets = listOf(ChosenTarget.Player(opponent)),
        )).error shouldBe null
        d.getGraveyard(you) shouldContain bauble
        d.bothPass()

        val known = theirHand.filter { you in d.revealedTo(it) }
        known shouldHaveSize 1
        d.getHand(opponent) shouldContainExactly theirHand
        val look = d.events.filterIsInstance<HandLookedAtEvent>().single()
        look.cardIds shouldContainExactly known
        withClue("only the activator sees the card") {
            ClientEventTransformer.transform(listOf(look), opponent, d.state).shouldBeEmpty()
        }

        val handSize = d.getHandSize(you)
        withClue("no draw this turn") { d.state.stack.size shouldBe 0 }
        d.passPriorityUntil(Step.UPKEEP)
        d.state.activePlayerId shouldNotBe you
        d.state.stack.size shouldBe 1
        d.bothPass()
        withClue("the activator draws during the next turn's upkeep, whoever's turn it is") {
            d.getHandSize(you) shouldBe handSize + 1
        }
    }
})
