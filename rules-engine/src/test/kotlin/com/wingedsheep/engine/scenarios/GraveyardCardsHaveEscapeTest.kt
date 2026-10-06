package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.AlternativeCostType
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.legalactions.LegalAction
import com.wingedsheep.engine.legalactions.LegalActionEnumerator
import com.wingedsheep.engine.state.components.battlefield.CastChoicesComponent
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import com.wingedsheep.sdk.scripting.ChoiceSlot
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GraveyardCardsHaveEscape
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.conditions.Escaped
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * `GraveyardCardsHaveEscape` — the whole-graveyard escape grant behind Underworld Breach ("Each
 * nonland card in your graveyard has escape. The escape cost is equal to the card's mana cost plus
 * exile three other cards from your graveyard."), read through `EscapeCasts.escapeOptions`.
 *
 * Rules pinned here:
 * - CR 702.138a: a granted escape is an escape ability — cast from the graveyard for the escape cost
 *   (here the card's mana cost plus exiling three *other* cards), normal timing, not exiled on
 *   resolution.
 * - CR 702.138b: a spell cast with a granted escape "escaped" (instant reads it while resolving; a
 *   permanent is stamped).
 * - CR 109.5: "your graveyard" — only the granter's controller's graveyard gets escape.
 * - CR 118.6: a card with no mana cost has an unpayable escape cost from this grant.
 * - CR 107.3a: an {X} in the mana cost is announced as usual.
 * - CR 601.2b + Underworld Breach ruling: with a printed and a granted escape, the caster chooses
 *   which to apply; each is charged as its own cost.
 */
class GraveyardCardsHaveEscapeTest : FunSpec({

    val breach = card("Breach Test") {
        manaCost = "{1}{R}"
        typeLine = "Enchantment"
        staticAbility {
            ability = GraveyardCardsHaveEscape(
                filter = GameObjectFilter.Nonland,
                additionalCost = Costs.additional.ExileOtherCards(3)
            )
        }
    }

    // {1}{R} instant: gain 5 life if it escaped, else 2.
    val escapeShock = card("Breach Shock") {
        manaCost = "{1}{R}"
        typeLine = "Instant"
        spell {
            effect = Effects.If(
                condition = Conditions.Escaped,
                then = Effects.GainLife(5),
                otherwise = Effects.GainLife(2)
            )
        }
    }

    val bear = card("Breach Bear") {
        manaCost = "{1}{G}"
        typeLine = "Creature — Bear"
        power = 2
        toughness = 2
    }

    // Printed escape {B} + exile two; mana cost {3}{B} (so Breach's escape is {3}{B} + exile three).
    val printedEscaper = card("Printed Escaper") {
        manaCost = "{3}{B}"
        typeLine = "Creature — Zombie"
        power = 3
        toughness = 3
        keywordAbility(KeywordAbility.escape("{B}", Costs.additional.ExileOtherCards(2)))
    }

    // No mana cost (suspend-style): unpayable escape cost from the grant.
    val noCost = card("Costless Wonder") {
        typeLine = "Sorcery"
        spell { effect = Effects.GainLife(1) }
    }

    // {X}{R} sorcery: gain X life.
    val xGain = card("Breach X Gain") {
        manaCost = "{X}{R}"
        typeLine = "Sorcery"
        spell { effect = Effects.GainLife(DynamicAmounts.xValue()) }
    }

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(breach, escapeShock, bear, printedEscaper, noCost, xGain))
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), startingLife = 20)
        return driver
    }

    fun escapeOffers(driver: GameTestDriver, playerId: EntityId, cardId: EntityId): List<LegalAction> =
        LegalActionEnumerator.create(driver.cardRegistry).enumerate(driver.state, playerId)
            .filter { (it.action as? CastSpell)?.let { a -> a.cardId == cardId && a.alternativeCostType == AlternativeCostType.ESCAPE } == true }

    fun escape(playerId: EntityId, cardId: EntityId, exiled: List<EntityId>, choice: Int? = null, x: Int? = null) = CastSpell(
        playerId = playerId,
        cardId = cardId,
        xValue = x,
        useAlternativeCost = true,
        alternativeCostType = AlternativeCostType.ESCAPE,
        escapeChoice = choice,
        additionalCostPayment = AdditionalCostPayment(exiledCards = exiled),
        paymentStrategy = PaymentStrategy.FromPool
    )

    fun fillers(driver: GameTestDriver, playerId: EntityId, n: Int) = List(n) { driver.putCardInGraveyard(playerId, "Swamp") }

    fun resolveStack(driver: GameTestDriver) {
        while (driver.state.stack.isNotEmpty()) driver.bothPass()
    }

    fun manaLeft(driver: GameTestDriver, playerId: EntityId): Int =
        driver.state.getEntity(playerId)?.get<ManaPoolComponent>()
            ?.let { it.white + it.blue + it.black + it.red + it.green + it.colorless } ?: 0

    test("the grant gives a nonland graveyard card escape at its mana cost, and never a land") {
        val driver = createDriver()
        val you = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.putPermanentOnBattlefield(you, "Breach Test")
        val shock = driver.putCardInGraveyard(you, "Breach Shock")
        val swamps = fillers(driver, you, 3)
        driver.giveMana(you, Color.RED, 2)

        val offer = escapeOffers(driver, you, shock).single()
        offer.affordable.shouldBeTrue()
        offer.manaCostString shouldBe "{1}{R}"
        offer.description shouldBe "Cast Breach Shock (Escape)"
        offer.additionalCostInfo.shouldNotBeNull().validExileTargets shouldContainAll swamps
        offer.additionalCostInfo!!.validExileTargets shouldNotContain shock
        swamps.forEach { escapeOffers(driver, you, it).shouldBeEmpty() }
    }

    test("escaping with the grant exiles three others; the instant escaped and returns to the graveyard") {
        val driver = createDriver()
        val you = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.putPermanentOnBattlefield(you, "Breach Test")
        val shock = driver.putCardInGraveyard(you, "Breach Shock")
        val swamps = fillers(driver, you, 3)
        driver.giveMana(you, Color.RED, 2)
        val lifeBefore = driver.getLifeTotal(you)

        val result = driver.submit(escape(you, shock, swamps))
        withClue("error=${result.error}") { result.outcome shouldBe Outcome.Done }
        driver.getExile(you) shouldContainAll swamps
        resolveStack(driver)

        driver.getLifeTotal(you) shouldBe lifeBefore + 5
        driver.getGraveyard(you) shouldContain shock
        driver.getExile(you) shouldNotContain shock
    }

    test("a permanent cast with the granted escape escaped (CR 702.138b)") {
        val driver = createDriver()
        val you = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.putPermanentOnBattlefield(you, "Breach Test")
        val bearCard = driver.putCardInGraveyard(you, "Breach Bear")
        val swamps = fillers(driver, you, 3)
        driver.giveMana(you, Color.GREEN, 2)

        driver.submit(escape(you, bearCard, swamps)).outcome shouldBe Outcome.Done
        resolveStack(driver)

        val perm = driver.findPermanent(you, "Breach Bear").shouldNotBeNull()
        driver.state.getEntity(perm)?.get<CastChoicesComponent>()?.chosen?.containsKey(ChoiceSlot.ESCAPED) shouldBe true
        PredicateEvaluator(cardRegistry = null).conditions.evaluate(
            driver.state, Escaped, EffectContext(sourceId = perm, controllerId = you)
        ).shouldBeTrue()
    }

    test("the exile half needs three OTHER cards: the card plus two others can't pay it") {
        val driver = createDriver()
        val you = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.putPermanentOnBattlefield(you, "Breach Test")
        val shock = driver.putCardInGraveyard(you, "Breach Shock")
        val swamps = fillers(driver, you, 2)
        driver.giveMana(you, Color.RED, 2)

        escapeOffers(driver, you, shock).single().affordable.shouldBeFalse()
        driver.submitExpectFailure(escape(you, shock, swamps))
        driver.submitExpectFailure(escape(you, shock, swamps + shock))
    }

    test("no granter, no escape; an opponent's granter doesn't grant escape to your graveyard (CR 109.5)") {
        val driver = createDriver()
        val you = driver.activePlayer!!
        val opponent = driver.getOpponent(you)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val shock = driver.putCardInGraveyard(you, "Breach Shock")
        val swamps = fillers(driver, you, 3)
        driver.giveMana(you, Color.RED, 2)

        escapeOffers(driver, you, shock).shouldBeEmpty()
        driver.putPermanentOnBattlefield(opponent, "Breach Test")
        escapeOffers(driver, you, shock).shouldBeEmpty()
        driver.submitExpectFailure(escape(you, shock, swamps))
    }

    test("a card with no mana cost has an unpayable escape cost, so it can't escape (CR 118.6)") {
        val driver = createDriver()
        val you = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.putPermanentOnBattlefield(you, "Breach Test")
        val wonder = driver.putCardInGraveyard(you, "Costless Wonder")
        val swamps = fillers(driver, you, 3)

        escapeOffers(driver, you, wonder).shouldBeEmpty()
        driver.submitExpectFailure(escape(you, wonder, swamps))
        driver.getGraveyard(you) shouldContain wonder
    }

    test("an {X} in the mana cost is announced and paid as usual (CR 107.3a)") {
        val driver = createDriver()
        val you = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.putPermanentOnBattlefield(you, "Breach Test")
        val spell = driver.putCardInGraveyard(you, "Breach X Gain")
        val swamps = fillers(driver, you, 3)
        driver.giveMana(you, Color.RED, 4)
        val lifeBefore = driver.getLifeTotal(you)

        val offer = escapeOffers(driver, you, spell).single()
        offer.hasXCost.shouldBeTrue()
        offer.maxAffordableX shouldBe 3

        val result = driver.submit(escape(you, spell, swamps, x = 3))
        withClue("error=${result.error}") { result.outcome shouldBe Outcome.Done }
        manaLeft(driver, you) shouldBe 0
        resolveStack(driver)
        driver.getLifeTotal(you) shouldBe lifeBefore + 3
    }

    test("printed and granted escape: one offer each, the caster chooses, and each charges its own cost") {
        val driver = createDriver()
        val you = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.putPermanentOnBattlefield(you, "Breach Test")
        val escaper = driver.putCardInGraveyard(you, "Printed Escaper")
        val swamps = fillers(driver, you, 3)
        driver.giveMana(you, Color.BLACK, 1)

        val offers = escapeOffers(driver, you, escaper)
        offers shouldHaveSize 2
        offers.map { (it.action as CastSpell).escapeChoice } shouldContainExactlyInAnyOrder listOf(0, 1)
        offers.map { it.manaCostString } shouldContainExactlyInAnyOrder listOf("{B}", "{3}{B}")
        offers.first { (it.action as CastSpell).escapeChoice == 0 }.affordable.shouldBeTrue()
        offers.first { (it.action as CastSpell).escapeChoice == 1 }.affordable.shouldBeFalse()

        // Breach's escape costs {3}{B}: one black mana can't pay it, and a choice past the
        // options is rejected rather than falling back to another escape.
        driver.submitExpectFailure(escape(you, escaper, swamps, choice = 1))
        driver.submitExpectFailure(escape(you, escaper, swamps.take(2), choice = 2))

        // The printed escape: {B} + exactly two others.
        val result = driver.submit(escape(you, escaper, swamps.take(2), choice = 0))
        withClue("error=${result.error}") { result.outcome shouldBe Outcome.Done }
        driver.getExile(you) shouldContainAll swamps.take(2)
        driver.getGraveyard(you) shouldContain swamps[2]
    }

    test("choosing the granted escape over the printed one pays the mana cost and exiles three") {
        val driver = createDriver()
        val you = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.putPermanentOnBattlefield(you, "Breach Test")
        val escaper = driver.putCardInGraveyard(you, "Printed Escaper")
        val swamps = fillers(driver, you, 3)
        driver.giveMana(you, Color.BLACK, 4)

        val result = driver.submit(escape(you, escaper, swamps, choice = 1))
        withClue("error=${result.error}") { result.outcome shouldBe Outcome.Done }
        manaLeft(driver, you) shouldBe 0
        driver.getExile(you) shouldContainAll swamps
        resolveStack(driver)
        val perm = driver.findPermanent(you, "Printed Escaper").shouldNotBeNull()
        driver.state.getEntity(perm)?.get<CastChoicesComponent>()?.chosen?.containsKey(ChoiceSlot.ESCAPED) shouldBe true
    }

    test("two granters grant the same escape once") {
        val driver = createDriver()
        val you = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.putPermanentOnBattlefield(you, "Breach Test")
        driver.putPermanentOnBattlefield(you, "Breach Test")
        val shock = driver.putCardInGraveyard(you, "Breach Shock")
        fillers(driver, you, 3)
        driver.giveMana(you, Color.RED, 2)

        val offer = escapeOffers(driver, you, shock).single()
        (offer.action as CastSpell).escapeChoice shouldBe null
    }

    test("escape grants no timing permission: with a spell on the stack an instant escapes, a creature doesn't") {
        val driver = createDriver()
        val you = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.putPermanentOnBattlefield(you, "Breach Test")
        val shockInHand = driver.putCardInHand(you, "Breach Shock")
        val shock = driver.putCardInGraveyard(you, "Breach Shock")
        val bearCard = driver.putCardInGraveyard(you, "Breach Bear")
        fillers(driver, you, 3)
        driver.giveMana(you, Color.RED, 4)
        driver.giveMana(you, Color.GREEN, 2)

        escapeOffers(driver, you, bearCard) shouldHaveSize 1
        driver.submit(CastSpell(you, shockInHand, paymentStrategy = PaymentStrategy.FromPool)).outcome shouldBe Outcome.Done
        driver.state.stack.isNotEmpty().shouldBeTrue()

        escapeOffers(driver, you, shock) shouldHaveSize 1
        escapeOffers(driver, you, bearCard).shouldBeEmpty()
    }
})
