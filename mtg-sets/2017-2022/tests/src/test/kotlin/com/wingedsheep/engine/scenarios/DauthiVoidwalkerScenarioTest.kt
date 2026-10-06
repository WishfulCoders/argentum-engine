package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.mh2.cards.DauthiVoidwalker
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Dauthi Voidwalker (MH2 #81) — the first card on void counters.
 *
 * What is being proved:
 *  - "from anywhere": an opponent's creature that would die, an opponent's spell that resolves,
 *    one that is countered, and a card an opponent discards are all exiled **with a void counter**
 *    — the countered route is the one the stack used to drop the rider on;
 *  - only opponents' cards: your own creature and your own spells still go to your graveyard;
 *  - the sacrifice ability picks among void-countered cards in an opponent's exile and lets you
 *    cast the chosen one this turn with no mana at all, under your control; an exiled card
 *    without a void counter is not on offer.
 */
class DauthiVoidwalkerScenarioTest : FunSpec({

    val discard = card("Test Opponent Discard") {
        manaCost = "{B}"
        typeLine = "Sorcery"
        spell { effect = Effects.EachOpponentDiscards(1) }
    }

    val abilityId = DauthiVoidwalker.activatedAbilities.first().id

    fun newDriver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + listOf(DauthiVoidwalker, discard))
        d.initMirrorMatch(deck = Deck.of("Mountain" to 40), skipMulligans = true, startingPlayer = 0)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return d
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

    fun GameTestDriver.bolt(caster: EntityId, target: ChosenTarget): EntityId {
        giveMana(caster, Color.RED, 1)
        val bolt = putCardInHand(caster, "Lightning Bolt")
        submit(CastSpell(caster, bolt, targets = listOf(target), paymentStrategy = PaymentStrategy.FromPool))
            .error shouldBe null
        return bolt
    }

    test("it has shadow") {
        val d = newDriver()
        val me = d.player1
        val walker = d.putCreatureOnBattlefield(me, "Dauthi Voidwalker")
        d.state.projectedState.hasKeyword(walker, Keyword.SHADOW) shouldBe true
    }

    test("an opponent's creature that would die is exiled with a void counter; yours still dies") {
        val d = newDriver()
        val me = d.player1
        val opponent = d.getOpponent(me)
        d.putCreatureOnBattlefield(me, "Dauthi Voidwalker")
        val theirBear = d.putCreatureOnBattlefield(opponent, "Grizzly Bears")
        val myLions = d.putCreatureOnBattlefield(me, "Savannah Lions")

        d.bolt(me, ChosenTarget.Permanent(theirBear))
        d.settle()
        d.bolt(me, ChosenTarget.Permanent(myLions))
        d.settle()

        withClue("the opponent's creature never reached the graveyard") {
            d.getGraveyardCardNames(opponent).shouldBeEmpty()
            d.getExile(opponent) shouldContain theirBear
            d.voidCounters(theirBear) shouldBe 1
        }
        withClue("your own cards are untouched: the Lions and both Bolts are in your graveyard") {
            d.getGraveyardCardNames(me).count { it == "Savannah Lions" } shouldBe 1
            d.getGraveyardCardNames(me).count { it == "Lightning Bolt" } shouldBe 2
        }
    }

    test("an opponent's spell is exiled with a void counter whether it resolves or is countered") {
        val d = newDriver()
        val me = d.player1
        val opponent = d.getOpponent(me)
        d.putCreatureOnBattlefield(me, "Dauthi Voidwalker")

        d.passPriority(me)
        val resolved = d.bolt(opponent, ChosenTarget.Player(me))
        d.settle()
        d.getLifeTotal(me) shouldBe 17

        d.passPriority(me)
        val countered = d.bolt(opponent, ChosenTarget.Player(me))
        d.passPriority(opponent)
        d.giveMana(me, Color.BLUE, 2)
        val counterspell = d.putCardInHand(me, "Counterspell")
        d.submit(
            CastSpell(me, counterspell, targets = listOf(ChosenTarget.Spell(countered)), paymentStrategy = PaymentStrategy.FromPool)
        ).error shouldBe null
        d.settle()
        d.getLifeTotal(me) shouldBe 17

        d.getGraveyardCardNames(opponent).shouldBeEmpty()
        d.voidCounters(resolved) shouldBe 1
        withClue("countered (CR 701.6a) is still 'put into a graveyard': exiled with the counter") {
            d.getExile(opponent) shouldContain countered
            d.voidCounters(countered) shouldBe 1
        }
        d.getGraveyardCardNames(me) shouldContain "Counterspell"
    }

    test("a card an opponent discards is exiled with a void counter") {
        val d = newDriver()
        val me = d.player1
        val opponent = d.getOpponent(me)
        d.putCreatureOnBattlefield(me, "Dauthi Voidwalker")
        val handBefore = d.getHandSize(opponent)

        d.giveMana(me, Color.BLACK, 1)
        val spell = d.putCardInHand(me, "Test Opponent Discard")
        d.submit(CastSpell(me, spell, paymentStrategy = PaymentStrategy.FromPool)).error shouldBe null
        d.settle()

        d.getHandSize(opponent) shouldBe handBefore - 1
        d.getGraveyardCardNames(opponent).shouldBeEmpty()
        val exiled = d.getExile(opponent)
        exiled.size shouldBe 1
        d.voidCounters(exiled.single()) shouldBe 1
    }

    test("sacrifice it to cast a void-countered card of an opponent's this turn without paying mana") {
        val d = newDriver()
        val me = d.player1
        val opponent = d.getOpponent(me)
        val walker = d.putCreatureOnBattlefield(me, "Dauthi Voidwalker")
        d.removeSummoningSickness(walker)
        val courser = d.putCreatureOnBattlefield(opponent, "Centaur Courser")
        d.bolt(me, ChosenTarget.Permanent(courser))
        d.settle()
        d.voidCounters(courser) shouldBe 1

        // Exiled the ordinary way, with no void counter: never on offer.
        val plain = d.putCardInExile(opponent, "Savannah Lions")

        d.submit(ActivateAbility(me, walker, abilityId)).error shouldBe null
        d.settle()
        withClue("sacrificed as a cost — and it is your card, so it goes to your graveyard") {
            d.getGraveyardCardNames(me) shouldContain "Dauthi Voidwalker"
        }

        val castable = d.legalActions(me).mapNotNull { (it.action as? CastSpell)?.cardId }
        castable shouldContain courser
        withClue("an exiled card without a void counter is not playable") {
            castable.contains(plain) shouldBe false
        }

        // No mana in pool and no lands: the cast only works because the mana cost is waived.
        d.submit(CastSpell(me, courser, emptyList())).error shouldBe null
        d.settle()
        d.findPermanent(me, "Centaur Courser").shouldNotBeNull()
        d.getController(d.findPermanent(me, "Centaur Courser")!!) shouldBe me
    }
})
