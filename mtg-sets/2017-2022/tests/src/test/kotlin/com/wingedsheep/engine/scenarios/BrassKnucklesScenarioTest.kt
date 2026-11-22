package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.battlefield.AttachmentsComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.mrd.cards.Bonesplitter
import com.wingedsheep.mtg.sets.definitions.snc.cards.BrassKnuckles
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Brass Knuckles ({4}, Artifact — Equipment):
 *   "When you cast this spell, copy it. (The copy becomes a token.)
 *    Equipped creature has double strike as long as two or more Equipment are attached to it.
 *    Equip {1}"
 *
 * Pins the two things a reader can't confirm from the script alone:
 *  1. The cast trigger copies the permanent spell into a token, exactly once (CR 707.10f).
 *  2. The double-strike condition counts *every* Equipment on the equipped creature — Knuckles
 *     alone is not enough, Knuckles plus any other Equipment is.
 */
class BrassKnucklesScenarioTest : FunSpec({

    val Bear = CardDefinition.creature(
        name = "Knuckle Bear",
        manaCost = ManaCost.parse("{1}{G}"),
        subtypes = setOf(Subtype("Bear")),
        power = 2,
        toughness = 2,
        oracleText = ""
    )

    val stateProjector = StateProjector()

    fun GameTestDriver.attach(equipmentId: EntityId, creatureId: EntityId) {
        var newState = state.updateEntity(equipmentId) { c -> c.with(AttachedToComponent(creatureId)) }
        val existing = newState.getEntity(creatureId)?.get<AttachmentsComponent>()?.attachedIds ?: emptyList()
        newState = newState.updateEntity(creatureId) { c -> c.with(AttachmentsComponent(existing + equipmentId)) }
        replaceState(newState)
    }

    fun setUp(): Pair<GameTestDriver, EntityId> {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(Bear, BrassKnuckles, Bonesplitter))
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver to player
    }

    test("casting it copies the spell: one card and one token copy enter") {
        val (driver, player) = setUp()
        val opponent = driver.getOpponent(player)
        val knuckles = driver.putCardInHand(player, "Brass Knuckles")
        driver.giveColorlessMana(player, 4)
        driver.castSpell(player, knuckles, emptyList())

        // Stack: [cast trigger, Brass Knuckles].
        driver.stackSize shouldBe 2
        var guard = 0
        while (driver.stackSize > 0 && driver.pendingDecision == null && guard++ < 25) {
            driver.passPriority(player)
            driver.passPriority(opponent)
        }

        val onBattlefield = driver.getPermanents(player).filter { driver.getCardName(it) == "Brass Knuckles" }
        onBattlefield.size shouldBe 2
        onBattlefield.count { driver.state.getEntity(it)?.has<TokenComponent>() == true } shouldBe 1
    }

    test("a lone Brass Knuckles does not grant double strike") {
        val (driver, player) = setUp()
        val bear = driver.putCreatureOnBattlefield(player, "Knuckle Bear")
        driver.attach(driver.putPermanentOnBattlefield(player, "Brass Knuckles"), bear)

        stateProjector.project(driver.state).hasKeyword(bear, Keyword.DOUBLE_STRIKE) shouldBe false
    }

    test("two Brass Knuckles on one creature grant double strike") {
        val (driver, player) = setUp()
        val bear = driver.putCreatureOnBattlefield(player, "Knuckle Bear")
        driver.attach(driver.putPermanentOnBattlefield(player, "Brass Knuckles"), bear)
        driver.attach(driver.putPermanentOnBattlefield(player, "Brass Knuckles"), bear)

        stateProjector.project(driver.state).hasKeyword(bear, Keyword.DOUBLE_STRIKE) shouldBe true
    }

    test("any other Equipment counts toward the two") {
        val (driver, player) = setUp()
        val bear = driver.putCreatureOnBattlefield(player, "Knuckle Bear")
        driver.attach(driver.putPermanentOnBattlefield(player, "Brass Knuckles"), bear)
        driver.attach(driver.putPermanentOnBattlefield(player, "Bonesplitter"), bear)

        val projected = stateProjector.project(driver.state)
        projected.hasKeyword(bear, Keyword.DOUBLE_STRIKE) shouldBe true
        projected.getPower(bear) shouldBe 4 // Bonesplitter's own +2/+0 still applies
    }

    test("double strike falls off when the second Equipment leaves") {
        val (driver, player) = setUp()
        val bear = driver.putCreatureOnBattlefield(player, "Knuckle Bear")
        val knuckles = driver.putPermanentOnBattlefield(player, "Brass Knuckles")
        val bonesplitter = driver.putPermanentOnBattlefield(player, "Bonesplitter")
        driver.attach(knuckles, bear)
        driver.attach(bonesplitter, bear)
        stateProjector.project(driver.state).hasKeyword(bear, Keyword.DOUBLE_STRIKE) shouldBe true

        var newState = driver.state.updateEntity(bear) { c -> c.with(AttachmentsComponent(listOf(knuckles))) }
        newState = newState.removeEntity(bonesplitter)
        driver.replaceState(newState)

        stateProjector.project(driver.state).hasKeyword(bear, Keyword.DOUBLE_STRIKE) shouldBe false
    }
})
