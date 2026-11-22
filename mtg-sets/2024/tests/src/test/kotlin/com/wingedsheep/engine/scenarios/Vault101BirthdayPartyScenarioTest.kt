package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.lea.cards.HolyStrength
import com.wingedsheep.mtg.sets.definitions.mrd.cards.Bonesplitter
import com.wingedsheep.mtg.sets.definitions.pip.cards.Vault101BirthdayParty
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Vault 101: Birthday Party (PIP #28), chapters II/III: an Aura or Equipment card from hand or
 * graveyard may enter the battlefield with no fixed host. An Aura's controller picks what it
 * enchants as it enters (any legal object, not targeted — an opponent's creature included); an
 * Equipment that entered may then be attached to a creature you control, or left unattached.
 */
class Vault101BirthdayPartyScenarioTest : FunSpec({
    // Run the real chapter II effect as a sorcery so the test doesn't walk turns through draw steps.
    val chapterTwo = card("Vault 101 Chapter II Probe") {
        manaCost = "{W}"
        typeLine = "Sorcery"
        spell { effect = Vault101BirthdayParty.sagaChapters.single { it.chapter == 2 }.effect }
    }

    fun driver(): GameTestDriver = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(Vault101BirthdayParty, HolyStrength, Bonesplitter, chapterTwo))
        initMirrorMatch(Deck.of("Plains" to 40))
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }
    fun GameTestDriver.castChapterTwo() {
        val me = activePlayer!!
        val spell = putCardInHand(me, chapterTwo.name)
        giveMana(me, Color.WHITE, 1)
        castSpell(me, spell).error shouldBe null
        bothPass()
    }
    fun GameTestDriver.hostOf(id: EntityId) = state.getEntity(id)?.get<AttachedToComponent>()?.targetId

    test("offers Auras and Equipment from hand and graveyard; an Equipment may be attached to your creature") {
        val d = driver()
        val me = d.activePlayer!!
        val bears = d.putCreatureOnBattlefield(me, "Grizzly Bears")
        val holy = d.putCardInHand(me, "Holy Strength")
        val bonesplitter = d.putCardInGraveyard(me, "Bonesplitter")

        d.castChapterTwo()
        val select = d.pendingDecision
        select.shouldBeInstanceOf<SelectCardsDecision>()
        select.options shouldContainExactlyInAnyOrder listOf(holy, bonesplitter)
        d.submitCardSelection(me, listOf(bonesplitter)).error shouldBe null

        d.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
        d.submitYesNo(me, true).error shouldBe null
        val host = d.pendingDecision
        host.shouldBeInstanceOf<ChooseTargetsDecision>()
        host.legalTargets[0]!! shouldContainExactlyInAnyOrder listOf(bears)
        d.submitTargetSelection(me, listOf(bears)).error shouldBe null

        d.getPermanents(me) shouldContain bonesplitter
        d.hostOf(bonesplitter) shouldBe bears
        d.state.projectedState.getPower(bears) shouldBe 4
    }

    test("declining the attach leaves the Equipment on the battlefield unattached") {
        val d = driver()
        val me = d.activePlayer!!
        d.putCreatureOnBattlefield(me, "Grizzly Bears")
        val bonesplitter = d.putCardInGraveyard(me, "Bonesplitter")

        d.castChapterTwo()
        d.submitCardSelection(me, listOf(bonesplitter)).error shouldBe null
        d.submitYesNo(me, false).error shouldBe null

        d.pendingDecision shouldBe null
        d.getPermanents(me) shouldContain bonesplitter
        d.hostOf(bonesplitter) shouldBe null
    }

    test("an Aura enters attached to an object its controller chooses, including an opponent's creature") {
        val d = driver()
        val me = d.activePlayer!!
        val opponent = d.getOpponent(me)
        val bears = d.putCreatureOnBattlefield(me, "Grizzly Bears")
        val theirBears = d.putCreatureOnBattlefield(opponent, "Grizzly Bears")
        val holy = d.putCardInHand(me, "Holy Strength")

        d.castChapterTwo()
        d.submitCardSelection(me, listOf(holy)).error shouldBe null
        val host = d.pendingDecision
        host.shouldBeInstanceOf<ChooseTargetsDecision>()
        host.legalTargets[0]!! shouldContainExactlyInAnyOrder listOf(bears, theirBears)
        d.submitTargetSelection(me, listOf(theirBears)).error shouldBe null

        d.pendingDecision shouldBe null
        d.getPermanents(me) shouldContain holy
        d.hostOf(holy) shouldBe theirBears
        d.state.projectedState.getPower(theirBears) shouldBe 3
    }
})
