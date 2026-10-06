package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.player.LandDropsComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Gix, Yawgmoth Praetor (BRO #95) — {1}{B}{B} Legendary Creature — Phyrexian Praetor 3/3.
 *
 *   Whenever a creature deals combat damage to one of your opponents, its controller may pay 1
 *   life. If they do, they draw a card.
 *   {4}{B}{B}{B}, Discard X cards: Exile the top X cards of target opponent's library. You may
 *   play lands and cast spells from among cards exiled this way without paying their mana costs.
 *
 * Pins the two primitives the card introduced: the `DiscardX` activation cost (X *is* how many
 * cards you discard — one selection, no number picker) and the `playLands` axis of the
 * resolution-time free-cast loop (a land is *played*, using up your land play, and is offered only
 * while you could play one).
 */
class GixYawgmothPraetorScenarioTest : ScenarioTestBase() {

    private val gixAbilityId
        get() = cardRegistry.getCard("Gix, Yawgmoth Praetor")!!.script.activatedAbilities[0].id

    private fun TestGame.nameOf(id: EntityId): String? = state.getEntity(id)?.get<CardComponent>()?.name

    private fun TestGame.optionNames(decision: SelectCardsDecision): Set<String> =
        decision.options.mapNotNull { nameOf(it) }.toSet()

    private fun TestGame.exileNames(playerNumber: Int): List<String> {
        val playerId = if (playerNumber == 1) player1Id else player2Id
        return state.getExile(playerId).mapNotNull { nameOf(it) }
    }

    private fun TestGame.activateGix(): SelectCardsDecision {
        val result = execute(
            ActivateAbility(
                playerId = player1Id,
                sourceId = findPermanent("Gix, Yawgmoth Praetor")!!,
                abilityId = gixAbilityId,
                targets = listOf(ChosenTarget.Player(player2Id))
            )
        )
        withClue("activation should pause for the discard selection: ${result.error}") { result.error shouldBe null }
        return getPendingDecision() as SelectCardsDecision
    }

    private fun gixBoard(landsPlayedAlready: Boolean = false) = scenario()
        .withPlayers("Player", "Opponent")
        .withCardOnBattlefield(1, "Gix, Yawgmoth Praetor")
        .withLandsOnBattlefield(1, "Swamp", 7)
        .withCardInHand(1, "Hill Giant")
        .withCardInHand(1, "Hill Giant")
        .withCardInHand(1, "Shock")
        // Top of the opponent's library, in order: a land, a spell, then a card that stays put.
        .withCardInLibrary(2, "Forest")
        .withCardInLibrary(2, "Grizzly Bears")
        .withCardInLibrary(2, "Centaur Courser")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()
        .also { game ->
            if (landsPlayedAlready) {
                game.state = game.state.updateEntity(game.player1Id) {
                    it.with(LandDropsComponent(remaining = 0, maxPerTurn = 1))
                }
            }
        }

    init {
        context("Gix's activated ability") {

            test("X is the number of cards discarded; a land is played and a spell cast for free during resolution") {
                val game = gixBoard()

                val discard = game.activateGix()
                withClue("every card in hand may be discarded, and discarding none is legal (X = 0)") {
                    discard.options.size shouldBe 3
                    discard.minSelections shouldBe 0
                    discard.maxSelections shouldBe 3
                }
                val giants = game.findCardsInHand(1, "Hill Giant")
                game.selectCards(giants).error shouldBe null

                withClue("the two discarded cards are in the graveyard; Shock stays in hand") {
                    game.findCardsInGraveyard(1, "Hill Giant").size shouldBe 2
                    game.isInHand(1, "Shock") shouldBe true
                }
                game.resolveStack()

                withClue("X = 2: exactly the top two cards of the opponent's library are exiled") {
                    game.exileNames(2).toSet() shouldBe setOf("Forest", "Grizzly Bears")
                    game.findCardsInLibrary(2, "Centaur Courser").size shouldBe 1
                }

                val first = game.getPendingDecision() as SelectCardsDecision
                withClue("on your own turn with a land play left, the land is offered alongside the spell") {
                    game.optionNames(first) shouldBe setOf("Forest", "Grizzly Bears")
                }
                val forest = first.options.single { game.nameOf(it) == "Forest" }
                game.selectCards(listOf(forest)).error shouldBe null

                withClue("the Forest is played as your land for the turn") {
                    game.state.getZone(game.player1Id, Zone.BATTLEFIELD).contains(forest) shouldBe true
                    game.state.getEntity(game.player1Id)!!.get<LandDropsComponent>()!!.remaining shouldBe 0
                }

                val second = game.getPendingDecision() as SelectCardsDecision
                withClue("only the spell is left to choose") {
                    game.optionNames(second) shouldBe setOf("Grizzly Bears")
                }
                game.selectCards(second.options).error shouldBe null
                game.resolveStack()

                withClue("the free Grizzly Bears resolves under your control") {
                    game.state.getZone(game.player1Id, Zone.BATTLEFIELD)
                        .any { game.nameOf(it) == "Grizzly Bears" } shouldBe true
                    game.exileNames(2) shouldBe emptyList()
                }
            }

            test("with the land play already used, the exiled land isn't offered and stays in exile") {
                val game = gixBoard(landsPlayedAlready = true)

                game.activateGix()
                game.selectCards(game.findCardsInHand(1, "Hill Giant")).error shouldBe null
                game.resolveStack()

                val offer = game.getPendingDecision() as SelectCardsDecision
                withClue("no land play left (ruling), so only the spell is offered") {
                    game.optionNames(offer) shouldBe setOf("Grizzly Bears")
                }
                game.selectCards(emptyList()).error shouldBe null

                withClue("declining leaves both cards in exile — they can't be played later") {
                    game.exileNames(2).toSet() shouldBe setOf("Forest", "Grizzly Bears")
                }
            }

            test("discarding nothing settles as X = 0 and exiles nothing") {
                val game = gixBoard()

                game.activateGix()
                game.selectCards(emptyList()).error shouldBe null
                game.resolveStack()

                withClue("no discard, no exile, no prompt") {
                    game.handSize(1) shouldBe 3
                    game.exileNames(2) shouldBe emptyList()
                    game.librarySize(2) shouldBe 3
                    game.getPendingDecision() shouldBe null
                }
            }
        }

        context("Gix's combat-damage trigger") {

            fun attackWithBears(): TestGame {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Gix, Yawgmoth Praetor")
                    .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                    .withCardInLibrary(1, "Swamp")
                    .withCardInLibrary(2, "Swamp")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareNoBlockers().error shouldBe null
                // Pass through combat damage until the trigger resolves into its pay prompt.
                repeat(20) {
                    if (game.getPendingDecision() != null) return game
                    game.passPriority()
                }
                return game
            }

            test("paying 1 life draws a card") {
                val game = attackWithBears()
                withClue("the Bears' controller is asked whether to pay") {
                    (game.getPendingDecision() is YesNoDecision) shouldBe true
                    game.getPendingDecision()!!.playerId shouldBe game.player1Id
                }
                val handBefore = game.handSize(1)
                game.answerYesNo(true).error shouldBe null
                game.resolveStack()

                game.getLifeTotal(1) shouldBe 19
                game.handSize(1) shouldBe handBefore + 1
                game.getLifeTotal(2) shouldBe 18
            }

            test("declining pays nothing and draws nothing") {
                val game = attackWithBears()
                val handBefore = game.handSize(1)
                game.answerYesNo(false).error shouldBe null
                game.resolveStack()

                game.getLifeTotal(1) shouldBe 20
                game.handSize(1) shouldBe handBefore
            }
        }
    }
}
