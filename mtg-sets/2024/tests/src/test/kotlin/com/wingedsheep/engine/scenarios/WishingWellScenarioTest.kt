package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.TargetsResponse
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.blb.cards.WishingWell
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Wishing Well — {3}{U} Artifact
 * {T}: Put a coin counter on this artifact. When you do, you may cast target instant or sorcery
 * card with mana value equal to the number of coin counters on this artifact from your graveyard
 * without paying its mana cost. If that spell would be put into your graveyard, exile it instead.
 * Activate only as a sorcery.
 *
 * "When you do" is a reflexive triggered ability (CR 603.12) with a real target: chosen as it goes
 * on the stack — after the counter is placed, so the mana value to match is the new count — and
 * re-checked when it resolves (CR 608.2b).
 */
class WishingWellScenarioTest : FunSpec({

    fun setup(): Pair<GameTestDriver, EntityId> {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(WishingWell))
        driver.initMirrorMatch(deck = Deck.of("Island" to 20, "Mountain" to 20))
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver to driver.activePlayer!!
    }

    fun GameTestDriver.activateWell(me: EntityId, well: EntityId) {
        submit(ActivateAbility(me, well, WishingWell.activatedAbilities.first().id)).error shouldBe null
    }

    fun GameTestDriver.coins(well: EntityId): Int =
        state.getEntity(well)?.get<CountersComponent>()?.getCount(CounterType.COIN) ?: 0

    test("the reflexive trigger targets a card whose mana value equals the new coin count, then casts it free") {
        val (driver, me) = setup()
        val opponent = driver.getOpponent(me)
        val well = driver.putPermanentOnBattlefield(me, "Wishing Well")
        val bolt = driver.putCardInGraveyard(me, "Lightning Bolt")    // MV 1
        val growth = driver.putCardInGraveyard(me, "Giant Growth")    // MV 1
        val blade = driver.putCardInGraveyard(me, "Doom Blade")       // MV 2 — doesn't match one coin
        driver.putCardInGraveyard(me, "Grizzly Bears")                 // not an instant or sorcery

        driver.activateWell(me, well)
        driver.bothPass() // the activated ability resolves: one coin counter, then "when you do"

        driver.coins(well) shouldBe 1
        val decision = driver.pendingDecision
        withClue("the reflexive trigger asks for its target: $decision") {
            (decision is ChooseTargetsDecision) shouldBe true
        }
        decision as ChooseTargetsDecision
        withClue("instants and sorceries in your graveyard with mana value 1") {
            decision.legalTargets[0]?.toSet() shouldBe setOf(bolt, growth)
        }
        driver.submitDecision(me, TargetsResponse(decision.id, mapOf(0 to listOf(bolt)))).error shouldBe null
        withClue("the reflexive ability is on the stack with its target") { driver.stackSize shouldBe 1 }

        driver.bothPass()
        driver.submitYesNo(me, true).error shouldBe null // "you may cast"
        val boltTargets = driver.pendingDecision as ChooseTargetsDecision
        driver.submitDecision(me, TargetsResponse(boltTargets.id, mapOf(0 to listOf(opponent)))).error shouldBe null
        driver.bothPass()

        driver.getLifeTotal(opponent) shouldBe 17
        withClue("the free spell is exiled instead of going back to the graveyard") {
            driver.getExile(me).contains(bolt) shouldBe true
        }
        withClue("untargeted cards stay put") {
            driver.getGraveyard(me).containsAll(listOf(growth, blade)) shouldBe true
        }
    }

    test("a target that leaves the graveyard in response means nothing is cast") {
        val (driver, me) = setup()
        val well = driver.putPermanentOnBattlefield(me, "Wishing Well")
        val bolt = driver.putCardInGraveyard(me, "Lightning Bolt")
        val growth = driver.putCardInGraveyard(me, "Giant Growth")

        driver.activateWell(me, well)
        driver.bothPass()
        val decision = driver.pendingDecision as ChooseTargetsDecision
        driver.submitDecision(me, TargetsResponse(decision.id, mapOf(0 to listOf(bolt)))).error shouldBe null

        driver.replaceState(driver.state.moveToZone(bolt, ZoneKey(me, Zone.GRAVEYARD), ZoneKey(me, Zone.EXILE)))
        driver.bothPass()

        withClue("no cast is offered for a target that is gone: ${driver.pendingDecision}") {
            (driver.pendingDecision is YesNoDecision) shouldBe false
        }
        driver.stackSize shouldBe 0
        withClue("the other card is not cast in its place") {
            driver.getGraveyard(me).contains(growth) shouldBe true
        }
    }

    test("with no matching card the counter still lands and nothing is targeted") {
        val (driver, me) = setup()
        val well = driver.putPermanentOnBattlefield(me, "Wishing Well")
        val blade = driver.putCardInGraveyard(me, "Doom Blade") // MV 2, one coin

        driver.activateWell(me, well)
        driver.bothPass()

        driver.coins(well) shouldBe 1
        driver.pendingDecision shouldBe null
        driver.stackSize shouldBe 0
        driver.getGraveyard(me).contains(blade) shouldBe true
    }
})
