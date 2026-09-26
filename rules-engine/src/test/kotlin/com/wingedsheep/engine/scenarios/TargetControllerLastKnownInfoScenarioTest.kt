package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.battlefield.LastKnownPermanentComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CreatePredefinedTokenEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * [EffectTarget.TargetController] must honor control-changing effects, live and as last-known
 * information:
 *
 *  - Control changes are Layer-2 continuous effects — they never touch the base
 *    ControllerComponent — so a live read must go through the projected controller.
 *  - CR 608.2h: an effect that needs information from an object it has itself moved out of the
 *    expected zone uses the object's last known information. "Destroy target creature. Its
 *    controller creates two Map tokens." (Get Lost) must credit the controller the permanent had
 *    when it left the battlefield, not the card's owner — the two differ exactly when a
 *    control-change effect (Threaten, Empress Galina) was active.
 *
 * The last-known snapshot rides on [LastKnownPermanentComponent], attached by
 * ZoneTransitionService on every battlefield exit and stripped again on the entity's next zone
 * change (CR 400.7 — a later move makes a new object).
 */
class TargetControllerLastKnownInfoScenarioTest : ScenarioTestBase() {

    /** "Gain control of target creature." — a zero-cost Threaten without the untap/haste riders. */
    private val testSteal = card("Test Steal") {
        manaCost = "{0}"
        typeLine = "Sorcery"
        oracleText = "Gain control of target creature."
        spell {
            val t = target(TargetFilter.Creature)
            effect = Effects.GainControl(t)
        }
    }

    /** "Target creature's controller creates two Map tokens." — TargetController with the target alive. */
    private val testTribute = card("Test Tribute") {
        manaCost = "{0}"
        typeLine = "Sorcery"
        oracleText = "Target creature's controller creates two Map tokens."
        spell {
            target(TargetFilter.Creature)
            effect = CreatePredefinedTokenEffect("Map", 2, EffectTarget.TargetController)
        }
    }

    /** "Return target creature card from a graveyard to the battlefield." — forces a next zone change. */
    private val testRecall = card("Test Recall") {
        manaCost = "{0}"
        typeLine = "Instant"
        oracleText = "Return target creature card from a graveyard to the battlefield."
        spell {
            val t = target(TargetFilter(GameObjectFilter.Creature, zone = Zone.GRAVEYARD))
            effect = Effects.PutOntoBattlefield(t)
        }
    }

    private fun countMaps(game: TestGame, playerNumber: Int): Int {
        val playerId = if (playerNumber == 1) game.player1Id else game.player2Id
        return game.state.getBattlefield(playerId).count { id ->
            game.state.getEntity(id)?.get<CardComponent>()?.name == "Map"
        }
    }

    init {
        cardRegistry.register(testSteal)
        cardRegistry.register(testTribute)
        cardRegistry.register(testRecall)

        context("TargetController with control-changing effects") {

            test("destroying a stolen creature credits its controller-at-death, not its owner") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardInHand(1, "Test Steal")
                    .withCardInHand(1, "Get Lost")
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!

                game.castSpell(1, "Test Steal", targetId = bears).error shouldBe null
                game.resolveStack()
                withClue("Alice controls the stolen Bears (Layer-2 projection)") {
                    game.state.projectedState.getController(bears) shouldBe game.player1Id
                }

                game.castSpell(1, "Get Lost", targetId = bears).error shouldBe null
                game.resolveStack()

                withClue("Grizzly Bears should be destroyed") {
                    game.findPermanent("Grizzly Bears") shouldBe null
                }
                withClue("controller-at-death (Alice, the thief) creates the Map tokens (CR 608.2h)") {
                    countMaps(game, 1) shouldBe 2
                }
                withClue("the owner (Bob) does not create the tokens") {
                    countMaps(game, 2) shouldBe 0
                }
                withClue("the graveyard entity carries the battlefield-exit snapshot") {
                    game.state.getEntity(bears)?.has<LastKnownPermanentComponent>() shouldBe true
                }
            }

            test("a live stolen creature's TargetController reads the projected controller") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardInHand(1, "Test Steal")
                    .withCardInHand(1, "Test Tribute")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!

                game.castSpell(1, "Test Steal", targetId = bears).error shouldBe null
                game.resolveStack()

                game.castSpell(1, "Test Tribute", targetId = bears).error shouldBe null
                game.resolveStack()

                withClue("Bears survives the tribute") {
                    game.findPermanent("Grizzly Bears") shouldBe bears
                }
                withClue("the projected controller (Alice, the thief) creates the tokens") {
                    countMaps(game, 1) shouldBe 2
                }
                withClue("the owner (Bob) does not create the tokens") {
                    countMaps(game, 2) shouldBe 0
                }
            }

            test("the last-known snapshot is stripped on the entity's next zone change (CR 400.7)") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardInHand(1, "Test Steal")
                    .withCardInHand(1, "Get Lost")
                    .withCardInHand(1, "Test Recall")
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                game.castSpell(1, "Test Steal", targetId = bears).error shouldBe null
                game.resolveStack()
                game.castSpell(1, "Get Lost", targetId = bears).error shouldBe null
                game.resolveStack()
                game.state.getEntity(bears)?.has<LastKnownPermanentComponent>() shouldBe true

                // Return the card to the battlefield: a new object (CR 400.7) must not carry the
                // old incarnation's last-known information.
                val recall = game.state.getHand(game.player1Id).first { id ->
                    game.state.getEntity(id)?.get<CardComponent>()?.name == "Test Recall"
                }
                game.execute(
                    CastSpell(
                        playerId = game.player1Id,
                        cardId = recall,
                        targets = listOf(ChosenTarget.Card(bears, game.player2Id, Zone.GRAVEYARD))
                    )
                ).error shouldBe null
                game.resolveStack()

                withClue("Bears is back on the battlefield") {
                    game.findPermanent("Grizzly Bears") shouldBe bears
                }
                withClue("the battlefield-exit snapshot did not survive the zone change") {
                    game.state.getEntity(bears)?.has<LastKnownPermanentComponent>() shouldBe false
                }
            }
        }
    }
}
