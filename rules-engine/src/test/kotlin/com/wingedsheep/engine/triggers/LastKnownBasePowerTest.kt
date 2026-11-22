package com.wingedsheep.engine.triggers

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.EntityNumericProperty
import io.kotest.matchers.shouldBe

/**
 * Base power and toughness read as a departed creature last existed on the battlefield (CR 608.2h).
 * Off the battlefield only the printed stat is readable, so a creature that died while a
 * "base power and toughness 4/4" effect (layer 7b, CR 613.4b) applied must still report base 4 —
 * through `EntityProperty(TriggeringEntity, BasePower)` and through the dies-trigger filters
 * `basePower(n)` and `powerGreaterThanBase()`.
 */
class LastKnownBasePowerTest : ScenarioTestBase() {

    private val setBoth = card("LKI Base 4/4") {
        manaCost = "{U}"
        typeLine = "Instant"
        oracleText = "Target creature has base power and toughness 4/4 until end of turn."
        spell {
            val creature = target(TargetFilter.Creature)
            effect = Effects.SetBasePowerAndToughness(4, 4, creature, Duration.EndOfTurn)
        }
    }

    private val kill = card("LKI Kill") {
        manaCost = "{U}"
        typeLine = "Instant"
        oracleText = "Destroy target creature."
        spell {
            val creature = target(TargetFilter.Creature)
            effect = Effects.Destroy(creature)
        }
    }

    private val baseDrawer = card("LKI Base Drawer") {
        manaCost = "{U}"
        typeLine = "Enchantment"
        oracleText = "Whenever a creature you control dies, draw cards equal to its base power."
        triggeredAbility {
            trigger = Triggers.a(GameObjectFilter.Creature.youControl()).dies()
            effect = Effects.DrawCards(
                DynamicAmounts.propertyOf(EffectTarget.TriggeringEntity, EntityNumericProperty.BasePower)
            )
        }
    }

    private val baseFourWatcher = card("LKI Base Four Watcher") {
        manaCost = "{U}"
        typeLine = "Enchantment"
        oracleText = "Whenever a creature you control with base power 4 dies, you gain 4 life."
        triggeredAbility {
            trigger = Triggers.a(GameObjectFilter.Creature.basePower(4).youControl()).dies()
            effect = Effects.GainLife(4)
        }
    }

    private val pumpedWatcher = card("LKI Pumped Watcher") {
        manaCost = "{U}"
        typeLine = "Enchantment"
        oracleText = "Whenever a creature you control with power greater than its base power dies, you gain 1 life."
        triggeredAbility {
            trigger = Triggers.a(GameObjectFilter.Creature.powerGreaterThanBase().youControl()).dies()
            effect = Effects.GainLife(1)
        }
    }

    private val sacDrawer = card("LKI Sac Drawer") {
        manaCost = "{U}"
        typeLine = "Artifact"
        oracleText = "Sacrifice a creature: Draw cards equal to the sacrificed creature's base power."
        activatedAbility {
            cost = Costs.Sacrifice(GameObjectFilter.Creature)
            effect = Effects.DrawCards(
                DynamicAmounts.propertyOf(EffectTarget.SacrificedAsCost(), EntityNumericProperty.BasePower)
            )
        }
    }

    init {
        cardRegistry.register(listOf(setBoth, kill, baseDrawer, baseFourWatcher, pumpedWatcher, sacDrawer))

        fun board(watcher: String, setBase: Boolean = true) = scenario()
            .withPlayers()
            .apply { if (setBase) withCardInHand(1, "LKI Base 4/4") }
            .withCardInHand(1, "LKI Kill")
            .withCardOnBattlefield(1, watcher)
            .withCardOnBattlefield(1, "Grizzly Bears") // printed 2/2
            .withLandsOnBattlefield(1, "Island", 2)
            .withCardInLibrary(1, "Island")
            .withCardInLibrary(1, "Island")
            .withCardInLibrary(1, "Island")
            .withCardInLibrary(1, "Island")
            .withCardInLibrary(1, "Island")
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()

        fun ScenarioTestBase.TestGame.setBaseThenKill(withCounter: Boolean = false) {
            val bear = findPermanent("Grizzly Bears")!!
            castSpell(1, "LKI Base 4/4", bear).error shouldBe null
            resolveStack()
            if (withCounter) {
                state = state.updateEntity(bear) {
                    it.with(CountersComponent(mapOf(CounterType.PLUS_ONE_PLUS_ONE to 1)))
                }
            }
            castSpell(1, "LKI Kill", bear).error shouldBe null
            resolveStack()
            findPermanent("Grizzly Bears") shouldBe null
        }

        test("EntityProperty(TriggeringEntity, BasePower) reads the set base power, not the printed one") {
            val game = board("LKI Base Drawer")
            val hand = game.handSize(1)
            game.setBaseThenKill()
            // Cast two spells (-2), drew four for base power 4.
            game.handSize(1) shouldBe hand - 2 + 4
        }

        test("EntityProperty(TriggeringEntity, BasePower) falls back to the printed base when nothing set it") {
            val game = board("LKI Base Drawer", setBase = false)
            val hand = game.handSize(1)
            val bear = game.findPermanent("Grizzly Bears")!!
            game.castSpell(1, "LKI Kill", bear).error shouldBe null
            game.resolveStack()
            game.handSize(1) shouldBe hand - 1 + 2
        }

        test("a creature sacrificed as a cost reports its set base power from the cost snapshot") {
            val game = board("LKI Sac Drawer")
            val bear = game.findPermanent("Grizzly Bears")!!
            game.castSpell(1, "LKI Base 4/4", bear).error shouldBe null
            game.resolveStack()
            val drawer = game.findPermanent("LKI Sac Drawer")!!
            val hand = game.handSize(1)
            game.execute(
                ActivateAbility(
                    game.player1Id, drawer, sacDrawer.activatedAbilities.single().id,
                    costPayment = AdditionalCostPayment(sacrificedPermanents = listOf(bear)),
                )
            ).error shouldBe null
            game.resolveStack()
            game.handSize(1) shouldBe hand + 4
        }

        test("a dies trigger filtered on base power matches the last-known set base power") {
            val game = board("LKI Base Four Watcher")
            val life = game.getLifeTotal(1)
            game.setBaseThenKill()
            game.getLifeTotal(1) shouldBe life + 4
        }

        test("power equal to a set base power is not greater than it, as last known") {
            val game = board("LKI Pumped Watcher")
            val life = game.getLifeTotal(1)
            game.setBaseThenKill()
            game.getLifeTotal(1) shouldBe life
        }

        test("a counter on top of a set base power is greater than it, as last known") {
            val game = board("LKI Pumped Watcher")
            val life = game.getLifeTotal(1)
            game.setBaseThenKill(withCounter = true)
            game.getLifeTotal(1) shouldBe life + 1
        }
    }
}
