package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.lea.cards.Clone
import com.wingedsheep.mtg.sets.definitions.mrd.cards.GildedLotus
import com.wingedsheep.mtg.sets.definitions.nph.cards.PhyrexianMetamorph
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
 * Phyrexian Metamorph (NPH #42): "You may have this creature enter as a copy of any artifact or
 * creature on the battlefield, except it's an artifact in addition to its other types."
 *
 * What the snapshot can't show: the "artifact or creature" menu, that the added artifact type is
 * applied on top of the copied type line (CR 205.1b) and is itself copiable (CR 707.9b — a Clone
 * of the Metamorph is an artifact too), that a noncreature artifact copy stops being a creature,
 * and that declining leaves a 0/0 that dies.
 */
class PhyrexianMetamorphScenarioTest : FunSpec({
    fun driver() = GameTestDriver().also {
        it.registerCards(TestCards.all + listOf(PhyrexianMetamorph, Clone, GildedLotus))
        it.initMirrorMatch(Deck.of("Island" to 40), skipMulligans = true, startingPlayer = 0)
        it.passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun GameTestDriver.castCopier(name: String, choose: EntityId?, check: (SelectCardsDecision) -> Unit = {}): EntityId {
        val id = putCardInHand(player1, name)
        giveMana(player1, Color.BLUE, 4)
        castSpell(player1, id).error shouldBe null
        bothPass()
        val decision = state.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        check(decision)
        submitCardSelection(player1, listOfNotNull(choose)).error shouldBe null
        return id
    }

    test("copying a creature makes it an artifact creature with the copied characteristics") {
        val d = driver()
        val giant = d.putPermanentOnBattlefield(d.player2, "Hill Giant")
        val forest = d.putPermanentOnBattlefield(d.player2, "Forest")
        val meta = d.castCopier("Phyrexian Metamorph", giant) { decision ->
            decision.options shouldContain giant
            withClue("a land is neither an artifact nor a creature") { decision.options shouldNotContain forest }
        }

        val card = d.state.getEntity(meta)!!.get<CardComponent>()!!
        card.name shouldBe "Hill Giant"
        d.state.projectedState.isCreature(meta) shouldBe true
        d.state.projectedState.hasType(meta, "ARTIFACT") shouldBe true
        d.state.projectedState.hasSubtype(meta, "Giant") shouldBe true
        withClue("Phyrexian / Shapeshifter are not copiable through the copy") {
            d.state.projectedState.hasSubtype(meta, "Shapeshifter") shouldBe false
        }
        d.state.projectedState.getPower(meta) shouldBe 3
        d.state.projectedState.getToughness(meta) shouldBe 3
    }

    test("copying a noncreature artifact leaves it a noncreature artifact") {
        val d = driver()
        val lotus = d.putPermanentOnBattlefield(d.player2, "Gilded Lotus")
        val meta = d.castCopier("Phyrexian Metamorph", lotus) { it.options shouldContain lotus }

        (meta in d.state.getBattlefield()) shouldBe true
        d.state.getEntity(meta)!!.get<CardComponent>()!!.name shouldBe "Gilded Lotus"
        d.state.projectedState.hasType(meta, "ARTIFACT") shouldBe true
        d.state.projectedState.isCreature(meta) shouldBe false
    }

    test("the added artifact type is copiable: a Clone of the Metamorph is an artifact too") {
        val d = driver()
        val giant = d.putPermanentOnBattlefield(d.player2, "Hill Giant")
        val meta = d.castCopier("Phyrexian Metamorph", giant)
        val clone = d.castCopier("Clone", meta)

        d.state.getEntity(clone)!!.get<CardComponent>()!!.name shouldBe "Hill Giant"
        d.state.projectedState.hasType(clone, "ARTIFACT") shouldBe true
        d.state.projectedState.isCreature(clone) shouldBe true
    }

    test("declining the copy leaves a 0/0 artifact creature that dies") {
        val d = driver()
        d.putPermanentOnBattlefield(d.player2, "Hill Giant")
        d.castCopier("Phyrexian Metamorph", null)
        d.getGraveyardCardNames(d.player1) shouldContain "Phyrexian Metamorph"
    }
})
