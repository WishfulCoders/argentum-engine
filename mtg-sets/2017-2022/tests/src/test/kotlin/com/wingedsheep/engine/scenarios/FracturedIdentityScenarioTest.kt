package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.c17.cards.FracturedIdentity
import com.wingedsheep.mtg.sets.definitions.lea.cards.Clone
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Fractured Identity (C17 #37): "Exile target nonland permanent. Each player other than its
 * controller creates a token that's a copy of it."
 *
 * The exile comes first (CR 608.2c), so both halves read last-known information (CR 608.2h): who
 * controlled it, and what it was — a Clone copying Hill Giant makes Hill Giant tokens (ruling).
 */
class FracturedIdentityScenarioTest : FunSpec({
    fun driver(): GameTestDriver = GameTestDriver().also {
        it.registerCards(TestCards.all + listOf(FracturedIdentity, Clone))
        it.initMirrorMatch(Deck.of("Island" to 40), skipMulligans = true, startingPlayer = 0)
        it.passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun GameTestDriver.tokensOf(player: EntityId): List<String?> =
        getPermanents(player).filter { state.getEntity(it)?.has<TokenComponent>() == true }.map { getCardName(it) }

    fun GameTestDriver.castFracture(target: EntityId) {
        val spell = putCardInHand(player1, "Fractured Identity")
        giveMana(player1, Color.WHITE, 1)
        giveMana(player1, Color.BLUE, 4)
        castSpellWithTargets(player1, spell, listOf(ChosenTarget.Permanent(target))).error shouldBe null
        bothPass()
    }

    test("exiling an opponent's creature gives you — and only you — a token copy") {
        val d = driver()
        val giant = d.putCreatureOnBattlefield(d.player2, "Hill Giant")
        d.castFracture(giant)
        (giant in d.state.getZone(ZoneKey(d.player2, Zone.EXILE))) shouldBe true
        d.tokensOf(d.player1) shouldBe listOf("Hill Giant")
        d.tokensOf(d.player2) shouldBe emptyList()
    }

    test("exiling your own permanent gives the copy to your opponent") {
        val d = driver()
        val giant = d.putCreatureOnBattlefield(d.player1, "Hill Giant")
        d.castFracture(giant)
        d.tokensOf(d.player1) shouldBe emptyList()
        d.tokensOf(d.player2) shouldBe listOf("Hill Giant")
    }

    test("your Clone copying Hill Giant is copied as Hill Giant, not as Clone") {
        val d = driver()
        val giant = d.putCreatureOnBattlefield(d.player2, "Hill Giant")
        val clone = d.putCardInHand(d.player1, "Clone")
        d.giveMana(d.player1, Color.BLUE, 4)
        d.castSpell(d.player1, clone).error shouldBe null
        d.bothPass()
        d.state.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        d.submitCardSelection(d.player1, listOf(giant)).error shouldBe null

        d.castFracture(clone)
        withClue("the exiled card is a Clone again, but the opponent's token is what it last was") {
            d.getCardName(clone) shouldBe "Clone"
            d.tokensOf(d.player2) shouldBe listOf("Hill Giant")
            d.tokensOf(d.player1) shouldBe emptyList()
        }
    }
})
