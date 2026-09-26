package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldStartWith
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Feature tests for the **Exhaust** keyword (CR 702.177) — "Exhaust — [cost]: [effect]" means
 * "[cost]: [effect]. Activate only once."
 *
 * Exhaust is a marker flag (`isExhaust = true`) on an activated ability: the DSL desugars it to an
 * [ActivationRestriction.Once] (the once-per-object enforcement) and prefixes "Exhaust — " on the
 * ability text. The load-bearing rules claim these pin down:
 *  - the marker auto-adds the `Once` restriction and renders the prefix (no drift);
 *  - the ability can be activated exactly once for an object's lifetime;
 *  - per CR 400.7 / 403.4 a permanent that leaves and re-enters the battlefield is a *new object*
 *    whose exhaust ability may be activated **again** — i.e. it is per-object, NOT once per game.
 */
class ExhaustKeywordScenarioTest : ScenarioTestBase() {

    // "Exhaust — {1}: Put a +1/+1 counter on this creature."
    private val exhaustDummy = card("Exhaust Dummy") {
        manaCost = "{2}"
        typeLine = "Creature — Spirit"
        power = 1
        toughness = 1
        oracleText = "Exhaust — {1}: Put a +1/+1 counter on this creature. (Activate each exhaust ability only once.)"
        activatedAbility {
            isExhaust = true
            cost = Costs.Mana("{1}")
            effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
        }
    }

    // A {0} sorcery that returns the exhaust creature to its owner's hand, so we can replay it as a
    // new object and prove the exhaust limit resets.
    private val bounce = card("Recall") {
        manaCost = "{0}"
        typeLine = "Sorcery"
        oracleText = "Return target creature to its owner's hand."
        spell {
            val t = target(TargetFilter.Creature)
            effect = Effects.ReturnToHand(t)
        }
    }

    private val abilityId get() = cardRegistry.getCard("Exhaust Dummy")!!.script.activatedAbilities[0].id

    init {
        cardRegistry.register(exhaustDummy)
        cardRegistry.register(bounce)

        context("Exhaust keyword") {

            test("isExhaust desugars to ActivationRestriction.Once and prefixes the description") {
                val ability = cardRegistry.getCard("Exhaust Dummy")!!.script.activatedAbilities[0]
                ability.isExhaust shouldBe true
                withClue("the marker must carry the once-per-object enforcement") {
                    ability.restrictions.contains(ActivationRestriction.Once) shouldBe true
                }
                ability.description shouldStartWith "Exhaust — {1}:"
            }

            test("can be activated once, then a second activation is illegal") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Exhaust Dummy", summoningSickness = false)
                    .withLandsOnBattlefield(1, "Mountain", 2) // two activations' worth of mana available
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val dummyId = game.findPermanent("Exhaust Dummy")!!

                val first = game.execute(ActivateAbility(game.player1Id, dummyId, abilityId))
                withClue("first activation should succeed: ${first.error}") { first.error shouldBe null }
                if (game.getPendingDecision() is com.wingedsheep.engine.core.SelectManaSourcesDecision) {
                    game.submitManaSourcesAutoPay()
                }
                game.resolveStack()

                game.state.getEntity(dummyId)?.get<CountersComponent>()
                    ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 1

                // The ability is no longer offered, and a forced re-activation is rejected.
                withClue("an exhausted ability must not appear in legal actions") {
                    game.getLegalActions(1).any { it.description.startsWith("Exhaust —") } shouldBe false
                }
                val second = game.execute(ActivateAbility(game.player1Id, dummyId, abilityId))
                withClue("second activation of the same object must be illegal") {
                    (second.error != null) shouldBe true
                }
                game.state.getEntity(dummyId)?.get<CountersComponent>()
                    ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 1
            }

            test("re-entering the battlefield is a new object — exhaust may be activated again (CR 400.7)") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Exhaust Dummy", summoningSickness = false)
                    .withCardInHand(1, "Recall")
                    .withLandsOnBattlefield(1, "Mountain", 4) // activate, recast {2}, activate again
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val firstObject = game.findPermanent("Exhaust Dummy")!!

                // Exhaust the first object.
                game.execute(ActivateAbility(game.player1Id, firstObject, abilityId)).error shouldBe null
                if (game.getPendingDecision() is com.wingedsheep.engine.core.SelectManaSourcesDecision) {
                    game.submitManaSourcesAutoPay()
                }
                game.resolveStack()

                // Bounce it and replay it — it returns as a brand-new object.
                game.castSpell(1, "Recall", firstObject).error shouldBe null
                game.resolveStack()
                game.isInHand(1, "Exhaust Dummy") shouldBe true
                game.castSpell(1, "Exhaust Dummy").error shouldBe null
                game.resolveStack()

                // The re-entered permanent is a new object (CR 400.7); even if the engine reuses the
                // entity id, its "activate only once" memory was stripped on leaving the battlefield.
                val secondObject = game.findPermanent("Exhaust Dummy")!!
                withClue("the recast permanent starts with no counters (CountersComponent was stripped)") {
                    game.state.getEntity(secondObject)?.get<CountersComponent>()
                        ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0 shouldBe 0
                }

                // The new object's exhaust ability is fresh: activating it succeeds and adds a counter.
                val reactivate = game.execute(ActivateAbility(game.player1Id, secondObject, abilityId))
                withClue("a new object may activate its exhaust ability again: ${reactivate.error}") {
                    reactivate.error shouldBe null
                }
                if (game.getPendingDecision() is com.wingedsheep.engine.core.SelectManaSourcesDecision) {
                    game.submitManaSourcesAutoPay()
                }
                game.resolveStack()

                game.state.getEntity(secondObject)?.get<CountersComponent>()
                    ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 1
            }
        }
    }
}
