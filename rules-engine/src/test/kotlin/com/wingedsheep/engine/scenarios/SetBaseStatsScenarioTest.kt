package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import io.kotest.matchers.shouldBe
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * The unified [com.wingedsheep.sdk.scripting.effects.SetBaseStatsEffect] (reached through the
 * `Effects.SetBasePower` / `Effects.SetBaseToughness` / `Effects.SetBasePowerAndToughness` facades)
 * sets base power, base toughness, or both via a Layer 7b SET_VALUES floating effect. A `null` stat
 * leaves that stat unchanged.
 *
 * This pins all three shapes on the same 2/2 — in particular the power-only and toughness-only paths
 * must leave the *other* stat at its printed value (the asymmetry the two predecessor effects could
 * not express in one type).
 */
class SetBaseStatsScenarioTest : ScenarioTestBase() {

    private val setPowerOnly = card("Set Base Power Test") {
        manaCost = "{U}"
        typeLine = "Instant"
        oracleText = "Target creature has base power 7 until end of turn."
        spell {
            val creature = target(TargetFilter.Creature)
            effect = Effects.SetBasePower(creature, DynamicAmount.Fixed(7), Duration.EndOfTurn)
        }
    }

    private val setToughnessOnly = card("Set Base Toughness Test") {
        manaCost = "{U}"
        typeLine = "Instant"
        oracleText = "Target creature has base toughness 7 until end of turn."
        spell {
            val creature = target(TargetFilter.Creature)
            effect = Effects.SetBaseToughness(creature, DynamicAmount.Fixed(7), Duration.EndOfTurn)
        }
    }

    private val setBoth = card("Set Base Both Test") {
        manaCost = "{U}"
        typeLine = "Instant"
        oracleText = "Target creature has base power and toughness 4/4 until end of turn."
        spell {
            val creature = target(TargetFilter.Creature)
            effect = Effects.SetBasePowerAndToughness(4, 4, creature, Duration.EndOfTurn)
        }
    }

    init {
        cardRegistry.register(listOf(setPowerOnly, setToughnessOnly, setBoth))

        fun build(spellName: String) = scenario()
            .withPlayers()
            .withCardInHand(1, spellName)
            .withCardOnBattlefield(1, "Grizzly Bears") // printed 2/2
            .withCardOnBattlefield(1, "Island")
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()

        test("SetBasePower sets only base power, leaving toughness at its printed value") {
            val game = build("Set Base Power Test")
            val bear = game.findPermanent("Grizzly Bears")!!
            game.state.projectedState.getPower(bear) shouldBe 2
            game.state.projectedState.getToughness(bear) shouldBe 2

            game.castSpell(1, "Set Base Power Test", bear).error shouldBe null
            game.resolveStack()

            game.state.projectedState.getPower(bear) shouldBe 7
            game.state.projectedState.getToughness(bear) shouldBe 2
        }

        test("SetBaseToughness sets only base toughness, leaving power at its printed value") {
            val game = build("Set Base Toughness Test")
            val bear = game.findPermanent("Grizzly Bears")!!

            game.castSpell(1, "Set Base Toughness Test", bear).error shouldBe null
            game.resolveStack()

            game.state.projectedState.getPower(bear) shouldBe 2
            game.state.projectedState.getToughness(bear) shouldBe 7
        }

        test("SetBasePowerAndToughness sets both base power and toughness") {
            val game = build("Set Base Both Test")
            val bear = game.findPermanent("Grizzly Bears")!!

            game.castSpell(1, "Set Base Both Test", bear).error shouldBe null
            game.resolveStack()

            game.state.projectedState.getPower(bear) shouldBe 4
            game.state.projectedState.getToughness(bear) shouldBe 4
        }
    }
}
