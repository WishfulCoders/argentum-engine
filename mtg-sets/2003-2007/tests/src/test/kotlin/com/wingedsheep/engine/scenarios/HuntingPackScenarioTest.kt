package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.scg.cards.HuntingPack
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class HuntingPackScenarioTest : FunSpec({

    test("Hunting Pack with two spells cast before it creates three 4/4 Beasts") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(HuntingPack))
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40))
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val caster = driver.activePlayer!!
        driver.replaceState(driver.state.copy(spellsCastThisTurn = 2))
        repeat(7) { driver.putLandOnBattlefield(caster, "Forest") }
        val pack = driver.putCardInHand(caster, "Hunting Pack")
        driver.castSpell(caster, pack).outcome shouldBe Outcome.Done

        repeat(10) { if (driver.state.stack.isNotEmpty()) driver.bothPass() }
        driver.state.stack.isEmpty() shouldBe true

        val beasts = driver.getCreatures(caster).filter {
            driver.state.getEntity(it)?.get<CardComponent>()?.name == "Beast Token"
        }
        beasts.size shouldBe 3
    }
})
