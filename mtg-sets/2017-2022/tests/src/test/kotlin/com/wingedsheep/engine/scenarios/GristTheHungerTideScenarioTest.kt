package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.mh2.cards.GristTheHungerTide
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.AbilityCost
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Grist, the Hunger Tide (MH2 #202).
 *
 * Pins the +1's "repeat while an Insect card was milled" loop — including Grist itself counting as an
 * Insect card off the battlefield (CR 113.6c) — the −5 counting Grist in the graveyard as a creature
 * card, the optional reflexive −2, and that Grist on the battlefield is not a creature.
 */
class GristTheHungerTideScenarioTest : ScenarioTestBase() {
    init {
        fun abilityId(change: Int) = GristTheHungerTide.activatedAbilities
            .single { (it.cost as? AbilityCost.Loyalty)?.change == change }.id

        fun TestGame.grist() = findPermanent("Grist, the Hunger Tide")!!
        fun TestGame.loyalty() = state.getEntity(grist())!!.get<CountersComponent>()!!.getCount(CounterType.LOYALTY)
        fun TestGame.activate(change: Int) = execute(ActivateAbility(player1Id, grist(), abilityId(change)))

        fun board(vararg library: String) = scenario()
            .withPlayers("Grist", "Opponent")
            .withCardOnBattlefield(1, "Grist, the Hunger Tide")
            .apply { library.forEach { withCardInLibrary(1, it) } }
            .withCardInLibrary(2, "Forest")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)

        test("on the battlefield Grist is a planeswalker, not a creature") {
            val game = board("Forest").build()
            game.state.projectedState.isCreature(game.grist()) shouldBe false
            game.state.projectedState.isPlaneswalker(game.grist()) shouldBe true
        }

        test("+1 repeats while the milled card is an Insect card") {
            val game = board("Carrion Ants", "Killer Bees", "Forest", "Forest").build()
            game.activate(+1).error shouldBe null
            game.resolveStack()
            withClue("three passes: Carrion Ants and Killer Bees were Insects, the Forest stopped it") {
                game.findPermanents("Insect Token").size shouldBe 3
                game.isInGraveyard(1, "Carrion Ants") shouldBe true
                game.isInGraveyard(1, "Killer Bees") shouldBe true
                game.graveyardSize(1) shouldBe 3
                game.librarySize(1) shouldBe 1
            }
            withClue("3 + 1 (cost) + one counter per Insect milled") { game.loyalty() shouldBe 6 }
        }

        test("+1: a milled Grist card is an Insect card, so it keeps the process going") {
            val game = board("Grist, the Hunger Tide", "Forest").build()
            game.activate(+1).error shouldBe null
            game.resolveStack()
            game.findPermanents("Insect Token").size shouldBe 2
            game.loyalty() shouldBe 5
            val milledGrist = game.findCardsInGraveyard(1, "Grist, the Hunger Tide").single()
            game.state.getEntity(milledGrist)!!.get<CardComponent>()!!.typeLine.isCreature shouldBe true
        }

        test("−5: each opponent loses life equal to the creature cards in your graveyard, Grist included") {
            val game = scenario()
                .withPlayers("Grist", "Opponent")
                .withCardOnBattlefield(1, "Grist, the Hunger Tide")
                .withCardInGraveyard(1, "Grizzly Bears")
                .withCardInGraveyard(1, "Hill Giant")
                .withCardInGraveyard(1, "Grist, the Hunger Tide")
                .withCardInGraveyard(1, "Forest")
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(2, "Forest")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            game.state = game.state.updateEntity(game.grist()) {
                it.with(CountersComponent(mapOf(CounterType.LOYALTY to 6)))
            }
            game.activate(-5).error shouldBe null
            game.resolveStack()
            game.getLifeTotal(2) shouldBe 17
        }

        test("−2: sacrificing a creature destroys target creature or planeswalker; declining does nothing") {
            val game = board("Forest")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(2, "Hill Giant")
                .build()
            game.activate(-2).error shouldBe null
            game.resolveStack()
            game.answerYesNo(true).error shouldBe null
            withClue("the only creature was sacrificed automatically") { game.isInGraveyard(1, "Grizzly Bears") shouldBe true }
            game.selectTargets(listOf(game.findPermanent("Hill Giant")!!)).error shouldBe null
            game.resolveStack()
            game.isInGraveyard(2, "Hill Giant") shouldBe true

            val declined = board("Forest")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(2, "Hill Giant")
                .build()
            declined.activate(-2).error shouldBe null
            declined.resolveStack()
            declined.answerYesNo(false).error shouldBe null
            declined.resolveStack()
            declined.isOnBattlefield("Grizzly Bears") shouldBe true
            declined.isOnBattlefield("Hill Giant") shouldBe true
        }
    }
}
