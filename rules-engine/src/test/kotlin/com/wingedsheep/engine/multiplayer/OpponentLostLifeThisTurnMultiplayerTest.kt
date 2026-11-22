package com.wingedsheep.engine.multiplayer

import com.wingedsheep.engine.core.GameConfig
import com.wingedsheep.engine.core.GameInitializer
import com.wingedsheep.engine.core.PlayerConfig
import com.wingedsheep.engine.event.TriggerMatcher
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.player.LifeLostThisTurnComponent
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.blb.cards.FireglassMentor
import com.wingedsheep.mtg.sets.definitions.blb.cards.FlamecacheGecko
import com.wingedsheep.sdk.core.Format
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * "If an opponent lost life this turn" in multiplayer — the intervening-if on Fireglass Mentor and
 * Flamecache Gecko (BLB).
 *
 * "An opponent" is *any* opponent: in a four-player free-for-all one bleeding seat is enough, and
 * the controller's own life loss never counts. In Two-Headed Giant a teammate is not an opponent
 * (CR 102.3), so only the other team's heads count. Fireglass Mentor's "your second main phase"
 * is the team's turn in Two-Headed Giant, so it fires for either head of the active team.
 */
class OpponentLostLifeThisTurnMultiplayerTest : FunSpec({

    fun game(twoHeadedGiant: Boolean): Pair<GameState, List<EntityId>> {
        val registry = CardRegistry().also { it.register(TestCards.all) }
        val players = (1..4).map { PlayerConfig("Player $it", Deck.of("Forest" to 40)) }
        val config = if (twoHeadedGiant) {
            GameConfig(
                format = Format.TwoHeadedGiant(),
                players = players,
                teams = listOf(listOf(0, 1), listOf(2, 3)),
                startingPlayerIndex = 0,
                skipMulligans = true,
            )
        } else {
            GameConfig(players = players, startingPlayerIndex = 0, skipMulligans = true)
        }
        val result = GameInitializer(registry).initializeGame(config)
        return result.state to result.playerIds
    }

    fun GameState.lostLife(player: EntityId) = updateEntity(player) { it.with(LifeLostThisTurnComponent) }

    val evaluator = PredicateEvaluator(cardRegistry = null)
    val cards = listOf(FireglassMentor, FlamecacheGecko)

    test("free-for-all: any one opponent losing life satisfies the condition, the controller's own does not") {
        val (state, p) = game(twoHeadedGiant = false)
        val context = EffectContext(sourceId = null, controllerId = p[0])
        for (card in cards) {
            val condition = card.triggeredAbilities.first().interveningIf!!
            fun holds(s: GameState) = evaluator.conditions.evaluate(s, condition, context)

            holds(state) shouldBe false
            holds(state.lostLife(p[0])) shouldBe false
            holds(state.lostLife(p[1])) shouldBe true
            holds(state.lostLife(p[3])) shouldBe true
        }
    }

    test("Two-Headed Giant: a teammate losing life is not an opponent losing life") {
        val (state, p) = game(twoHeadedGiant = true)
        // Player 2 controls the card; player 1 is their teammate, players 3 and 4 the other team.
        val context = EffectContext(sourceId = null, controllerId = p[1])
        for (card in cards) {
            val condition = card.triggeredAbilities.first().interveningIf!!
            fun holds(s: GameState) = evaluator.conditions.evaluate(s, condition, context)

            holds(state.lostLife(p[0])) shouldBe false
            holds(state.lostLife(p[2])) shouldBe true
            holds(state.lostLife(p[3])) shouldBe true
        }
    }

    test("Fireglass Mentor's second-main-phase trigger: controller's turn only, either head in Two-Headed Giant") {
        val matcher = TriggerMatcher(evaluator, evaluator.conditions)
        val trigger = FireglassMentor.triggeredAbilities.first().trigger
        fun fires(state: GameState, controller: EntityId) =
            matcher.matchesStepTrigger(trigger, Step.POSTCOMBAT_MAIN, controller, state)

        val (ffa, f) = game(twoHeadedGiant = false)
        fires(ffa, f[0]) shouldBe true
        fires(ffa, f[1]) shouldBe false

        val (thg, t) = game(twoHeadedGiant = true)
        fires(thg, t[0]) shouldBe true
        fires(thg, t[1]) shouldBe true
        fires(thg, t[2]) shouldBe false
    }
})
