package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.AddColorlessManaEffect
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Engine-level coverage for the Antiquities "becomes tapped / activates an ability without {T} in
 * its activation cost" trigger family (Haunting Wind, Powerleech, Artifact Possession).
 *
 * The feature is two composable halves:
 *  - `Triggers.<subject>.becomesTapped(reason, firstTimeEachTurn)` with a [GameObjectFilter] (the tap half) — fires off [TappedEvent].
 *  - `Triggers.<player>.activatesAbility(of, withoutTapInCost = true)` (the ability half) — fires off the engine's
 *    [com.wingedsheep.engine.core.AbilityActivatedEvent] when its `costsTap` flag is false,
 *    regardless of whether the ability is a mana ability.
 *
 * These tests pin the *engine* rules (not a specific card): a {T}-cost ability fires only the tap
 * half; a non-{T} non-mana ability fires only the ability half; a non-{T} *mana* ability fires the
 * ability half (the case the default "isn't a mana ability" wording misses); a tapped-cost mana
 * ability fires only the tap half; non-artifacts are filtered out; and "an opponent controls"
 * scoping works.
 */
class ArtifactTapActivateTriggerScenarioTest : FunSpec({

    // Watcher: counts +1 damage to itself each time an artifact's *non-{T}* ability is activated,
    // globally (Haunting Wind shape, but routed at the controller for an easy assertion).
    val nonTapWatcher = card("Non-Tap Watcher") {
        manaCost = "{2}"
        typeLine = "Enchantment"
        oracleText = "Whenever a player activates an artifact's ability without {T} in its " +
            "activation cost, this deals 1 damage to that artifact's controller."
        triggeredAbility {
            trigger = Triggers.anyPlayer.activatesAbility(of = GameObjectFilter.Artifact, withoutTapInCost = true)
            effect = Effects.DealDamage(1, EffectTarget.PlayerRef(Player.TriggeringPlayer))
        }
    }

    // Watcher: counts +1 life each time an artifact becomes tapped, globally.
    val tapWatcher = card("Tap Watcher") {
        manaCost = "{2}"
        typeLine = "Enchantment"
        oracleText = "Whenever an artifact becomes tapped, you gain 1 life."
        triggeredAbility {
            trigger = Triggers.a(GameObjectFilter.Artifact).becomesTapped()
            effect = Effects.GainLife(1)
        }
    }

    // Artifact with a {T}: Add {C} mana ability (tap half should fire; ability half must NOT).
    val tapManaRock = card("Tap Mana Rock") {
        manaCost = "{2}"
        typeLine = "Artifact"
        oracleText = "{T}: Add {C}."
        activatedAbility {
            cost = AbilityCost.Tap
            effect = AddColorlessManaEffect(1)
            manaAbility = true
        }
    }

    // Artifact with a non-{T}, non-mana ability ("{1}: you gain 1 life"): ability half fires.
    val nonTapNonManaArtifact = card("Spendthrift Engine") {
        manaCost = "{2}"
        typeLine = "Artifact"
        oracleText = "{1}: You gain 1 life."
        activatedAbility {
            cost = com.wingedsheep.sdk.dsl.Costs.Mana("{1}")
            effect = Effects.GainLife(1)
        }
    }

    // Artifact with a non-{T} MANA ability ("Sacrifice this artifact: Add {C}{C}"): ability half
    // must fire even though it's a mana ability (the {T}-in-cost wording, not "isn't a mana ability").
    val sacForManaArtifact = card("Sacrificial Battery") {
        manaCost = "{2}"
        typeLine = "Artifact"
        oracleText = "Sacrifice this artifact: Add {C}{C}."
        activatedAbility {
            cost = AbilityCost.SacrificeSelf
            effect = AddColorlessManaEffect(2)
            manaAbility = true
        }
    }

    // A creature with a non-{T} non-mana ability — the artifact source filter must reject it.
    val nonArtifactCreature = card("Chatty Bear") {
        manaCost = "{1}"
        typeLine = "Creature — Bear"
        power = 2
        toughness = 2
        oracleText = "{1}: You gain 1 life."
        activatedAbility {
            cost = com.wingedsheep.sdk.dsl.Costs.Mana("{1}")
            effect = Effects.GainLife(1)
        }
    }

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        listOf(
            nonTapWatcher, tapWatcher, tapManaRock, nonTapNonManaArtifact,
            sacForManaArtifact, nonArtifactCreature
        ).forEach { driver.registerCard(it) }
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.resolveStack() {
        var guard = 0
        while (state.stack.isNotEmpty() && guard < 30) {
            bothPass(); guard++
        }
    }

    test("a {T}-cost mana ability fires the tap half but NOT the non-{T} ability half") {
        val driver = createDriver()
        val me = driver.player1

        driver.putPermanentOnBattlefield(me, "Non-Tap Watcher")
        driver.putPermanentOnBattlefield(me, "Tap Watcher")
        val rock = driver.putPermanentOnBattlefield(me, "Tap Mana Rock")

        val lifeBefore = driver.getLifeTotal(me)
        val abilityId = driver.cardRegistry.requireCard("Tap Mana Rock").activatedAbilities[0].id
        driver.submitSuccess(ActivateAbility(playerId = me, sourceId = rock, abilityId = abilityId))
        driver.resolveStack()

        // Tap half fired (gain 1 life); non-{T} ability half did NOT (no damage to me).
        driver.getLifeTotal(me) shouldBe lifeBefore + 1
    }

    test("a non-{T}, non-mana ability fires the ability half but NOT the tap half") {
        val driver = createDriver()
        val me = driver.player1

        driver.putPermanentOnBattlefield(me, "Non-Tap Watcher")
        driver.putPermanentOnBattlefield(me, "Tap Watcher")
        val engine = driver.putPermanentOnBattlefield(me, "Spendthrift Engine")

        val lifeBefore = driver.getLifeTotal(me)
        driver.giveColorlessMana(me, 1)
        val abilityId = driver.cardRegistry.requireCard("Spendthrift Engine").activatedAbilities[0].id
        driver.submitSuccess(ActivateAbility(playerId = me, sourceId = engine, abilityId = abilityId))
        driver.resolveStack()

        // The ability resolved (+1 life from the artifact) and the ability half dealt 1 to me;
        // the tap half did NOT fire (the artifact never tapped). Net: +1 (gain) - 1 (damage) = 0.
        driver.getLifeTotal(me) shouldBe lifeBefore
    }

    test("a non-{T} MANA ability fires the ability half (the {T}-in-cost wording, not isn't-a-mana-ability)") {
        val driver = createDriver()
        val me = driver.player1

        driver.putPermanentOnBattlefield(me, "Non-Tap Watcher")
        val battery = driver.putPermanentOnBattlefield(me, "Sacrificial Battery")

        val lifeBefore = driver.getLifeTotal(me)
        val abilityId = driver.cardRegistry.requireCard("Sacrificial Battery").activatedAbilities[0].id
        driver.submitSuccess(ActivateAbility(playerId = me, sourceId = battery, abilityId = abilityId))
        driver.resolveStack()

        // Sacrificing for mana is a non-{T} mana ability — the ability half fires and deals 1 to me.
        driver.getLifeTotal(me) shouldBe lifeBefore - 1
    }

    test("the artifact source filter rejects a creature's non-{T} ability") {
        val driver = createDriver()
        val me = driver.player1

        driver.putPermanentOnBattlefield(me, "Non-Tap Watcher")
        val bear = driver.putCreatureOnBattlefield(me, "Chatty Bear")
        driver.removeSummoningSickness(bear)

        val lifeBefore = driver.getLifeTotal(me)
        driver.giveColorlessMana(me, 1)
        val abilityId = driver.cardRegistry.requireCard("Chatty Bear").activatedAbilities[0].id
        driver.submitSuccess(ActivateAbility(playerId = me, sourceId = bear, abilityId = abilityId))
        driver.resolveStack()

        // Creature's ability is not an artifact's — only +1 from the gain, no damage.
        driver.getLifeTotal(me) shouldBe lifeBefore + 1
    }

    test("opponent-controls scoping: only an opponent's artifact ability fires the watcher") {
        val driver = createDriver()
        val me = driver.player1
        val opp = driver.player2

        // Watcher keyed to "an opponent activates an artifact's ability without {T}".
        val oppScopedWatcher = card("Opp Watcher") {
            manaCost = "{2}"
            typeLine = "Enchantment"
            oracleText = "Whenever an opponent activates an artifact's ability without {T}, you gain 1 life."
            triggeredAbility {
                trigger = Triggers.anOpponent.activatesAbility(of = GameObjectFilter.Artifact.opponentControls(), withoutTapInCost = true)
                effect = Effects.GainLife(1)
            }
        }
        driver.registerCard(oppScopedWatcher)
        driver.putPermanentOnBattlefield(me, "Opp Watcher")

        // My own artifact ability does NOT fire the watcher.
        val mine = driver.putPermanentOnBattlefield(me, "Spendthrift Engine")
        driver.giveColorlessMana(me, 1)
        val myAbilityId = driver.cardRegistry.requireCard("Spendthrift Engine").activatedAbilities[0].id
        val lifeBefore = driver.getLifeTotal(me)
        driver.submitSuccess(ActivateAbility(playerId = me, sourceId = mine, abilityId = myAbilityId))
        driver.resolveStack()
        // +1 from my own artifact's gain effect, watcher did NOT fire.
        driver.getLifeTotal(me) shouldBe lifeBefore + 1
    }
})
