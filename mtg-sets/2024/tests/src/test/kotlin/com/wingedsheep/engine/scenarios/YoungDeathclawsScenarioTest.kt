package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AbilityId
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe

/**
 * Young Deathclaws (PIP #125) — {2}{B}{G} Creature — Lizard Mutant 4/2.
 *
 *   Menace
 *   Each creature card in your graveyard has scavenge. The scavenge cost is equal to its mana cost.
 */
class YoungDeathclawsScenarioTest : ScenarioTestBase() {
    private val scavenge = AbilityId("scavenge_own")

    private fun TestGame.scavengeActions(card: String) = getLegalActions(1)
        .mapNotNull { it.action as? ActivateAbility }
        .filter { it.abilityId == scavenge && it.sourceId in findCardsInGraveyard(1, card) }

    private fun TestGame.plusOnes(id: EntityId) =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    init {
        test("a graveyard creature scavenges for its own mana cost, adding counters equal to its power") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Young Deathclaws")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardInGraveyard(1, "Hill Giant")
                .withLandsOnBattlefield(1, "Mountain", 4)
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(2, "Forest")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val bears = game.findPermanent("Grizzly Bears")!!

            val action = game.scavengeActions("Hill Giant").single()
            game.execute(action.copy(targets = listOf(ChosenTarget.Permanent(bears)))).error shouldBe null
            // Exiling the card is part of the cost: it's gone before the ability resolves.
            game.isInExile(1, "Hill Giant") shouldBe true
            game.resolveStack()

            game.plusOnes(bears) shouldBe 3
        }

        test("the scavenge cost keeps the card's coloured mana requirements") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Young Deathclaws")
                .withCardInGraveyard(1, "Hill Giant")
                .withLandsOnBattlefield(1, "Forest", 6)
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(2, "Forest")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.scavengeActions("Hill Giant").shouldBeEmpty()
        }

        test("only as a sorcery, and only while Young Deathclaws is on the battlefield") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Young Deathclaws")
                .withCardInGraveyard(1, "Hill Giant")
                .withLandsOnBattlefield(1, "Mountain", 4)
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(2, "Forest")
                .withActivePlayer(1)
                .inPhase(Phase.BEGINNING, Step.UPKEEP)
                .build()
            game.scavengeActions("Hill Giant").shouldBeEmpty()

            val noDeathclaws = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInGraveyard(1, "Hill Giant")
                .withLandsOnBattlefield(1, "Mountain", 4)
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(2, "Forest")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            noDeathclaws.scavengeActions("Hill Giant").shouldBeEmpty()
        }

        test("counters use the card's power as it last existed in the graveyard") {
            // In the graveyard Tarmogoyf sees a creature and an instant (2); once exiled, only
            // the instant is left (1). The ruling counts the graveyard value.
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Young Deathclaws")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardInGraveyard(1, "Tarmogoyf")
                .withCardInGraveyard(2, "Lightning Bolt")
                .withLandsOnBattlefield(1, "Forest", 2)
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(2, "Forest")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val bears = game.findPermanent("Grizzly Bears")!!

            val action = game.scavengeActions("Tarmogoyf").single()
            game.execute(action.copy(targets = listOf(ChosenTarget.Permanent(bears)))).error shouldBe null
            game.resolveStack()

            game.plusOnes(bears) shouldBe 2
        }

        test("a printed {0} is payable; a card with no mana cost can't scavenge") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Young Deathclaws")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardInGraveyard(1, "Memnite")
                .withCardInGraveyard(1, "Dryad Arbor")
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(2, "Forest")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val bears = game.findPermanent("Grizzly Bears")!!

            game.scavengeActions("Dryad Arbor").shouldBeEmpty()
            val action = game.scavengeActions("Memnite").single()
            game.execute(action.copy(targets = listOf(ChosenTarget.Permanent(bears)))).error shouldBe null
            game.resolveStack()

            game.plusOnes(bears) shouldBe 1
        }

        test("an {X} in the scavenged card's mana cost is 0") {
            // Endless One costs {X}: scavenged with no mana at all, and no X to announce.
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Young Deathclaws")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardInGraveyard(1, "Endless One")
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(2, "Forest")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val bears = game.findPermanent("Grizzly Bears")!!

            val action = game.scavengeActions("Endless One").single()
            game.execute(action.copy(targets = listOf(ChosenTarget.Permanent(bears)))).error shouldBe null
            game.hasPendingDecision() shouldBe false
            game.resolveStack()

            game.isInExile(1, "Endless One") shouldBe true
            game.plusOnes(bears) shouldBe 0
        }
    }
}
