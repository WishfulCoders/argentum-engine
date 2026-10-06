package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.SagaComponent
import com.wingedsheep.engine.state.components.player.LandDropsComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.serialization.json.Json

/**
 * Two pieces of vocabulary Urza's Saga needs, proven without the card:
 *
 * 1. **A Saga that is a land enters as a Saga when it is played.** CR 714.3a gives every Saga the
 *    intrinsic replacement "this Saga enters with a lore counter on it", which applies to *every*
 *    entry — including the land-play special action (CR 305.1), which bypasses both the stack and
 *    the zone-transition pipeline. Chapter I then triggers (CR 714.2b), lore accrues at the
 *    controller's precombat main (CR 714.3c), and the Saga is sacrificed after its final chapter
 *    (CR 714.4).
 * 2. **`CardPredicate.ManaCostIs`** matches the printed mana cost symbol for symbol (CR 202.1), not
 *    the mana value: `{0}` and `{1}` match, `{U}` (mana value 1) and `{X}` (mana value 0) don't, and
 *    a card with no mana cost (CR 202.1b) is not a `{0}` card.
 */
class SagaLandAndManaCostScenarioTest : FunSpec({

    // A Saga land whose chapters each gain life — a visible signal that each chapter triggered.
    val sagaLand = card("Test Saga Land") {
        manaCost = ""
        colorIdentity = ""
        typeLine = "Enchantment Land — Saga"
        oracleText = "I, II, III — You gain 1 life."
        for (n in 1..3) sagaChapter(n) { effect = Effects.GainLife(n) }
    }

    fun artifact(name: String, cost: String) = card(name) {
        manaCost = cost
        colorIdentity = ""
        typeLine = "Artifact"
        oracleText = ""
    }

    val zeroCost = artifact("Test Zero Trinket", "{0}")
    val oneCost = artifact("Test One Trinket", "{1}")
    val blueCost = artifact("Test Blue Trinket", "{U}")
    val xCost = artifact("Test X Trinket", "{X}")
    val noCost = artifact("Test Costless Trinket", "")

    val tutor = card("Test Trinket Tutor") {
        manaCost = "{U}"
        colorIdentity = "U"
        typeLine = "Sorcery"
        oracleText = "Search your library for an artifact card with mana cost {0} or {1}, reveal it, put it into your hand, then shuffle."
        spell {
            effect = Patterns.Library.searchLibrary(
                filter = GameObjectFilter.Artifact.withManaCost("{0}", "{1}"),
                count = 1,
                reveal = true,
            )
        }
    }

    fun resolveStack(driver: GameTestDriver) {
        var guard = 0
        while (guard++ < 40 && driver.state.stack.isNotEmpty() && !driver.isPaused) driver.bothPass()
    }

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(
            TestCards.all + listOf(sagaLand, zeroCost, oneCost, blueCost, xCost, noCost, tutor)
        )
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun lore(driver: GameTestDriver, id: com.wingedsheep.sdk.model.EntityId): Int =
        driver.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.LORE) ?: 0

    fun advanceToOwnNextMain(driver: GameTestDriver) {
        repeat(2) {
            driver.passPriorityUntil(Step.END, maxPasses = 300)
            driver.passPriorityUntil(Step.PRECOMBAT_MAIN, maxPasses = 300)
        }
    }

    test("CR 714.3a: a played Saga land enters with a lore counter and chapter I triggers") {
        val driver = newDriver()
        val active = driver.activePlayer!!
        val land = driver.putCardInHand(active, "Test Saga Land")
        val life = driver.getLifeTotal(active)

        driver.playLand(active, land).error shouldBe null

        driver.state.getZone(active, Zone.BATTLEFIELD).contains(land) shouldBe true
        driver.state.getEntity(land)!!.get<SagaComponent>() shouldNotBe null
        lore(driver, land) shouldBe 1
        driver.state.getEntity(active)!!.get<LandDropsComponent>()!!.remaining shouldBe 0
        driver.state.stack.size shouldBe 1 // chapter I

        resolveStack(driver)
        driver.getLifeTotal(active) shouldBe life + 1
    }

    test("CR 714.3c / 714.4: lore accrues each precombat main and the land is sacrificed after III") {
        val driver = newDriver()
        val active = driver.activePlayer!!
        val land = driver.putCardInHand(active, "Test Saga Land")
        val life = driver.getLifeTotal(active)
        driver.playLand(active, land)
        resolveStack(driver)

        advanceToOwnNextMain(driver)
        lore(driver, land) shouldBe 2
        resolveStack(driver)
        driver.getLifeTotal(active) shouldBe life + 1 + 2
        driver.state.getZone(active, Zone.BATTLEFIELD).contains(land) shouldBe true

        advanceToOwnNextMain(driver)
        resolveStack(driver)
        driver.getLifeTotal(active) shouldBe life + 1 + 2 + 3
        driver.state.getZone(active, Zone.BATTLEFIELD).contains(land) shouldBe false
        driver.getGraveyardCardNames(active).contains("Test Saga Land") shouldBe true
    }

    test("CR 202.1: ManaCostIs reads mana symbols, not mana value, and {0} is not 'no mana cost'") {
        val driver = newDriver()
        val active = driver.activePlayer!!
        val wanted = listOf("Test Zero Trinket", "Test One Trinket")
        val names = wanted + listOf("Test Blue Trinket", "Test X Trinket", "Test Costless Trinket")
        val ids = names.associateWith { driver.putCardOnTopOfLibrary(active, it) }

        val spell = driver.putCardInHand(active, "Test Trinket Tutor")
        driver.giveMana(active, com.wingedsheep.sdk.core.Color.BLUE, 1)
        driver.castSpell(active, spell).error shouldBe null
        driver.bothPass()

        val decision = driver.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        decision.options shouldContainExactlyInAnyOrder wanted.map { ids.getValue(it) }
    }

    test("ManaCostIs compares symbols as a multiset and round-trips through serialization") {
        val predicate = CardPredicate.ManaCostIs(ManaCost.parse("{1}{U}"))
        predicate.matches(ManaCost.parse("{U}{1}")) shouldBe true
        predicate.matches(ManaCost.parse("{2}")) shouldBe false
        CardPredicate.ManaCostIs(ManaCost.parse("{0}")).matches(ManaCost.ZERO) shouldBe false

        val json = Json
        val encoded = json.encodeToString(CardPredicate.serializer(), predicate)
        json.decodeFromString(CardPredicate.serializer(), encoded) shouldBe predicate
    }
})
