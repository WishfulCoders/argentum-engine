package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.nem.cards.ParallaxWave
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Parallax Wave (NEM #17) — the first card on engine-live Fading (CR 702.32).
 *
 * What is being proved:
 *  - it enters with five fade counters, and each activation spends one as its cost;
 *  - the exile and the leaves-the-battlefield return are linked (CR 607.2a): when the Wave leaves,
 *    exactly the cards it exiled come back, each under its owner's control;
 *  - the fading clock is the whole card's endgame: once the counters are spent, the next upkeep
 *    finds none to remove, sacrifices the Wave, and that sacrifice returns everything;
 *  - an activation that resolves after the Wave has left exiles its target for good — the leaves
 *    trigger, put on the stack above it, has already resolved.
 */
class ParallaxWaveScenarioTest : FunSpec({

    val shatter = card("Test Enchantment Shatter") {
        manaCost = "{W}"
        typeLine = "Instant"
        spell {
            val enchantment = target(TargetFilter.Enchantment)
            effect = Effects.Destroy(enchantment)
        }
    }

    val waveAbility = ParallaxWave.activatedAbilities.first().id

    fun newDriver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + listOf(ParallaxWave, shatter))
        d.initMirrorMatch(deck = Deck.of("Plains" to 40), skipMulligans = true, startingPlayer = 0)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return d
    }

    fun GameTestDriver.fade(wave: EntityId): Int =
        state.getEntity(wave)?.get<CountersComponent>()?.getCount(CounterType.FADE) ?: 0

    fun GameTestDriver.castWave(me: EntityId): EntityId {
        giveMana(me, Color.WHITE, 4)
        val waveCard = putCardInHand(me, "Parallax Wave")
        submit(CastSpell(me, waveCard, paymentStrategy = PaymentStrategy.FromPool)).error shouldBe null
        bothPass()
        return findPermanent(me, "Parallax Wave")!!
    }

    fun GameTestDriver.activateWave(me: EntityId, wave: EntityId, target: EntityId) {
        val r = submit(ActivateAbility(me, wave, waveAbility, targets = listOf(ChosenTarget.Permanent(target))))
        withClue("activation: ${r.error}") { r.error shouldBe null }
    }

    fun GameTestDriver.castShatter(caster: EntityId, wave: EntityId) {
        giveMana(caster, Color.WHITE, 1)
        val s = putCardInHand(caster, "Test Enchantment Shatter")
        submit(CastSpell(caster, s, targets = listOf(ChosenTarget.Permanent(wave)), paymentStrategy = PaymentStrategy.FromPool))
            .error shouldBe null
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

    test("enters with five fade counters; each activation spends one and exiles its target") {
        val d = newDriver()
        val me = d.player1
        val opponent = d.getOpponent(me)
        val bear = d.putCreatureOnBattlefield(opponent, "Grizzly Bears")

        val wave = d.castWave(me)
        d.fade(wave) shouldBe 5

        d.activateWave(me, wave, bear)
        withClue("the fade counter is a cost: gone before the ability resolves") { d.fade(wave) shouldBe 4 }
        d.bothPass()

        d.findPermanent(opponent, "Grizzly Bears").shouldBeNull()
        d.getExileCardNames(opponent) shouldBe listOf("Grizzly Bears")
    }

    test("when the Wave leaves, the cards it exiled return under their owners' control") {
        val d = newDriver()
        val me = d.player1
        val opponent = d.getOpponent(me)
        val theirBear = d.putCreatureOnBattlefield(opponent, "Grizzly Bears")
        val myCourser = d.putCreatureOnBattlefield(me, "Centaur Courser")
        val wave = d.castWave(me)

        d.activateWave(me, wave, theirBear)
        d.bothPass()
        d.activateWave(me, wave, myCourser)
        d.bothPass()
        d.getExileCardNames(opponent) shouldBe listOf("Grizzly Bears")
        d.getExileCardNames(me) shouldBe listOf("Centaur Courser")

        // An unrelated card in exile must stay there: the return is linked, not "all exiled cards".
        d.putCardInExile(opponent, "Savannah Lions")

        d.castShatter(me, wave)
        d.settle()

        d.findPermanent(me, "Parallax Wave").shouldBeNull()
        withClue("each card comes back under its owner's control") {
            d.findPermanent(opponent, "Grizzly Bears").shouldNotBeNull()
            d.findPermanent(me, "Centaur Courser").shouldNotBeNull()
        }
        d.getExileCardNames(opponent) shouldBe listOf("Savannah Lions")
    }

    test("fading: once its counters are spent, the next upkeep sacrifices it and returns everything") {
        val d = newDriver()
        val me = d.player1
        val opponent = d.getOpponent(me)
        val bears = (1..5).map { d.putCreatureOnBattlefield(opponent, "Grizzly Bears") }
        val wave = d.castWave(me)

        bears.forEach { bear ->
            d.activateWave(me, wave, bear)
            d.bothPass()
        }
        d.fade(wave) shouldBe 0
        d.findPermanent(me, "Parallax Wave").shouldNotBeNull()
        d.getExileCardNames(opponent).size shouldBe 5

        // Five activations is all it has: a sixth can't pay its cost.
        val sixth = d.putCreatureOnBattlefield(opponent, "Savannah Lions")
        d.submit(ActivateAbility(me, wave, waveAbility, targets = listOf(ChosenTarget.Permanent(sixth))))
            .error.shouldNotBeNull()

        // Through the opponent's turn to my upkeep: the fading trigger finds no counter.
        do {
            d.passPriorityUntil(Step.PRECOMBAT_MAIN)
            d.passPriorityUntil(Step.UPKEEP)
        } while (d.activePlayer != me)
        d.settle()

        d.findPermanent(me, "Parallax Wave").shouldBeNull()
        d.getGraveyardCardNames(me).contains("Parallax Wave") shouldBe true
        d.getCreatures(opponent).count { d.getCardName(it) == "Grizzly Bears" } shouldBe 5
        d.getExileCardNames(opponent).size shouldBe 0
    }

    test("an activation that resolves after the Wave has left exiles its target for good") {
        val d = newDriver()
        val me = d.player1
        val opponent = d.getOpponent(me)
        val bear = d.putCreatureOnBattlefield(opponent, "Grizzly Bears")
        val wave = d.castWave(me)

        d.activateWave(me, wave, bear)
        // In response, the Wave is destroyed. Its leaves trigger goes on the stack above the
        // activation and resolves first — with nothing exiled yet, it returns nothing.
        d.castShatter(me, wave)
        d.settle()

        d.findPermanent(me, "Parallax Wave").shouldBeNull()
        d.findPermanent(opponent, "Grizzly Bears").shouldBeNull()
        d.getExileCardNames(opponent) shouldBe listOf("Grizzly Bears")
    }
})
