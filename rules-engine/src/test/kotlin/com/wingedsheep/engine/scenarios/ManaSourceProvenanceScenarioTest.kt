package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.events.SpellCastPredicate
import io.kotest.matchers.shouldBe

/**
 * "Whenever you cast a … spell using mana produced by this" — the mana-source provenance the engine
 * records ([SpellCastPredicate.PaidWithManaFromSource]). Exercises the full production path: a land's
 * mana ability tags the mana it makes with the land's entity id ([com.wingedsheep.engine.handlers.effects.mana.ManaProvenanceTracker]),
 * the payment consumes that tag and stamps it on the [com.wingedsheep.engine.core.SpellCastEvent], and
 * the source's own trigger matches its id against the spent-mana sources. This is the shape Tecutlan /
 * Barracks of the Thousand / The Myriad Pools use.
 */
class ManaSourceProvenanceScenarioTest : ScenarioTestBase() {

    init {
        cardRegistry.register(listOf(ProvenanceRift, PlainRift, ProvenanceBear))

        context("cast using mana produced by this source") {
            test("mana tapped from the source fires its own cast trigger") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Provenance Rift")
                    .withCardInHand(1, "Provenance Bear")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val riftId = game.findPermanent("Provenance Rift")!!
                val manaAbilityId = ProvenanceRift.activatedAbilities.first { it.isManaAbility }.id

                // Tap the Rift for {G} — the mana is tagged with the Rift's entity id.
                game.execute(ActivateAbility(game.player1Id, riftId, manaAbilityId)).error shouldBe null

                // Cast the creature spending that mana; the Rift's trigger sees its own mana.
                game.castSpell(1, "Provenance Bear").error shouldBe null
                game.resolveStack()

                game.getLifeTotal(1) shouldBe 25
            }

            test("mana from another source does not fire the trigger") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Provenance Rift")
                    .withCardInHand(1, "Provenance Bear")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Pay with plain green mana carrying no source provenance.
                game.state = game.state.updateEntity(game.player1Id) { container ->
                    container.with(ManaPoolComponent(green = 1))
                }

                game.castSpell(1, "Provenance Bear").error shouldBe null
                game.resolveStack()

                game.getLifeTotal(1) shouldBe 20
            }

            // Regression: floating mana from a *second* source must not wipe the first source's
            // provenance. Previously ActivateAbilityHandler rebuilt the pool without the provenance
            // maps when a mana ability resolved, so tapping a second land dropped the Rift's tag and
            // its trigger silently missed. Tap the Rift, then another land, then cast — the Rift's
            // mana is still in the pool and its trigger fires.
            test("floating mana from a second source does not wipe the first source's provenance") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Provenance Rift")
                    .withCardOnBattlefield(1, "Plain Rift")
                    .withCardInHand(1, "Provenance Bear")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val riftId = game.findPermanent("Provenance Rift")!!
                val plainId = game.findPermanent("Plain Rift")!!
                val riftMana = ProvenanceRift.activatedAbilities.first { it.isManaAbility }.id
                val plainMana = PlainRift.activatedAbilities.first { it.isManaAbility }.id

                // Float {G} from the Rift, then {G} from another land: pool = {G}{G}, both tagged.
                game.execute(ActivateAbility(game.player1Id, riftId, riftMana)).error shouldBe null
                game.execute(ActivateAbility(game.player1Id, plainId, plainMana)).error shouldBe null

                game.castSpell(1, "Provenance Bear").error shouldBe null
                game.resolveStack()

                game.getLifeTotal(1) shouldBe 25
            }
        }
    }

    companion object {
        /** A land that taps for {G} and rewards casting a permanent spell with its own mana. */
        private val ProvenanceRift = card("Provenance Rift") {
            typeLine = "Land"
            activatedAbility {
                cost = Costs.Tap
                effect = Effects.AddMana(Color.GREEN, 1)
                manaAbility = true
                timing = TimingRule.ManaAbility
            }
            triggeredAbility {
                trigger = Triggers.you.casts(GameObjectFilter.Permanent, requires = setOf(SpellCastPredicate.PaidWithManaFromSource))
                effect = Effects.GainLife(5)
            }
        }

        /** A plain land that taps for {G} with no cast payoff — a second, untriggered mana source. */
        private val PlainRift = card("Plain Rift") {
            typeLine = "Land"
            activatedAbility {
                cost = Costs.Tap
                effect = Effects.AddMana(Color.GREEN, 1)
                manaAbility = true
                timing = TimingRule.ManaAbility
            }
        }

        private val ProvenanceBear = card("Provenance Bear") {
            manaCost = "{G}"
            typeLine = "Creature — Bear"
            power = 2
            toughness = 2
        }
    }
}
