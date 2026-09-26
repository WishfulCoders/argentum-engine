package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.core.TargetsResponse
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.blb.cards.ZoralineCosmosCaller
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Zoraline, Cosmos Caller — {1}{W}{B} Legendary Creature — Bat Cleric 3/3
 * Flying, vigilance
 * Whenever a Bat you control attacks, you gain 1 life.
 * Whenever Zoraline enters or attacks, you may pay {W}{B} and 2 life. When you do, return target
 * nonland permanent card with mana value 3 or less from your graveyard to the battlefield with a
 * finality counter on it.
 *
 * Per the 2024-07-26 ruling the target is chosen by a second, reflexive ability (CR 603.12) as it
 * goes on the stack after the payment — not while the first trigger resolves — and it is re-checked
 * on resolution (CR 608.2b).
 */
class ZoralineCosmosCallerScenarioTest : FunSpec({

    fun setup(): Pair<GameTestDriver, EntityId> {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(ZoralineCosmosCaller))
        driver.initMirrorMatch(deck = Deck.of("Plains" to 20, "Swamp" to 20))
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver to driver.activePlayer!!
    }

    /** Cast Zoraline with enough mana left over for the {W}{B}, and let it resolve. */
    fun GameTestDriver.castZoraline(me: EntityId) {
        val zoraline = putCardInHand(me, "Zoraline, Cosmos Caller")
        giveColorlessMana(me, 1)
        giveMana(me, Color.WHITE, 2)
        giveMana(me, Color.BLACK, 2)
        castSpell(me, zoraline).error shouldBe null
        bothPass()
    }

    /** Answer the may-pay question and any mana-source prompt until the reflexive target decision. */
    fun GameTestDriver.payUntilTargeting(me: EntityId, pay: Boolean) {
        var guard = 0
        while (guard++ < 10) {
            when (val d = pendingDecision) {
                is YesNoDecision -> submitYesNo(me, pay).error shouldBe null
                is SelectManaSourcesDecision -> submitManaAutoPayOrDecline(me, autoPay = true).error shouldBe null
                is ChooseTargetsDecision -> return
                null -> if (stackSize > 0) bothPass() else return
                else -> error("unexpected decision $d")
            }
        }
    }

    test("paying puts a reflexive trigger on the stack that targets a card of mana value 3 or less") {
        val (driver, me) = setup()
        val bears = driver.putCardInGraveyard(me, "Grizzly Bears")      // MV 2
        val lions = driver.putCardInGraveyard(me, "Savannah Lions")     // MV 1
        val giant = driver.putCardInGraveyard(me, "Hill Giant")         // MV 4
        val bolt = driver.putCardInGraveyard(me, "Lightning Bolt")      // not a permanent card
        val plains = driver.putCardInGraveyard(me, "Plains")            // a land

        driver.castZoraline(me)
        driver.payUntilTargeting(me, pay = true)

        withClue("two life were paid before any target was chosen") { driver.getLifeTotal(me) shouldBe 18 }
        val decision = driver.pendingDecision as ChooseTargetsDecision
        withClue("nonland permanent cards with mana value 3 or less") {
            decision.legalTargets[0]?.toSet() shouldBe setOf(bears, lions)
        }
        driver.submitDecision(me, TargetsResponse(decision.id, mapOf(0 to listOf(bears)))).error shouldBe null
        withClue("the reflexive ability waits on the stack with its target") { driver.stackSize shouldBe 1 }
        driver.bothPass()

        driver.findPermanent(me, "Grizzly Bears") shouldBe bears
        withClue("it returns with a finality counter") {
            driver.state.getEntity(bears)?.get<CountersComponent>()?.getCount(CounterType.FINALITY) shouldBe 1
        }
        driver.getGraveyard(me).containsAll(listOf(lions, giant, bolt, plains)) shouldBe true
    }

    test("declining to pay asks for no target and returns nothing") {
        val (driver, me) = setup()
        val bears = driver.putCardInGraveyard(me, "Grizzly Bears")
        driver.putCardInGraveyard(me, "Savannah Lions")

        driver.castZoraline(me)
        driver.payUntilTargeting(me, pay = false)

        driver.pendingDecision shouldBe null
        driver.getLifeTotal(me) shouldBe 20
        driver.getGraveyard(me).contains(bears) shouldBe true
    }

    test("a target exiled in response leaves the reflexive trigger with nothing to return") {
        val (driver, me) = setup()
        val bears = driver.putCardInGraveyard(me, "Grizzly Bears")
        val lions = driver.putCardInGraveyard(me, "Savannah Lions")

        driver.castZoraline(me)
        driver.payUntilTargeting(me, pay = true)
        val decision = driver.pendingDecision as ChooseTargetsDecision
        driver.submitDecision(me, TargetsResponse(decision.id, mapOf(0 to listOf(bears)))).error shouldBe null

        driver.replaceState(driver.state.moveToZone(bears, ZoneKey(me, Zone.GRAVEYARD), ZoneKey(me, Zone.EXILE)))
        driver.bothPass()

        driver.getExile(me).contains(bears) shouldBe true
        withClue("no other card is returned in its place") {
            driver.findPermanent(me, "Savannah Lions") shouldBe null
            driver.getGraveyard(me).contains(lions) shouldBe true
        }
    }
})
