package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.SagaComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * CR 714.2b / 714.3a: a token that's a copy of a Saga enters the battlefield **as a Saga** — it
 * gains a [SagaComponent], gets its on-enter lore counter, and its chapter I ability triggers; lore
 * then accrues on the controller's following turns like any other Saga.
 *
 * The ad-hoc token-creation path (`BattlefieldEntry.place`) does not run the enters-with-counters /
 * Saga-entry setup that the standard `moveToZone` pipeline does, so the token-copy executors call the
 * shared [com.wingedsheep.engine.handlers.effects.ZoneMovementUtils.applySagaEntryIfNeeded] hook
 * themselves. This test proves that generally (not through any one card), driven by a plain
 * "create a token that's a copy of target enchantment you control" instant.
 */
class TokenCopyOfSagaEntryScenarioTest : FunSpec({

    val projector = StateProjector()

    // A nonlegendary Saga whose chapters gain life — a visible signal that each chapter triggered.
    val chronicle = card("Test Chronicle") {
        manaCost = "{2}"
        colorIdentity = ""
        typeLine = "Enchantment — Saga"
        oracleText = "I, II, III, IV, V — You gain 1 life."
        for (n in 1..5) sagaChapter(n) { effect = Effects.GainLife(1) }
    }

    // "Create a token that's a copy of target enchantment you control." — a generic copy maker.
    val echo = card("Test Echo") {
        manaCost = "{2}{U}"
        colorIdentity = "U"
        typeLine = "Instant"
        oracleText = "Create a token that's a copy of target enchantment you control."
        spell {
            target = TargetObject(filter = TargetFilter.Enchantment)
            effect = Effects.CreateTokenCopyOfTarget(EffectTarget.ContextTarget(0))
        }
    }

    fun resolveStack(driver: GameTestDriver) {
        var guard = 0
        while (guard++ < 40 && driver.state.stack.isNotEmpty() && !driver.isPaused) driver.bothPass()
    }

    fun clearBenignDecisions(driver: GameTestDriver) {
        var guard = 0
        while (guard++ < 20 && driver.isPaused) driver.autoResolveDecision()
    }

    fun advanceToNextTurnMain(driver: GameTestDriver) {
        driver.passPriorityUntil(Step.END, maxPasses = 300)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN, maxPasses = 300)
        clearBenignDecisions(driver)
        resolveStack(driver)
        clearBenignDecisions(driver)
    }

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(chronicle, echo))
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun tokenCopy(driver: GameTestDriver, player: EntityId, name: String): EntityId? =
        driver.state.getZone(player, Zone.BATTLEFIELD).firstOrNull { id ->
            val c = driver.state.getEntity(id) ?: return@firstOrNull false
            c.get<CardComponent>()?.name == name && c.get<TokenComponent>() != null
        }

    test("a token copy of a Saga enters with a SagaComponent, one lore counter, and chapter I fires") {
        val driver = newDriver()
        val active = driver.activePlayer!!
        val source = driver.putPermanentOnBattlefield(active, "Test Chronicle")
        val lifeBefore = driver.getLifeTotal(active)

        val echoCard = driver.putCardInHand(active, "Test Echo")
        driver.giveMana(active, Color.BLUE, 1)
        driver.giveColorlessMana(active, 2)
        driver.castSpell(active, echoCard, targets = listOf(source))
        driver.bothPass()
        resolveStack(driver)
        clearBenignDecisions(driver) // resolve the copied Saga's chapter I (gain 1 life)
        resolveStack(driver)

        val token = tokenCopy(driver, active, "Test Chronicle")
        token shouldNotBe null
        val container = driver.state.getEntity(token!!)!!
        container.get<SagaComponent>() shouldNotBe null
        container.get<CountersComponent>()!!.getCount(CounterType.LORE) shouldBe 1
        projector.project(driver.state).hasType(token, "Saga") shouldBe true

        val lifeAfter = driver.getLifeTotal(active)
        // Chapter I of the token copy triggered and resolved (gained 1 life). The original on the
        // battlefield was placed directly (no chapter trigger), so the +1 is the token's chapter I.
        lifeAfter shouldBe lifeBefore + 1
    }

    test("a token copy of a Saga accrues lore on the controller's next turn (chapter II triggers)") {
        val driver = newDriver()
        val active = driver.activePlayer!!
        val source = driver.putPermanentOnBattlefield(active, "Test Chronicle")

        val echoCard = driver.putCardInHand(active, "Test Echo")
        driver.giveMana(active, Color.BLUE, 1)
        driver.giveColorlessMana(active, 2)
        driver.castSpell(active, echoCard, targets = listOf(source))
        driver.bothPass()
        resolveStack(driver)
        clearBenignDecisions(driver)
        resolveStack(driver)

        val token = tokenCopy(driver, active, "Test Chronicle")!!
        driver.state.getEntity(token)!!.get<CountersComponent>()!!.getCount(CounterType.LORE) shouldBe 1

        advanceToNextTurnMain(driver) // opponent's turn
        advanceToNextTurnMain(driver) // controller's next turn -> draw step adds a lore counter

        driver.state.getEntity(token)!!.get<CountersComponent>()!!
            .getCount(CounterType.LORE) shouldBe 2
    }
})
