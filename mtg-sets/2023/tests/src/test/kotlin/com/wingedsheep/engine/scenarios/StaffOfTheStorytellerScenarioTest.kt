package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.onc.cards.StaffOfTheStoryteller
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Staff of the Storyteller (ONC #10) — {1}{W} Artifact.
 *
 * "When this artifact enters, create a 1/1 white Spirit creature token with flying.
 *  Whenever you create one or more creature tokens, put a story counter on this artifact.
 *  {W}, {T}, Remove a story counter from this artifact: Draw a card."
 *
 * The middle line is the batched token-creation trigger: one story counter per creation however
 * many creature tokens it made, none for noncreature tokens, none for an opponent's tokens.
 */
class StaffOfTheStorytellerScenarioTest : ScenarioTestBase() {

    init {
        val drawAbilityId = StaffOfTheStoryteller.activatedAbilities.single().id

        fun story(game: TestGame, id: EntityId): Int =
            game.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.STORY) ?: 0

        fun addStory(game: TestGame, id: EntityId, count: Int) {
            game.state = game.state.updateEntity(id) { container ->
                val existing = container.get<CountersComponent>() ?: CountersComponent()
                container.with(existing.withAdded(CounterType.STORY, count))
            }
        }

        test("entering creates a flying Spirit, and that creation puts the first story counter on it") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Staff of the Storyteller")
                .withLandsOnBattlefield(1, "Plains", 2)
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(2, "Plains")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Staff of the Storyteller").error shouldBe null
            game.resolveStack()

            val staff = game.findPermanent("Staff of the Storyteller")!!
            game.findPermanents("Spirit Token").size shouldBe 1
            withClue("the Spirit it created is a creature token, so the Staff counts it") {
                story(game, staff) shouldBe 1
            }
        }

        test("two creature tokens created at once add a single story counter") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Staff of the Storyteller")
                .withCardInHand(1, "Raise the Alarm")
                .withLandsOnBattlefield(1, "Plains", 2)
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(2, "Plains")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val staff = game.findPermanent("Staff of the Storyteller")!!
            game.castSpell(1, "Raise the Alarm").error shouldBe null
            game.resolveStack()

            game.findPermanents("Soldier Token").size shouldBe 2
            story(game, staff) shouldBe 1
        }

        test("noncreature tokens don't add a story counter") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Staff of the Storyteller")
                .withCardInHand(1, "Strike It Rich")
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(2, "Plains")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val staff = game.findPermanent("Staff of the Storyteller")!!
            game.castSpell(1, "Strike It Rich").error shouldBe null
            game.resolveStack()

            game.findPermanents("Treasure").size shouldBe 1
            story(game, staff) shouldBe 0
        }

        test("an opponent's creature tokens don't add a story counter") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Staff of the Storyteller")
                .withCardInHand(2, "Raise the Alarm")
                .withLandsOnBattlefield(2, "Plains", 2)
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(2, "Plains")
                .withActivePlayer(2)
                .withPriorityPlayer(2)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val staff = game.findPermanent("Staff of the Storyteller")!!
            game.castSpell(2, "Raise the Alarm").error shouldBe null
            game.resolveStack()

            game.findPermanents("Soldier Token").size shouldBe 2
            story(game, staff) shouldBe 0
        }

        test("{W}, {T}, remove a story counter: draw a card") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Staff of the Storyteller")
                .withLandsOnBattlefield(1, "Plains", 1)
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(2, "Plains")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val staff = game.findPermanent("Staff of the Storyteller")!!
            addStory(game, staff, 2)
            val handBefore = game.handSize(1)

            val result = game.execute(
                ActivateAbility(playerId = game.player1Id, sourceId = staff, abilityId = drawAbilityId)
            )
            result.error shouldBe null
            if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
            game.resolveStack()

            game.handSize(1) shouldBe handBefore + 1
            story(game, staff) shouldBe 1
        }

        test("the draw ability can't be activated without a story counter") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Staff of the Storyteller")
                .withLandsOnBattlefield(1, "Plains", 1)
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(2, "Plains")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val staff = game.findPermanent("Staff of the Storyteller")!!
            val result = game.execute(
                ActivateAbility(playerId = game.player1Id, sourceId = staff, abilityId = drawAbilityId)
            )
            result.error shouldNotBe null
        }
    }
}
