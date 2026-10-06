package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GameObjectFilter
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * A sacrifice filter is read in the ability's own context, so a source-relative predicate works
 * in it. "You may sacrifice another creature or an artifact" (Gut, True Soul Zealot) binds
 * "another" to the creature branch only: `Creature.notSourceItself() or Artifact`. The source is
 * not offered as a creature, but is offered if it is an artifact (Gut's ruling: "If Gut somehow
 * becomes an artifact, you may sacrifice it to its own ability"). Both the may-pay affordability
 * check and the sacrifice itself must agree.
 */
class SacrificeSourceRelativeFilterTest : FunSpec({

    fun zealot(name: String, typeLine: String) = card(name) {
        manaCost = "{1}"
        this.typeLine = typeLine
        power = 2
        toughness = 2
        activatedAbility {
            cost = Costs.Mana("{1}")
            effect = Effects.MayPay(
                cost = Effects.SacrificeOwn(GameObjectFilter.Creature.notSourceItself() or GameObjectFilter.Artifact),
                then = Effects.GainLife(3)
            )
        }
    }
    val creatureZealot = zealot("Test Creature Zealot", "Creature — Goblin")
    val artifactZealot = zealot("Test Artifact Zealot", "Artifact Creature — Golem")

    fun driver() = GameTestDriver().also {
        it.registerCards(TestCards.all + listOf(creatureZealot, artifactZealot))
        it.initMirrorMatch(Deck.of("Island" to 40), skipMulligans = true, startingPlayer = 0)
        it.passPriorityUntil(Step.PRECOMBAT_MAIN)
    }
    fun GameTestDriver.activate(source: EntityId, def: com.wingedsheep.sdk.model.CardDefinition) {
        giveColorlessMana(player1, 1)
        submit(ActivateAbility(player1, source, def.script.activatedAbilities.single().id)).error shouldBe null
        bothPass()
    }

    test("alone, a non-artifact source isn't fodder for itself: no prompt, no payoff") {
        val d = driver()
        val source = d.putCreatureOnBattlefield(d.player1, creatureZealot.name)
        val life = d.getLifeTotal(d.player1)
        d.activate(source, creatureZealot)
        d.state.pendingDecision shouldBe null
        d.findPermanent(d.player1, creatureZealot.name) shouldBe source
        d.getLifeTotal(d.player1) shouldBe life
    }

    test("another creature is offered and the source is not") {
        val d = driver()
        val source = d.putCreatureOnBattlefield(d.player1, creatureZealot.name)
        val bears = d.putCreatureOnBattlefield(d.player1, "Grizzly Bears")
        val life = d.getLifeTotal(d.player1)
        d.activate(source, creatureZealot)
        d.state.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
        d.submitYesNo(d.player1, true).error shouldBe null
        // Exactly one legal permanent: it is sacrificed without a choice, or offered alone.
        (d.state.pendingDecision as? SelectCardsDecision)?.let { decision ->
            decision.options shouldBe listOf(bears)
            d.submitCardSelection(d.player1, listOf(bears)).error shouldBe null
        }
        d.findPermanent(d.player1, "Grizzly Bears") shouldBe null
        d.findPermanent(d.player1, creatureZealot.name) shouldBe source
        d.getLifeTotal(d.player1) shouldBe life + 3
    }

    test("a source that is an artifact may sacrifice itself") {
        val d = driver()
        val source = d.putCreatureOnBattlefield(d.player1, artifactZealot.name)
        val life = d.getLifeTotal(d.player1)
        d.activate(source, artifactZealot)
        withClue("the artifact branch has no 'another'") {
            d.state.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
        }
        d.submitYesNo(d.player1, true).error shouldBe null
        (d.state.pendingDecision as? SelectCardsDecision)?.let { decision ->
            decision.options shouldBe listOf(source)
            d.submitCardSelection(d.player1, listOf(source)).error shouldBe null
        }
        d.findPermanent(d.player1, artifactZealot.name) shouldBe null
        d.getLifeTotal(d.player1) shouldBe life + 3
    }
})
