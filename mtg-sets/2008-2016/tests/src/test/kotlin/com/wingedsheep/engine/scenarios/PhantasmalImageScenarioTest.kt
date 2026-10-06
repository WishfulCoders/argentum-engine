package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.lea.cards.Clone
import com.wingedsheep.mtg.sets.definitions.m12.cards.PhantasmalImage
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Phantasmal Image (M12 #72): enters as a copy of any creature, except it's an Illusion in addition
 * to its other types and has "When this creature becomes the target of a spell or ability,
 * sacrifice it."
 *
 * Proves the two copy exceptions land (CR 707.9b), that the added trigger fires on targeting by
 * either player, that both exceptions are copiable values (a Clone of the Image inherits them), and
 * that declining the copy grants nothing.
 */
class PhantasmalImageScenarioTest : FunSpec({
    fun driver() = GameTestDriver().also {
        it.registerCards(TestCards.all + listOf(PhantasmalImage, Clone))
        it.initMirrorMatch(Deck.of("Island" to 40), skipMulligans = true, startingPlayer = 0)
        it.passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun GameTestDriver.castCopier(name: String, choose: EntityId?): EntityId {
        val id = putCardInHand(player1, name)
        giveMana(player1, Color.BLUE, 4)
        castSpell(player1, id).error shouldBe null
        bothPass()
        state.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        submitCardSelection(player1, listOfNotNull(choose)).error shouldBe null
        return id
    }

    test("copies a creature as an Illusion in addition to its other types") {
        val d = driver()
        val giant = d.putPermanentOnBattlefield(d.player2, "Hill Giant")
        val image = d.castCopier("Phantasmal Image", giant)

        d.state.getEntity(image)!!.get<CardComponent>()!!.name shouldBe "Hill Giant"
        d.state.projectedState.hasSubtype(image, "Giant") shouldBe true
        d.state.projectedState.hasSubtype(image, "Illusion") shouldBe true
        d.state.projectedState.getPower(image) shouldBe 3
        d.state.getEntity(image)!!.get<CardComponent>()!!.copyTriggeredAbilities.size shouldBe 1
    }

    test("its controller targeting it triggers the sacrifice, and the pump then fizzles") {
        val d = driver()
        val giant = d.putPermanentOnBattlefield(d.player2, "Hill Giant")
        val image = d.castCopier("Phantasmal Image", giant)

        val growth = d.putCardInHand(d.player1, "Giant Growth")
        d.giveMana(d.player1, Color.GREEN, 1)
        d.castSpellWithTargets(d.player1, growth, listOf(ChosenTarget.Permanent(image))).error shouldBe null
        withClue("the becomes-target trigger goes on the stack above Giant Growth") {
            d.state.stack.size shouldBe 2
        }
        d.bothPass()
        (image in d.state.getBattlefield()) shouldBe false
        d.getGraveyardCardNames(d.player1) shouldContain "Phantasmal Image"
        d.bothPass()
        d.state.stack.size shouldBe 0
    }

    test("an opponent's targeted spell also triggers the sacrifice") {
        val d = driver()
        val giant = d.putPermanentOnBattlefield(d.player2, "Hill Giant")
        val image = d.castCopier("Phantasmal Image", giant)

        val bolt = d.putCardInHand(d.player2, "Lightning Bolt")
        d.giveMana(d.player2, Color.RED, 1)
        // The active player passes with an empty stack, handing the opponent priority.
        d.passPriority(d.player1).error shouldBe null
        d.castSpellWithTargets(d.player2, bolt, listOf(ChosenTarget.Permanent(image))).error shouldBe null
        d.bothPass()
        (image in d.state.getBattlefield()) shouldBe false
    }

    test("a Clone of the Image inherits the Illusion type and the sacrifice trigger") {
        val d = driver()
        val giant = d.putPermanentOnBattlefield(d.player2, "Hill Giant")
        val image = d.castCopier("Phantasmal Image", giant)
        val clone = d.castCopier("Clone", image)

        d.state.getEntity(clone)!!.get<CardComponent>()!!.name shouldBe "Hill Giant"
        d.state.projectedState.hasSubtype(clone, "Illusion") shouldBe true
        d.state.projectedState.hasSubtype(clone, "Shapeshifter") shouldBe false

        val growth = d.putCardInHand(d.player1, "Giant Growth")
        d.giveMana(d.player1, Color.GREEN, 1)
        d.castSpellWithTargets(d.player1, growth, listOf(ChosenTarget.Permanent(clone))).error shouldBe null
        d.bothPass()
        (clone in d.state.getBattlefield()) shouldBe false
        (image in d.state.getBattlefield()) shouldBe true
    }

    test("declining the copy grants no trigger and the 0/0 Illusion dies") {
        val d = driver()
        d.putPermanentOnBattlefield(d.player2, "Hill Giant")
        val image = d.castCopier("Phantasmal Image", null)
        d.getGraveyardCardNames(d.player1) shouldContain "Phantasmal Image"
        d.getGraveyardCardNames(d.player1) shouldNotContain "Hill Giant"
        d.state.getEntity(image)!!.get<CardComponent>()!!.copyTriggeredAbilities shouldBe emptyList()
    }
})
