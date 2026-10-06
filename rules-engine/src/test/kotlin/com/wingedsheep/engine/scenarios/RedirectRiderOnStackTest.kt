package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
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
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.RedirectZoneChangeWith
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * A [RedirectZoneChangeWith] replacement's rider must ride along on **every** route a card takes to
 * a graveyard — including the two a spell takes off the stack without resolving.
 *
 * "If a card would be put into an opponent's graveyard from anywhere, instead exile it with a void
 * counter on it" (Dauthi Voidwalker) replaces the whole event (CR 614.6): the modified event is
 * "exile it with a void counter", not "exile it". A countered spell (CR 701.6a) and a spell whose
 * every target became illegal (CR 608.2b) are both put into their owner's graveyard, so both are
 * caught — and both used to land in exile *without* the counter, because those two stack exits
 * honoured the redirect's destination but dropped its rider. The resolved-spell path already
 * applied it and is the control here.
 */
class RedirectRiderOnStackTest : FunSpec({

    /** The Dauthi Voidwalker replacement on a bare enchantment. */
    val voidWarden = card("Test Void Warden") {
        manaCost = "{2}"
        typeLine = "Enchantment"
        replacementEffect(
            RedirectZoneChangeWith(
                newDestination = Zone.EXILE,
                additionalEffect = Effects.AddCounters(CounterType.VOID, 1, EffectTarget.TriggeringEntity),
                appliesTo = EventPattern.ZoneChangeEvent(
                    filter = GameObjectFilter.Any.nontoken().ownedByOpponent(),
                    to = Zone.GRAVEYARD,
                ),
            )
        )
    }

    val payoff = card("Test Void Payoff") {
        manaCost = "{U}"
        typeLine = "Instant"
        spell { effect = Effects.GainLife(2) }
    }

    val targetedPayoff = card("Test Void Targeted Payoff") {
        manaCost = "{U}"
        typeLine = "Instant"
        spell {
            target(TargetFilter.Creature)
            effect = Effects.DealDamage(1, EffectTarget.ContextTarget(0))
        }
    }

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(voidWarden, payoff, targetedPayoff))
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.settle() {
        var guard = 0
        while (guard++ < 30) {
            when {
                isPaused -> autoResolveDecision()
                state.stack.isNotEmpty() -> bothPass()
                else -> break
            }
        }
    }

    fun GameTestDriver.voidCounters(card: EntityId): Int =
        state.getEntity(card)?.get<CountersComponent>()?.getCount(CounterType.VOID) ?: 0

    /** Hand priority to [opponent] and have them cast [name] at instant speed. */
    fun GameTestDriver.opponentCasts(you: EntityId, opponent: EntityId, name: String, targets: List<ChosenTarget> = emptyList()): EntityId {
        val spell = putCardInHand(opponent, name)
        giveMana(opponent, Color.BLUE, 1)
        passPriority(you)
        submit(CastSpell(opponent, spell, targets = targets, paymentStrategy = PaymentStrategy.FromPool))
            .error shouldBe null
        return spell
    }

    test("control: a resolved opponent's spell is exiled with the rider's counter") {
        val driver = newDriver()
        val you = driver.player1
        val opponent = driver.getOpponent(you)
        driver.putPermanentOnBattlefield(you, "Test Void Warden")

        val spell = driver.opponentCasts(you, opponent, "Test Void Payoff")
        driver.settle()

        driver.getExile(opponent).contains(spell) shouldBe true
        driver.voidCounters(spell) shouldBe 1
    }

    test("a countered opponent's spell is exiled with the rider's counter (CR 701.6a, 614.6)") {
        val driver = newDriver()
        val you = driver.player1
        val opponent = driver.getOpponent(you)
        driver.putPermanentOnBattlefield(you, "Test Void Warden")

        val spell = driver.opponentCasts(you, opponent, "Test Void Payoff")
        val counter = driver.putCardInHand(you, "Counterspell")
        driver.giveMana(you, Color.BLUE, 2)
        driver.passPriority(opponent)
        driver.submit(
            CastSpell(you, counter, targets = listOf(ChosenTarget.Spell(spell)), paymentStrategy = PaymentStrategy.FromPool)
        ).error shouldBe null
        driver.settle()

        withClue("it was countered: no life gained") { driver.getLifeTotal(opponent) shouldBe 20 }
        driver.getGraveyardCardNames(opponent).contains("Test Void Payoff") shouldBe false
        driver.getExile(opponent).contains(spell) shouldBe true
        withClue("the counter rides along with the redirect") { driver.voidCounters(spell) shouldBe 1 }
        withClue("your own card is not an opponent's") {
            driver.getGraveyardCardNames(you).contains("Counterspell") shouldBe true
        }
    }

    test("a fizzled opponent's spell is exiled with the rider's counter (CR 608.2b, 614.6)") {
        val driver = newDriver()
        val you = driver.player1
        val opponent = driver.getOpponent(you)
        driver.putPermanentOnBattlefield(you, "Test Void Warden")
        val bear = driver.putCreatureOnBattlefield(you, "Grizzly Bears")

        val spell = driver.opponentCasts(you, opponent, "Test Void Targeted Payoff", listOf(ChosenTarget.Permanent(bear)))
        driver.moveToGraveyard(bear)
        driver.settle()

        driver.getExile(opponent).contains(spell) shouldBe true
        driver.voidCounters(spell) shouldBe 1
    }
})
