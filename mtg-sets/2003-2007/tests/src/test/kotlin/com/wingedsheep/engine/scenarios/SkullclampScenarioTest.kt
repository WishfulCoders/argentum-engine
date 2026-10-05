package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.dst.cards.Skullclamp
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Skullclamp (DST #140).
 *
 *   Equipped creature gets +1/-1.
 *   Whenever equipped creature dies, draw two cards.
 *   Equip {1}
 *
 * The load-bearing case is equipping a 1-toughness creature: it dies to the state-based check right
 * after the equip resolves, and the dies trigger must still see it as "equipped" from last-known
 * information.
 */
class SkullclampScenarioTest : ScenarioTestBase() {

    private val stateProjector = StateProjector()
    private val equipAbility = Skullclamp.activatedAbilities.single { it.isEquipAbility }

    init {
        context("Skullclamp") {

            test("equipping a 1-toughness creature kills it and draws two cards") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardOnBattlefield(1, "Skullclamp")
                    .withCardOnBattlefield(1, "Llanowar Elves")
                    .withLandsOnBattlefield(1, "Plains", 1)
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(2, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val clamp = game.findPermanent("Skullclamp")!!
                val elves = game.findPermanent("Llanowar Elves")!!
                val handBefore = game.handSize(1)

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = clamp,
                        abilityId = equipAbility.id,
                        targets = listOf(ChosenTarget.Permanent(elves))
                    )
                )
                withClue("Equip should activate: ${result.error}") { result.error shouldBe null }
                if (game.hasPendingDecision()) game.submitManaSourcesAutoPay()
                game.resolveStack()

                withClue("The 1/1 became a 2/0 and died") {
                    game.isInGraveyard(1, "Llanowar Elves") shouldBe true
                }
                withClue("The dies trigger drew two cards") {
                    game.handSize(1) shouldBe handBefore + 2
                }
                withClue("Skullclamp stays on the battlefield, unattached") {
                    game.isOnBattlefield("Skullclamp") shouldBe true
                }
            }

            test("equipped creature gets +1/-1, and dying to a spell still draws two") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardAttachedTo(1, "Skullclamp", "Grizzly Bears")
                    .withCardInHand(2, "Shock")
                    .withLandsOnBattlefield(2, "Mountain", 1)
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(2, "Island")
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                val projected = stateProjector.project(game.state)
                withClue("Equipped Grizzly Bears is a 3/1") {
                    projected.getPower(bears) shouldBe 3
                    projected.getToughness(bears) shouldBe 1
                }

                val handBefore = game.handSize(1)
                val cast = game.castSpell(2, "Shock", bears)
                withClue("Shock should cast: ${cast.error}") { cast.error shouldBe null }
                if (game.hasPendingDecision()) game.submitManaSourcesAutoPay()
                game.resolveStack()

                withClue("Grizzly Bears died") { game.isInGraveyard(1, "Grizzly Bears") shouldBe true }
                withClue("Skullclamp's controller drew two cards") {
                    game.handSize(1) shouldBe handBefore + 2
                }
            }
        }
    }
}
