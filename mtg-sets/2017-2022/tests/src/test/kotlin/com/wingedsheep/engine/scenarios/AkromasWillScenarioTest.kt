package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.cmr.cards.AkromasWill
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Akroma's Will (CMR #3), cast without its commander clause: each mode grants its keywords to
 * every creature you control, and none to the opponent's. Guards the per-creature grant inside
 * `ForEachInGroup`, which must name the visited creature (`EffectTarget.IterationEntity`) — after
 * the September 2026 upstream SDK rework `EffectTarget.Self` there is the spell itself.
 */
class AkromasWillScenarioTest : FunSpec({

    fun cast(mode: Int): Triple<GameTestDriver, List<com.wingedsheep.sdk.model.EntityId>, com.wingedsheep.sdk.model.EntityId> {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + AkromasWill)
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val you = driver.player1
        val opponent = driver.getOpponent(you)
        val mine = listOf(
            driver.putCreatureOnBattlefield(you, "Savannah Lions"),
            driver.putCreatureOnBattlefield(you, "Centaur Courser"),
        )
        val theirs = driver.putCreatureOnBattlefield(opponent, "Savannah Lions")
        val spell = driver.putCardInHand(you, "Akroma's Will")
        driver.giveMana(you, Color.WHITE, 4)
        val result = driver.submit(CastSpell(playerId = you, cardId = spell, chosenModes = listOf(mode)))
        if (result.outcome !is Outcome.Done) throw AssertionError("cast failed: ${result.error}")
        var guard = 0
        while (driver.state.stack.isNotEmpty() && guard++ < 10) driver.bothPass()
        return Triple(driver, mine, theirs)
    }

    test("mode one: your creatures gain flying, vigilance and double strike; theirs do not") {
        val (driver, mine, theirs) = cast(0)
        val p = driver.state.projectedState
        for (c in mine) for (k in listOf(Keyword.FLYING, Keyword.VIGILANCE, Keyword.DOUBLE_STRIKE)) {
            p.hasKeyword(c, k) shouldBe true
        }
        p.hasKeyword(theirs, Keyword.FLYING) shouldBe false
    }

    test("mode two: your creatures gain lifelink and indestructible; theirs do not") {
        val (driver, mine, theirs) = cast(1)
        val p = driver.state.projectedState
        for (c in mine) for (k in listOf(Keyword.LIFELINK, Keyword.INDESTRUCTIBLE)) {
            p.hasKeyword(c, k) shouldBe true
        }
        p.hasKeyword(theirs, Keyword.INDESTRUCTIBLE) shouldBe false
    }
})
