package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PassPriority
import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Regression test for the target-index-collapse class of bug:
 *     "If you kill one of the targets, the surviving target gets picked for both effects."
 *
 * Card reference — Friendly Rivalry ({R}{G}, Instant, LTR #204):
 *   "Target creature you control [0] and up to one other target legendary creature you control [1]
 *    each deal damage equal to their power to target creature you don't control [2]."
 *
 * Friendly Rivalry has THREE targets and two damage instructions whose *amount* is a positional
 * `EffectTarget.ContextTarget(n)` power read — Target(0) for the first creature, Target(1) for the
 * legendary. CR 608.2b drops illegal targets at resolution and the engine compacts
 * `context.targets`. Before the fix, the second instruction's `Target(1)` indexed the COMPACTED
 * list: once the first target (creature you control) died in response, the legendary slid into
 * slot 0 and the opponent's creature slid into slot 1, so `Target(1)` read the OPPONENT'S
 * creature's power instead of the legendary's — the surviving target was picked for both the
 * source and the amount.
 *
 * This case is not masked the way Diplomatic Relations is: the legendary's damageSource
 * (Target 1) is still legal, so the instruction does NOT fizzle — it just deals the wrong
 * amount. With the fix, positional references resolve against the position-aligned list (null
 * in dropped slots), so `Target(1)` keeps pointing at the legendary.
 *
 * Setup makes the difference observable: the opponent's creature is a 0/4 Wall of Mulch.
 *   - Correct (fixed):  amount = Blind Seer's power (3)        -> Wall takes 3 damage.
 *   - Buggy (compacted): amount = Wall of Mulch's power (0)    -> amount <= 0, no damage.
 */
class FriendlyRivalryPartialIllegalTargetTest : ScenarioTestBase() {

    init {
        context("Friendly Rivalry — partial illegal target (first creature dies in response)") {

            test("the legendary's damage must use the LEGENDARY's power, not the surviving opponent creature's") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardInHand(1, "Friendly Rivalry")
                    // Target 0: "creature you control" — dies to Bolt before resolution.
                    .withCardOnBattlefield(1, "Grizzly Bears", tapped = false, summoningSickness = false)
                    // Target 1: "up to one other legendary creature you control" — 3/3, survives.
                    .withCardOnBattlefield(1, "Blind Seer", tapped = false, summoningSickness = false)
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withLandsOnBattlefield(1, "Forest", 1)
                    // Target 2: "creature you don't control" — 0/4 Wall, survives 3 damage.
                    .withCardOnBattlefield(2, "Wall of Mulch", tapped = false, summoningSickness = false)
                    .withLandsOnBattlefield(2, "Mountain", 1)
                    .withCardInHand(2, "Lightning Bolt")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val grizzly = game.state.getBattlefield().single { entityId ->
                    val container = game.state.getEntity(entityId) ?: return@single false
                    container.get<CardComponent>()?.name == "Grizzly Bears" &&
                        container.get<ControllerComponent>()?.playerId == game.player1Id
                }
                val blindSeer = game.state.getBattlefield().single { entityId ->
                    val container = game.state.getEntity(entityId) ?: return@single false
                    container.get<CardComponent>()?.name == "Blind Seer"
                }
                val wall = game.state.getBattlefield().single { entityId ->
                    val container = game.state.getEntity(entityId) ?: return@single false
                    container.get<CardComponent>()?.name == "Wall of Mulch"
                }

                // Alice casts Friendly Rivalry: [creature you control = Grizzly Bears (0),
                // legendary = Blind Seer (1), creature you don't control = Wall of Mulch (2)].
                val rivalryId = game.findCardsInHand(1, "Friendly Rivalry").single()
                val cast = game.execute(
                    CastSpell(
                        game.player1Id,
                        rivalryId,
                        listOf(
                            ChosenTarget.Permanent(grizzly),
                            ChosenTarget.Permanent(blindSeer),
                            ChosenTarget.Permanent(wall),
                        )
                    )
                )
                withClue("Friendly Rivalry should cast: ${cast.error}") {
                    cast.error shouldBe null
                }

                game.execute(PassPriority(game.player1Id))
                withClue("Bob should have priority to respond") {
                    game.state.priorityPlayerId shouldBe game.player2Id
                }

                // Bob kills the FIRST target (Grizzly Bears) so it is dropped by 608.2b.
                val boltId = game.findCardsInHand(2, "Lightning Bolt").single()
                val bolt = game.execute(
                    CastSpell(
                        game.player2Id,
                        boltId,
                        listOf(ChosenTarget.Permanent(grizzly))
                    )
                )
                withClue("Lightning Bolt should cast: ${bolt.error}") {
                    bolt.error shouldBe null
                }

                // Resolve the whole stack: Bolt first (Grizzly dies), then Friendly Rivalry.
                var iterations = 0
                while (game.state.stack.isNotEmpty() && iterations < 40) {
                    val priorityPlayer = game.state.priorityPlayerId ?: break
                    val r = game.execute(PassPriority(priorityPlayer))
                    if (r.error != null) break
                    iterations++
                }

                withClue("Grizzly Bears should have died to Bolt") {
                    game.state.getBattlefield().contains(grizzly) shouldBe false
                }
                withClue("Wall of Mulch should survive (0/4 takes 3)") {
                    game.state.getBattlefield().contains(wall) shouldBe true
                }

                // The legendary's instruction must deal Blind Seer's power (3) to the Wall.
                // Pre-fix, Target(1) indexed the compacted list onto the Wall (power 0) -> 0 damage.
                val damage = game.state.getEntity(wall)?.get<DamageComponent>()?.amount ?: 0
                withClue("Wall must take 3 damage (Blind Seer's power via Target(1)), not 0 (the Wall's own power)") {
                    damage shouldBe 3
                }
            }
        }
    }
}
