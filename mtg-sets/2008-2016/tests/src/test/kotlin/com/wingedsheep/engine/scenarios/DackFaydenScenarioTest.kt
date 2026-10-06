package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.cns.cards.DackFayden
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Dack Fayden (CNS #42; {1}{U}{R}, Loyalty 3).
 *
 *   +1: Target player draws two cards, then discards two cards.
 *   −2: Gain control of target artifact.
 *   −6: You get an emblem with "Whenever you cast a spell that targets one or more permanents, gain
 *       control of those permanents."
 */
class DackFaydenScenarioTest : ScenarioTestBase() {

    private val plusOne = DackFayden.activatedAbilities[0].id
    private val minusTwo = DackFayden.activatedAbilities[1].id
    private val minusSix = DackFayden.activatedAbilities[2].id

    init {
        fun seedLoyalty(game: TestGame, id: EntityId, amount: Int) {
            game.state = game.state.updateEntity(id) { c ->
                c.with(CountersComponent().withAdded(CounterType.LOYALTY, amount))
            }
        }

        fun loyalty(game: TestGame, id: EntityId): Int =
            game.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.LOYALTY) ?: 0

        test("+1: the target player draws two cards, then discards two") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Dack Fayden")
                .withCardInHand(1, "Grizzly Bears")
                .withCardInLibrary(1, "Centaur Courser")
                .withCardInLibrary(1, "Llanowar Elves")
                .withCardInLibrary(1, "Savannah Lions")
                .withCardInLibrary(2, "Island")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val dack = game.findPermanent("Dack Fayden")!!
            seedLoyalty(game, dack, 3)

            game.execute(
                ActivateAbility(
                    playerId = game.player1Id, sourceId = dack, abilityId = plusOne,
                    targets = listOf(ChosenTarget.Player(game.player1Id)),
                )
            ).error shouldBe null
            game.resolveStack()

            val discard = game.getPendingDecision() as SelectCardsDecision
            withClue("three cards in hand to choose two discards from") { discard.options.size shouldBe 3 }
            val chosen = discard.options.take(2)
            game.selectCards(chosen).error shouldBe null

            game.handSize(1) shouldBe 1
            game.state.getGraveyard(game.player1Id).containsAll(chosen) shouldBe true
            loyalty(game, dack) shouldBe 4
        }

        test("−2: gain control of target artifact, which stays after Dack leaves") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Dack Fayden")
                .withCardOnBattlefield(2, "Ornithopter")
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(2, "Island")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val dack = game.findPermanent("Dack Fayden")!!
            seedLoyalty(game, dack, 2)
            val thopter = game.findPermanent("Ornithopter")!!

            game.execute(
                ActivateAbility(
                    playerId = game.player1Id, sourceId = dack, abilityId = minusTwo,
                    targets = listOf(ChosenTarget.Permanent(thopter)),
                )
            ).error shouldBe null
            game.resolveStack()

            withClue("Dack went to 0 loyalty and left the battlefield") {
                game.findPermanent("Dack Fayden") shouldBe null
            }
            game.state.projectedState.getController(thopter) shouldBe game.player1Id
        }

        test("−6: the emblem steals the permanents a later spell targets, before the spell resolves") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Dack Fayden")
                .withCardInHand(1, "Lightning Bolt")
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withCardOnBattlefield(2, "Force of Nature")
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(2, "Island")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val dack = game.findPermanent("Dack Fayden")!!
            seedLoyalty(game, dack, 6)

            game.execute(
                ActivateAbility(playerId = game.player1Id, sourceId = dack, abilityId = minusSix)
            ).error shouldBe null
            game.resolveStack()

            val force = game.findPermanent("Force of Nature")!!
            game.castSpell(1, "Lightning Bolt", force).error shouldBe null
            withClue("the emblem's trigger sits above the Bolt") { game.state.stack.size shouldBe 2 }
            game.resolveStack()

            game.state.projectedState.getController(force) shouldBe game.player1Id
        }

        test("−6: a spell that targets only a player doesn't trigger the emblem") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Dack Fayden")
                .withCardInHand(1, "Lightning Bolt")
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withCardOnBattlefield(2, "Force of Nature")
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(2, "Island")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val dack = game.findPermanent("Dack Fayden")!!
            seedLoyalty(game, dack, 6)
            game.execute(
                ActivateAbility(playerId = game.player1Id, sourceId = dack, abilityId = minusSix)
            ).error shouldBe null
            game.resolveStack()

            game.castSpellTargetingPlayer(1, "Lightning Bolt", 2).error shouldBe null
            game.state.stack.size shouldBe 1
            game.resolveStack()
            game.getLifeTotal(2) shouldBe 17
            game.state.projectedState.getController(game.findPermanent("Force of Nature")!!) shouldBe game.player2Id
        }
    }
}
