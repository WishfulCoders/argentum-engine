package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CardsDiscardedEvent
import com.wingedsheep.engine.core.GameEvent
import com.wingedsheep.engine.core.PermanentsSacrificedEvent
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.lea.cards.Balance
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Balance (LEA #3) — "Each player chooses a number of lands they control equal to the number of
 * lands controlled by the player who controls the fewest, then sacrifices the rest. Players
 * discard cards and sacrifice creatures the same way."
 *
 * Proves the three parts each equalize to the table minimum ([DynamicAmount.LeastAmongPlayers]),
 * that each part recounts after the previous one (the 2016-06-08 land-creature ruling), that the
 * player with the fewest is never asked to choose, and that every player chooses — in APNAP
 * order — before anything is sacrificed, with one sacrifice event per player.
 */
class BalanceScenarioTest : ScenarioTestBase() {

    private fun TestGame.battlefieldOf(playerId: EntityId): List<EntityId> =
        state.getBattlefield().filter { state.projectedState.getController(it) == playerId }

    private fun TestGame.namesOn(playerId: EntityId, name: String): List<EntityId> =
        battlefieldOf(playerId).filter { cardName(it) == name }

    private fun TestGame.cardName(id: EntityId): String? =
        state.getEntity(id)?.get<com.wingedsheep.engine.state.components.identity.CardComponent>()?.name

    /** Cast Balance from player 1 and pass until it resolves or pauses; returns the events seen. */
    private fun TestGame.castBalance(): MutableList<GameEvent> {
        val events = mutableListOf<GameEvent>()
        val cast = castSpell(1, "Balance")
        withClue("cast: ${cast.error}") { cast.error shouldBe null }
        events += cast.events
        resolveStack().forEach { events += it.events }
        return events
    }

    init {
        context("Balance") {

            test("lands: the player with more keeps the fewest count; the player with fewest is not asked") {
                val game = scenario()
                    .withPlayers()
                    .withLandsOnBattlefield(1, "Plains", 5)
                    .withLandsOnBattlefield(2, "Forest", 2)
                    .withCardInHand(1, "Balance")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castBalance()

                val decision = game.getPendingDecision()
                decision.shouldBeInstanceOf<SelectCardsDecision>()
                decision.playerId shouldBe game.player1Id
                decision.minSelections shouldBe 2
                decision.maxSelections shouldBe 2
                decision.useTargetingUI shouldBe true

                val kept = decision.options.take(2)
                game.selectCards(kept)

                withClue("no further choices: hands are 0 vs 0 and nobody has creatures") {
                    game.hasPendingDecision() shouldBe false
                }
                game.battlefieldOf(game.player1Id) shouldContainExactlyInAnyOrder kept
                game.namesOn(game.player2Id, "Forest").size shouldBe 2
                game.findCardsInGraveyard(1, "Plains").size shouldBe 3
            }

            test("hands: counted after lands, the bigger hand discards down into its owner's graveyard") {
                val game = scenario()
                    .withPlayers()
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withLandsOnBattlefield(2, "Plains", 2)
                    .withCardInHand(1, "Balance")
                    .withCardInHand(1, "Grizzly Bears")
                    .withCardsInHand(2, "Grizzly Bears", 2)
                    .withCardsInHand(2, "Forest", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castBalance()

                val decision = game.getPendingDecision()
                decision.shouldBeInstanceOf<SelectCardsDecision>()
                withClue("the caster's one remaining card is the fewest; only player 2 chooses") {
                    decision.playerId shouldBe game.player2Id
                }
                decision.minSelections shouldBe 1
                val keep = decision.options.first()
                val result = game.selectCards(listOf(keep))

                game.state.getHand(game.player2Id) shouldBe listOf(keep)
                game.handSize(1) shouldBe 1
                game.graveyardSize(2) shouldBe 3
                val discards = result.events.filterIsInstance<CardsDiscardedEvent>()
                withClue("the discard is attributed to its owner, not to Balance's caster") {
                    discards.map { it.playerId } shouldBe listOf(game.player2Id)
                    discards.single().cardIds.size shouldBe 3
                }
            }

            test("creatures: the player with more sacrifices down to the fewest") {
                val game = scenario()
                    .withPlayers()
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withLandsOnBattlefield(2, "Forest", 2)
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardInHand(1, "Balance")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castBalance()

                val decision = game.getPendingDecision()
                decision.shouldBeInstanceOf<SelectCardsDecision>()
                decision.playerId shouldBe game.player2Id
                decision.minSelections shouldBe 1
                val keep = decision.options.first()
                val result = game.selectCards(listOf(keep))

                game.namesOn(game.player2Id, "Grizzly Bears") shouldBe listOf(keep)
                game.namesOn(game.player1Id, "Grizzly Bears").size shouldBe 1
                game.findCardsInGraveyard(2, "Grizzly Bears").size shouldBe 2
                val sacrifices = result.events.filterIsInstance<PermanentsSacrificedEvent>()
                withClue("a permanent is sacrificed by its controller, not by Balance's caster") {
                    sacrifices.map { it.playerId } shouldBe listOf(game.player2Id)
                }
            }

            test("each part recounts: a land creature sacrificed with the lands isn't counted as a creature") {
                // Player 1: two Plains + Dryad Arbor (a land creature) + one Grizzly Bears.
                // Player 2: two Forests + three Grizzly Bears.
                // Lands: 3 vs 2 → player 1 keeps the Plains and sacrifices Dryad Arbor.
                // Creatures, counted now: 1 vs 3 → player 2 keeps one. Counted before the land
                // part it would have been 2 vs 3, and player 2 would wrongly keep two.
                val game = scenario()
                    .withPlayers()
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withCardOnBattlefield(1, "Dryad Arbor")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withLandsOnBattlefield(2, "Forest", 2)
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardInHand(1, "Balance")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castBalance()

                val landChoice = game.getPendingDecision()
                landChoice.shouldBeInstanceOf<SelectCardsDecision>()
                landChoice.playerId shouldBe game.player1Id
                landChoice.minSelections shouldBe 2
                val plains = landChoice.options.filter { game.cardName(it) == "Plains" }
                game.selectCards(plains)
                game.isInGraveyard(1, "Dryad Arbor") shouldBe true

                val creatureChoice = game.getPendingDecision()
                creatureChoice.shouldBeInstanceOf<SelectCardsDecision>()
                creatureChoice.playerId shouldBe game.player2Id
                withClue("player 1 has one creature left once Dryad Arbor is gone") {
                    creatureChoice.minSelections shouldBe 1
                }
                game.selectCards(listOf(creatureChoice.options.first()))

                game.namesOn(game.player2Id, "Grizzly Bears").size shouldBe 1
                game.namesOn(game.player1Id, "Grizzly Bears").size shouldBe 1
                game.namesOn(game.player1Id, "Plains").size shouldBe 2
            }

            test("three players choose in APNAP order, then all unchosen lands are sacrificed at once") {
                val driver = GameTestDriver()
                driver.registerCards(TestCards.all + Balance)
                val players = driver.initMultiplayer(
                    decks = List(3) { Deck.of("Plains" to 40) },
                    startingPlayer = 0,
                )
                driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
                val (p1, p2, p3) = players
                driver.activePlayer shouldBe p1
                // Empty every hand so the discard part asks nobody anything.
                players.forEach { p -> driver.getHand(p).forEach { driver.moveToGraveyard(it) } }

                val p1Lands = List(4) { driver.putLandOnBattlefield(p1, "Plains") }
                val p2Lands = List(3) { driver.putLandOnBattlefield(p2, "Plains") }
                val p3Land = driver.putLandOnBattlefield(p3, "Plains")

                val balance = driver.putCardInHand(p1, "Balance")
                driver.giveColorlessMana(p1, 1)
                driver.giveMana(p1, Color.WHITE, 1)
                driver.castSpell(p1, balance).error shouldBe null
                var guard = 0
                while (driver.stackSize > 0 && !driver.isPaused && guard++ < 10) {
                    driver.passPriority(driver.state.priorityPlayerId!!)
                }

                val first = driver.pendingDecision
                first.shouldBeInstanceOf<SelectCardsDecision>()
                withClue("the active player chooses first") { first.playerId shouldBe p1 }
                first.minSelections shouldBe 1
                driver.submitCardSelection(p1, listOf(p1Lands[0]))

                withClue("nothing is sacrificed until every player has chosen") {
                    p1Lands.all { it in driver.state.getBattlefield() } shouldBe true
                }

                val second = driver.pendingDecision
                second.shouldBeInstanceOf<SelectCardsDecision>()
                second.playerId shouldBe p2
                val eventsBefore = driver.events.size
                driver.submitCardSelection(p2, listOf(p2Lands[2]))

                driver.isPaused shouldBe false
                val battlefield = driver.state.getBattlefield()
                (p1Lands.drop(1) + p2Lands.take(2)).none { it in battlefield } shouldBe true
                (listOf(p1Lands[0], p2Lands[2], p3Land)).all { it in battlefield } shouldBe true

                val sacrifices = driver.events.drop(eventsBefore).filterIsInstance<PermanentsSacrificedEvent>()
                withClue("one simultaneous sacrifice, reported once per sacrificing player") {
                    sacrifices.map { it.playerId } shouldContainExactlyInAnyOrder listOf(p1, p2)
                    sacrifices.single { it.playerId == p1 }.permanentIds shouldContainExactlyInAnyOrder p1Lands.drop(1)
                    sacrifices.single { it.playerId == p2 }.permanentIds shouldContainExactlyInAnyOrder p2Lands.take(2)
                }
            }
        }
    }
}
