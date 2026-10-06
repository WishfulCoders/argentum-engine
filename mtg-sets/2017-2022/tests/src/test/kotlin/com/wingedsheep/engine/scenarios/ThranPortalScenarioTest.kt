package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.dmu.cards.ThranPortal
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.AbilityId
import com.wingedsheep.sdk.scripting.costs.CostAtom
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Thran Portal (DMU #259):
 *   This land enters tapped unless you control two or fewer other lands.
 *   As this land enters, choose a basic land type.
 *   This land is the chosen type in addition to its other types.
 *   Mana abilities of this land cost an additional 1 life to activate.
 *
 * Covers the chosen type (Gate *and* the chosen basic type, with that type's intrinsic mana
 * ability), the conditional enters-tapped, and the life tax on both the manual activation and
 * the auto-pay path — including that auto-pay prefers an untaxed source.
 */
class ThranPortalScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + ThranPortal)
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.redInPool(playerId: EntityId): Int =
        state.getEntity(playerId)?.get<ManaPoolComponent>()?.red ?: 0

    fun GameTestDriver.playPortalChoosing(player: EntityId, landType: String): EntityId {
        val portal = putCardInHand(player, "Thran Portal")
        (playLand(player, portal).outcome is Outcome.Paused) shouldBe true
        val choice = pendingDecision
        choice.shouldBeInstanceOf<ChooseOptionDecision>()
        choice.options shouldContain landType
        submitDecision(player, OptionChosenResponse(choice.id, choice.options.indexOf(landType)))
        pendingDecision shouldBe null
        return portal
    }

    test("enters untapped with two or fewer other lands and is a Gate and the chosen type") {
        val driver = createDriver()
        val p1 = driver.activePlayer!!
        driver.putLandOnBattlefield(p1, "Plains")
        driver.putLandOnBattlefield(p1, "Plains")

        val portal = driver.playPortalChoosing(p1, "Mountain")

        driver.isTapped(portal).shouldBeFalse()
        driver.state.projectedState.hasSubtype(portal, "Mountain").shouldBeTrue()
        driver.state.projectedState.hasSubtype(portal, "Gate").shouldBeTrue()
    }

    test("enters tapped with three or more other lands") {
        val driver = createDriver()
        val p1 = driver.activePlayer!!
        repeat(3) { driver.putLandOnBattlefield(p1, "Plains") }

        val portal = driver.playPortalChoosing(p1, "Island")

        driver.isTapped(portal).shouldBeTrue()
        driver.state.projectedState.hasSubtype(portal, "Island").shouldBeTrue()
    }

    test("its intrinsic mana ability is offered with the extra life cost, and activating it pays 1 life") {
        val driver = createDriver()
        val p1 = driver.activePlayer!!
        val portal = driver.playPortalChoosing(p1, "Mountain")

        val offered = driver.legalActions(p1)
            .map { it.action }
            .filterIsInstance<ActivateAbility>()
            .firstOrNull { it.sourceId == portal && it.abilityId == AbilityId.intrinsicMana('R') }
        offered.shouldNotBeNull()

        driver.submitSuccess(ActivateAbility(p1, portal, AbilityId.intrinsicMana('R')))

        driver.redInPool(p1) shouldBe 1
        driver.getLifeTotal(p1) shouldBe 19
        driver.isTapped(portal).shouldBeTrue()
    }

    test("auto-pay tapping it for a spell charges 1 life") {
        val driver = createDriver()
        val p1 = driver.activePlayer!!
        driver.playPortalChoosing(p1, "Mountain")

        val guide = driver.putCardInHand(p1, "Goblin Guide")
        driver.castSpell(p1, guide).error shouldBe null

        driver.getLifeTotal(p1) shouldBe 19
        driver.getStackSpellNames() shouldContain "Goblin Guide"
    }

    test("auto-pay prefers an untaxed source of the same color") {
        val driver = createDriver()
        val p1 = driver.activePlayer!!
        val mountain = driver.putLandOnBattlefield(p1, "Mountain")
        val portal = driver.playPortalChoosing(p1, "Mountain")

        val guide = driver.putCardInHand(p1, "Goblin Guide")
        driver.castSpell(p1, guide).error shouldBe null

        driver.getLifeTotal(p1) shouldBe 20
        driver.isTapped(mountain).shouldBeTrue()
        driver.isTapped(portal).shouldBeFalse()
    }

    test("the tax is folded into the ability's cost as a life payment") {
        val driver = createDriver()
        val p1 = driver.activePlayer!!
        val portal = driver.playPortalChoosing(p1, "Forest")

        val tax = driver.state.projectedState.getManaAbilityLifeTax(portal)
        tax shouldBe 1
        val taxed = com.wingedsheep.engine.mechanics.mana.ManaAbilityLifeCost.withTax(
            com.wingedsheep.engine.mechanics.mana.IntrinsicManaAbilities.lookup(AbilityId.intrinsicMana('G'))!!,
            tax
        )
        val cost = taxed.cost
        cost.shouldBeInstanceOf<AbilityCost.Composite>()
        cost.costs shouldContain AbilityCost.Atom(CostAtom.PayLife(1))
    }
})
