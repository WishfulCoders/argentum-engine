package com.wingedsheep.replay

import com.wingedsheep.ai.engine.AiProfile
import com.wingedsheep.ai.engine.hidden.OpponentModel
import com.wingedsheep.arena.GameRunner
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.model.GameRng
import kotlinx.serialization.Serializable

/**
 * Counterfactual rollouts for one of a choice's candidates (mtg-draft-ai `docs/36`, Idea A of `docs/35` §2).
 *
 * [PreferenceWriter] already simulates every legal alternative at a human priority choice to its quiet state;
 * this plays each of those quiet states on to a winner with [GameRunner.playFrom], the same continuation
 * [PlayOn] uses past a rebuilt game's break. The label stops being "the human took this one" and becomes
 * "this candidate won [RollResult.wins] of [RollResult.n]".
 *
 * Two disciplines the numbers depend on, both pre-registered in `docs/36` §2:
 *
 * - **Common random numbers.** Rollout `r` of every candidate of one choice is seeded from the same
 *   `(choice, r)` pair, so candidates are compared paired and the within-choice differences — the only
 *   quantity fitted — carry far less variance than the win rates themselves.
 * - **No lookahead.** The harness stacks the user's recorded future draws on top of their library and
 *   re-forces them each half-turn; a continuation that kept them would be scoring candidates against
 *   the draws that actually came (`docs/35` §3, item 6). [shuffle] reshuffles both libraries per rollout,
 *   which removes that and is also where the variance between rollouts comes from — the pilots are
 *   deterministic, so without it every rollout of a candidate is the same game.
 */
class RolloutWriter(
    registry: CardRegistry,
    /** The pilot in the user's seat: the policy whose evaluator the targets are for. */
    val acting: Pair<String, AiProfile>,
    /** Opponent pilots, cycled by rollout index; one entry plays that pilot in every rollout. */
    val opponents: List<Pair<String, AiProfile>>,
    /** Rollouts per candidate. */
    val n: Int,
    /** Base seed; a choice's seeds derive from it, the game id and the step, so a rerun repeats. */
    val seed: Long,
    /** Reshuffle both libraries per rollout. Off only to measure what the recorded draws are worth. */
    val shuffle: Boolean = true,
    maxTurnsPerSeat: Int = 50,
) {
    private val runner = GameRunner(registry, acting.second, maxTurnsPerSeat = maxTurnsPerSeat)

    /** Every card each player owns, by name: the deck their opponent's model may assume ([PlayOn] does the same). */
    fun decklists(state: GameState): Map<EntityId, OpponentModel> =
        state.turnOrder.associateWith { id -> OpponentModel.KnownDecklist(ownedNames(state, id)) }

    /**
     * Plays [quiet] — one candidate's simulated quiet state — to a winner [n] times from the user's side.
     * [choiceSeed] is shared by every candidate of the same choice.
     */
    fun play(quiet: GameState, user: EntityId, decklists: Map<EntityId, OpponentModel>, choiceSeed: Long): RollResult {
        val seats = quiet.turnOrder
        var wins = 0
        var undecided = 0
        var illegal = 0
        var turns = 0
        val winsByOpponent = IntArray(opponents.size)
        val playedByOpponent = IntArray(opponents.size)
        val outcomes = StringBuilder(n)
        for (r in 0 until n) {
            val o = r % opponents.size
            playedByOpponent[o]++
            val profiles = seats.map { if (it == user) acting.second else opponents[o].second }
            val start = if (shuffle) reshuffled(quiet, choiceSeed * MULT + r) else quiet
            val outcome = runCatching { runner.playFrom(start, profiles, decklists) }.getOrNull()
            if (outcome == null) {
                undecided++
                outcomes.append('U')
                continue
            }
            illegal += outcome.illegal
            turns += outcome.turns
            when (outcome.winnerSeat?.let { seats[it] }) {
                user -> { wins++; winsByOpponent[o]++; outcomes.append('W') }
                null -> { undecided++; outcomes.append('U') }
                else -> outcomes.append('L')
            }
        }
        return RollResult(
            n, wins, undecided, illegal, turns, winsByOpponent.toList(), playedByOpponent.toList(), outcomes.toString(),
        )
    }

    /** [quiet] with both libraries shuffled and the generator reseeded, so this rollout draws its own game. */
    private fun reshuffled(quiet: GameState, s: Long): GameState {
        var state = quiet.copy(rng = GameRng.seeded(s))
        for (player in quiet.turnOrder) {
            val key = ZoneKey(player, Zone.LIBRARY)
            val (library, next) = state.nextRandom { shuffle(state.getZone(key)) }
            state = next.reorderZone(key, library)
        }
        return state
    }

    private fun ownedNames(state: GameState, player: EntityId): Map<String, Int> {
        val zones = state.getLibrary(player) + state.getHand(player) + state.getGraveyard(player) +
            state.getExile(player) + state.projectedState.getBattlefieldControlledBy(player)
        return zones.mapNotNull { id ->
            state.getEntity(id)?.get<CardComponent>()?.takeIf { it.ownerId == null || it.ownerId == player }?.name
        }.groupingBy { it }.eachCount()
    }

    companion object {
        private const val MULT = 1_000_003L

        /** The seed every candidate of one choice rolls out under: the base seed, the game and the step. */
        fun choiceSeed(seed: Long, gameId: String, step: Int): Long =
            seed * 31L + gameId.hashCode().toLong() * 1_000_033L + step

        /** `-Dreplay.rollPilots=ACTING[,OPPONENT...]`: arena profile names; one name plays itself in both seats. */
        fun pilots(spec: String): Pair<Pair<String, AiProfile>, List<Pair<String, AiProfile>>> {
            val names = spec.split(',').map { it.trim() }.filter { it.isNotEmpty() }
            require(names.isNotEmpty()) { "-Dreplay.rollPilots needs at least one profile name" }
            val acting = names.first().let { it to com.wingedsheep.arena.arenaProfile(it) }
            val opponents = (if (names.size == 1) names else names.drop(1))
                .map { it to com.wingedsheep.arena.arenaProfile(it) }
            return acting to opponents
        }
    }
}

/** One candidate's rollouts. [winsByOpponent] and [playedByOpponent] follow `RollHeader.opponents`. */
@Serializable
data class RollResult(
    val n: Int,
    val wins: Int,
    /** Rollouts that reached no winner (the arena's turn/action caps, or a continuation that threw). */
    val undecided: Int,
    /** Illegal actions the pilots attempted and recovered from; a validity gate, expected 0. */
    val illegal: Int,
    /** Turns summed over the rollouts, for the cost calibration. */
    val turns: Int,
    val winsByOpponent: List<Int>,
    val playedByOpponent: List<Int>,
    /**
     * Rollout by rollout, `W`/`L`/`U` in rollout-index order. The labels are paired across a choice's
     * candidates by that index (common random numbers), so the per-index differences are the low-variance
     * estimator, and a subsample of the first `m` characters is the ranking this choice would have had at
     * budget `m` — which is how `docs/36` §3's stability curve is drawn without rerunning anything.
     */
    val outcomes: String,
)
