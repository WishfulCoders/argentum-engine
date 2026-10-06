package com.wingedsheep.engine.handlers.costs

import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * `PayCost.ManaCostOf(entity, genericReduction)` — "sacrifice it unless you pay its mana cost
 * reduced by {2}" (Flash), lowered by `PayOrSufferExecutor` at resolution.
 *
 * Pinned against the rules it cites and Flash's 2018-03-16 rulings:
 * - only the generic component is reduced (CR 118.7a): {1}{R} costs {R}, {3}{U}{U} costs {1}{U}{U};
 * - {X} in the named object's cost is 0 (CR 107.3h);
 * - declining, or being unable to pay, sacrifices the creature — and it really entered, so its
 *   enters trigger still fires;
 * - the cost names the pipeline's object, not the resolving spell (which would read {1}{U}).
 */
class ManaCostOfPayCostTest : FunSpec({

    val probe = card("Test Flash") {
        manaCost = "{1}{U}"
        typeLine = "Instant"
        spell {
            effect = Effects.Pipeline {
                val creatures = gather(CardSource.FromZone(Zone.HAND, Player.You, GameObjectFilter.Creature))
                val chosen = chooseUpTo(1, from = creatures)
                val entered = moveTracked(chosen, CardDestination.ToZone(Zone.BATTLEFIELD))
                ifNotEmpty(entered) {
                    run(
                        Effects.PayOrSuffer(
                            cost = Costs.pay.ManaCostOf(entered.asTarget, genericReduction = 2),
                            suffer = Effects.SacrificeTarget(entered.asTarget)
                        )
                    )
                }
            }
        }
    }
    val redBear = card("Test Red Bear") {
        manaCost = "{1}{R}"
        typeLine = "Creature — Bear"
        power = 2
        toughness = 2
        triggeredAbility {
            trigger = Triggers.self.enters()
            effect = Effects.GainLife(3)
        }
    }
    val bigBlue = card("Test Big Blue") {
        manaCost = "{3}{U}{U}"
        typeLine = "Creature — Serpent"
        power = 5
        toughness = 5
    }
    val xBeast = card("Test X Beast") {
        manaCost = "{X}{G}"
        typeLine = "Creature — Beast"
        power = 1
        toughness = 1
    }

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(probe, redBear, bigBlue, xBeast))
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingPlayer = 0, skipMulligans = true)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    /**
     * Cast the probe from a {1}{U} pool, float [extraMana] for the payment while it is on the
     * stack, put [creature] onto the battlefield, and return the payment prompt (if any).
     */
    fun GameTestDriver.flashIn(caster: EntityId, creature: EntityId, extraMana: GameTestDriver.() -> Unit = {}): YesNoDecision? {
        val spell = putCardInHand(caster, "Test Flash")
        giveMana(caster, Color.BLUE, 1)
        giveColorlessMana(caster, 1)
        castSpell(caster, spell).outcome shouldBe Outcome.Done
        extraMana()
        bothPass()
        pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        submitCardSelection(caster, listOf(creature))
        return pendingDecision as? YesNoDecision
    }

    test("only the generic part is reduced: {1}{R} costs {R}, and paying keeps the creature") {
        val driver = newDriver()
        val me = driver.activePlayer!!
        val bear = driver.putCardInHand(me, "Test Red Bear")

        val prompt = driver.flashIn(me, bear) { giveMana(me, Color.RED, 1) }!!
        prompt.prompt shouldContain "Pay {R} or"
        driver.submitYesNo(me, true)

        withClue("the creature stays") { driver.state.getBattlefield().contains(bear) shouldBe true }
        driver.state.getZone(me, Zone.GRAVEYARD).contains(bear) shouldBe false
    }

    test("{3}{U}{U} costs {1}{U}{U}") {
        val driver = newDriver()
        val me = driver.activePlayer!!
        val serpent = driver.putCardInHand(me, "Test Big Blue")

        val prompt = driver.flashIn(me, serpent) {
            giveMana(me, Color.BLUE, 2)
            giveColorlessMana(me, 1)
        }!!
        prompt.prompt shouldContain "Pay {1}{U}{U} or"
        driver.submitYesNo(me, true)
        driver.state.getBattlefield().contains(serpent) shouldBe true
    }

    test("X in the creature's cost is 0: {X}{G} costs {G}") {
        val driver = newDriver()
        val me = driver.activePlayer!!
        val beast = driver.putCardInHand(me, "Test X Beast")

        val prompt = driver.flashIn(me, beast) { giveMana(me, Color.GREEN, 1) }!!
        prompt.prompt shouldContain "Pay {G} or"
    }

    test("declining sacrifices it, and its enters trigger still fires") {
        val driver = newDriver()
        val me = driver.activePlayer!!
        val bear = driver.putCardInHand(me, "Test Red Bear")
        val life = driver.getLifeTotal(me)

        driver.flashIn(me, bear) { giveMana(me, Color.RED, 1) }!!
        driver.submitYesNo(me, false)

        driver.state.getZone(me, Zone.GRAVEYARD).contains(bear) shouldBe true
        var guard = 0
        while (driver.stackSize > 0 && guard++ < 10) driver.bothPass()
        withClue("the enters trigger resolves though the creature is gone") {
            driver.getLifeTotal(me) shouldBe life + 3
        }
    }

    test("a player who can't pay has it sacrificed without being asked") {
        val driver = newDriver()
        val me = driver.activePlayer!!
        val bear = driver.putCardInHand(me, "Test Red Bear")

        val prompt = driver.flashIn(me, bear)
        prompt shouldBe null
        driver.state.getZone(me, Zone.GRAVEYARD).contains(bear) shouldBe true
    }
})
