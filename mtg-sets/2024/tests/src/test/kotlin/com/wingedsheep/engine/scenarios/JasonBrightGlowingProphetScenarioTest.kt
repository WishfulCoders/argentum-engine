package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.isd.cards.WalkingCorpse
import com.wingedsheep.mtg.sets.definitions.lrw.cards.WingsOfVelisVel
import com.wingedsheep.mtg.sets.definitions.pip.cards.JasonBrightGlowingProphet
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Jason Bright, Glowing Prophet — the dies trigger compares the dead Zombie or Mutant's last-known
 * power with its last-known *base* power (CR 608.2h). A +1/+1 counter makes them differ; a "base
 * power and toughness 4/4" effect moves the base with the power, so a 4/4-based Walking Corpse
 * whose power is 4 does not draw even though its printed power is 2.
 */
class JasonBrightGlowingProphetScenarioTest : FunSpec({
    fun setup() = GameTestDriver().apply {
        registerCards(TestCards.all + JasonBrightGlowingProphet + WalkingCorpse + WingsOfVelisVel)
        initMirrorMatch(deck = Deck.of("Island" to 40), skipMulligans = true, startingPlayer = 0)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }
    fun resolve(d: GameTestDriver) {
        var guard = 0
        while (d.pendingDecision == null && d.state.stack.isNotEmpty() && guard++ < 40)
            d.passPriority(d.state.priorityPlayerId!!).error shouldBe null
    }
    /** Pays Come Fly With Me by sacrificing [victim], targeting Jason, and resolves everything. */
    fun sacrifice(d: GameTestDriver, jason: EntityId, victim: EntityId) {
        d.giveColorlessMana(d.player1, 2)
        d.submit(
            ActivateAbility(
                d.player1, jason, JasonBrightGlowingProphet.activatedAbilities.single().id,
                targets = listOf(ChosenTarget.Permanent(jason)),
                costPayment = AdditionalCostPayment(sacrificedPermanents = listOf(victim)),
            )
        ).error shouldBe null
        resolve(d)
    }

    test("a Zombie dying with power above its base power draws a card") {
        val d = setup()
        val jason = d.putCreatureOnBattlefield(d.player1, JasonBrightGlowingProphet.name)
        val corpse = d.putCreatureOnBattlefield(d.player1, WalkingCorpse.name)
        d.addComponent(corpse, CountersComponent().withAdded(CounterType.PLUS_ONE_PLUS_ONE, 1))
        val hand = d.getHandSize(d.player1)
        sacrifice(d, jason, corpse)
        d.getHandSize(d.player1) shouldBe hand + 1
    }

    test("a Zombie dying with power equal to its base power draws nothing") {
        val d = setup()
        val jason = d.putCreatureOnBattlefield(d.player1, JasonBrightGlowingProphet.name)
        val corpse = d.putCreatureOnBattlefield(d.player1, WalkingCorpse.name)
        val hand = d.getHandSize(d.player1)
        sacrifice(d, jason, corpse)
        d.getHandSize(d.player1) shouldBe hand
    }

    test("an effect that sets base power is the base it is compared against, read as it last existed") {
        val d = setup()
        val jason = d.putCreatureOnBattlefield(d.player1, JasonBrightGlowingProphet.name)
        val corpse = d.putCreatureOnBattlefield(d.player1, WalkingCorpse.name)
        val wings = d.putCardInHand(d.player1, WingsOfVelisVel.name)
        d.giveMana(d.player1, Color.BLUE, 2)
        d.castSpell(d.player1, wings, listOf(corpse)).error shouldBe null
        resolve(d)
        d.state.projectedState.getPower(corpse) shouldBe 4
        val hand = d.getHandSize(d.player1)
        // Power 4, base power 4 (printed 2): no draw.
        sacrifice(d, jason, corpse)
        d.getHandSize(d.player1) shouldBe hand
    }

    test("a set base power plus a counter still differs") {
        val d = setup()
        val jason = d.putCreatureOnBattlefield(d.player1, JasonBrightGlowingProphet.name)
        val corpse = d.putCreatureOnBattlefield(d.player1, WalkingCorpse.name)
        val wings = d.putCardInHand(d.player1, WingsOfVelisVel.name)
        d.giveMana(d.player1, Color.BLUE, 2)
        d.castSpell(d.player1, wings, listOf(corpse)).error shouldBe null
        resolve(d)
        d.addComponent(corpse, CountersComponent().withAdded(CounterType.PLUS_ONE_PLUS_ONE, 1))
        val hand = d.getHandSize(d.player1)
        sacrifice(d, jason, corpse)
        d.getHandSize(d.player1) shouldBe hand + 1
    }

    test("a non-Zombie, non-Mutant creature never triggers it") {
        val d = setup()
        val jason = d.putCreatureOnBattlefield(d.player1, JasonBrightGlowingProphet.name)
        val bears = d.putCreatureOnBattlefield(d.player1, "Grizzly Bears")
        d.addComponent(bears, CountersComponent().withAdded(CounterType.PLUS_ONE_PLUS_ONE, 1))
        val hand = d.getHandSize(d.player1)
        sacrifice(d, jason, bears)
        d.getHandSize(d.player1) shouldBe hand
    }

    test("Come Fly With Me puts a +1/+1 counter on the target and gives it flying") {
        val d = setup()
        val jason = d.putCreatureOnBattlefield(d.player1, JasonBrightGlowingProphet.name)
        val bears = d.putCreatureOnBattlefield(d.player1, "Grizzly Bears")
        sacrifice(d, jason, bears)
        d.state.getEntity(jason)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 1
        d.state.projectedState.hasKeyword(jason, Keyword.FLYING) shouldBe true
    }
})
