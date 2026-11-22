package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AlternativePaymentChoice
import com.wingedsheep.sdk.scripting.ConvokePayment
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Lethal Scheme (NCC #36) — {2}{B}{B} Instant, convoke. "Destroy target creature or planeswalker.
 * Each creature that convoked this spell connives."
 *
 * The convokers are read off the *spell's* convoke record while it resolves on the stack. A
 * convoker still on the battlefield connives as itself; one that has left — here, destroyed by the
 * spell — still connives (2022-04-29 ruling, CR 701.50b): its controller draws and discards, no
 * counter, and a connive replacement sees it as the creature it last was.
 */
class LethalSchemeScenarioTest : ScenarioTestBase() {

    private fun plusCounters(game: TestGame, id: EntityId): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    private fun discard(game: TestGame, cardId: EntityId) {
        game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
        game.selectCards(listOf(cardId)).error shouldBe null
    }

    private fun board() = scenario()
        .withPlayers("Player1", "Player2")
        .withCardInHand(1, "Lethal Scheme")
        .withCardInHand(1, "Hill Giant")
        .withCardInHand(1, "Hill Giant")
        .withLandsOnBattlefield(1, "Swamp", 2)
        .withCardOnBattlefield(1, "Grizzly Bears")
        .withCardOnBattlefield(1, "Grizzly Bears")
        .withCardOnBattlefield(1, "Glory Seeker")
        .withCardOnBattlefield(2, "Hill Giant")
        .withCardInLibrary(1, "Swamp")
        .withCardInLibrary(1, "Swamp")
        .withCardInLibrary(1, "Swamp")
        .withCardInLibrary(1, "Swamp")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        context("Lethal Scheme") {

            test("destroys the target and each convoking creature connives; a bystander does not") {
                val game = board()
                val (first, second) = game.findPermanents("Grizzly Bears")
                val bystander = game.findPermanent("Glory Seeker")!!
                val victim = game.findPermanents("Hill Giant").single()

                val cast = game.execute(
                    CastSpell(
                        playerId = game.player1Id,
                        cardId = game.findCardsInHand(1, "Lethal Scheme").single(),
                        targets = listOf(ChosenTarget.Permanent(victim)),
                        alternativePayment = AlternativePaymentChoice(
                            convokedCreatures = mapOf(
                                first to ConvokePayment(color = null),
                                second to ConvokePayment(color = null)
                            )
                        )
                    )
                )
                withClue("two bears pay {2}, two Swamps pay {B}{B}: ${cast.error}") { cast.error shouldBe null }
                game.resolveStack()

                withClue("the target is destroyed") {
                    game.isInGraveyard(2, "Hill Giant") shouldBe true
                }

                // Two connives, one per convoker: discard a nonland (Hill Giant) both times.
                val giants = game.findCardsInHand(1, "Hill Giant")
                discard(game, giants[0])
                discard(game, giants[1])
                game.hasPendingDecision() shouldBe false

                withClue("each convoker discarded a nonland, so each gets a +1/+1 counter") {
                    plusCounters(game, first) shouldBe 1
                    plusCounters(game, second) shouldBe 1
                }
                withClue("a creature that didn't convoke the spell doesn't connive") {
                    plusCounters(game, bystander) shouldBe 0
                }
                withClue("two cards drawn, two discarded: the hand is two Swamps") {
                    game.handSize(1) shouldBe 2
                    game.findCardsInHand(1, "Swamp").size shouldBe 2
                }
            }

            test("a convoker destroyed by the spell still connives: draw and discard, no counter") {
                val game = board()
                val (first, second) = game.findPermanents("Grizzly Bears")

                val cast = game.execute(
                    CastSpell(
                        playerId = game.player1Id,
                        cardId = game.findCardsInHand(1, "Lethal Scheme").single(),
                        targets = listOf(ChosenTarget.Permanent(first)),
                        alternativePayment = AlternativePaymentChoice(
                            convokedCreatures = mapOf(
                                first to ConvokePayment(color = null),
                                second to ConvokePayment(color = null)
                            )
                        )
                    )
                )
                cast.error shouldBe null
                game.resolveStack()

                withClue("the convoking bear that was targeted is destroyed") {
                    game.findPermanents("Grizzly Bears") shouldBe listOf(second)
                }

                val giants = game.findCardsInHand(1, "Hill Giant")
                discard(game, giants[0])
                discard(game, giants[1])
                game.hasPendingDecision() shouldBe false

                withClue("the surviving convoker grows; the destroyed one's connive still looted") {
                    plusCounters(game, second) shouldBe 1
                    game.handSize(1) shouldBe 2
                    game.findCardsInGraveyard(1, "Hill Giant").size shouldBe 2
                }
            }

            test("a destroyed convoker connives by last-known information, so Leader's replacement applies to it") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Lethal Scheme")
                    .withCardInHand(1, "Hill Giant")
                    .withCardInHand(1, "Hill Giant")
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(1, "Glory Seeker")
                    .withCardOnBattlefield(1, "Leader, Super-Genius")
                    .withCardInLibrary(1, "Swamp")
                    .withCardInLibrary(1, "Swamp")
                    .withCardInLibrary(1, "Swamp")
                    .withCardInLibrary(1, "Swamp")
                    .withCardInLibrary(1, "Swamp")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val bear = game.findPermanent("Grizzly Bears")!!
                val seeker = game.findPermanent("Glory Seeker")!!

                val cast = game.execute(
                    CastSpell(
                        playerId = game.player1Id,
                        cardId = game.findCardsInHand(1, "Lethal Scheme").single(),
                        targets = listOf(ChosenTarget.Permanent(bear)),
                        alternativePayment = AlternativePaymentChoice(
                            convokedCreatures = mapOf(
                                bear to ConvokePayment(color = null),
                                seeker to ConvokePayment(color = null)
                            )
                        )
                    )
                )
                cast.error shouldBe null
                game.resolveStack()

                withClue("the targeted convoker is destroyed") {
                    game.isInGraveyard(1, "Grizzly Bears") shouldBe true
                }

                val giants = game.findCardsInHand(1, "Hill Giant")
                discard(game, giants[0])
                discard(game, giants[1])
                game.hasPendingDecision() shouldBe false

                withClue("Leader replaced both connives — the destroyed bear's too — so four cards were drawn") {
                    game.handSize(1) shouldBe 4
                    game.findCardsInHand(1, "Swamp").size shouldBe 4
                }
                withClue("the live convoker grows; the destroyed one's counter has nowhere to go") {
                    plusCounters(game, seeker) shouldBe 1
                    plusCounters(game, bear) shouldBe 0
                }
            }
        }
    }
}
