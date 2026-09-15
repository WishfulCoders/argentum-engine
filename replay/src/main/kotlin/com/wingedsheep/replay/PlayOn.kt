package com.wingedsheep.replay

import com.wingedsheep.ai.engine.AiProfile
import com.wingedsheep.ai.engine.hidden.OpponentModel
import com.wingedsheep.arena.GameRunner
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.model.EntityId
import kotlinx.serialization.Serializable

/**
 * C1 (mtg-draft-ai `docs/27` §5): a game the harness could not rebuild to the end, played on from the start of its
 * failed half-turn ([Reconstructor.acceptedState]) with one pilot in both seats, once per pilot in [pilots].
 *
 * The user's recorded later draws are already on top of their library (the harness stacks the whole game's draws at
 * the start and re-forces them every user half-turn); the opponent keeps the hand and library the harness gave them,
 * their cards seen in the record plus basics. Every pilot starts from the same position, so pilots compare paired;
 * one game is a noisy bit, and only paired differences between pilots mean anything. The record's winner is the
 * reference. The moves are an agent's, never human labels.
 */
class PlayOn(registry: CardRegistry, private val pilots: List<Pair<String, AiProfile>>) {
    private val runners = pilots.map { (_, profile) -> GameRunner(registry, profile, measureHolding = true) }

    fun play(spec: GameSpec, result: GameResult, start: GameState): List<PlayOnRecord> {
        val seats = Seats.of(start)
        val decklists = start.turnOrder.associateWith { id -> OpponentModel.KnownDecklist(ownedNames(start, id)) }
        return pilots.mapIndexed { i, (name, profile) ->
            val o = runners[i].playFrom(start, start.turnOrder.map { profile }, decklists)
            val userSeat = start.turnOrder.indexOf(seats.user)
            PlayOnRecord(
                gameId = spec.gameId, set = spec.set, pilot = name,
                failedAt = result.failedAt ?: 0, halfTurns = spec.halfTurns.size, breakReason = result.reason,
                userWonRecord = spec.won,
                winner = o.winnerSeat?.let { seats.sideOf(start.turnOrder[it]) },
                turns = o.turns, actions = o.actions, illegal = o.illegal, end = o.reason,
                cycleUser = o.cycle?.get(userSeat), castsUser = o.casts?.get(userSeat),
            )
        }
    }

    /** Every card [player] owns, by name: the deck their opponent's model may assume. */
    private fun ownedNames(state: GameState, player: EntityId): Map<String, Int> {
        val zones = state.getLibrary(player) + state.getHand(player) + state.getGraveyard(player) + state.getExile(player) +
            state.projectedState.getBattlefieldControlledBy(player)
        return zones.mapNotNull { id ->
            state.getEntity(id)?.get<CardComponent>()?.takeIf { it.ownerId == null || it.ownerId == player }?.name
        }.groupingBy { it }.eachCount()
    }
}

/** One pilot's play-on of one broken game. [winner] is `user`, `oppo` or null (no winner within the arena's caps). */
@Serializable
data class PlayOnRecord(
    val gameId: String,
    val set: String,
    val pilot: String,
    val failedAt: Int,
    val halfTurns: Int,
    val breakReason: String?,
    val userWonRecord: Boolean,
    val winner: String?,
    val turns: Int,
    val actions: Int,
    val illegal: Int,
    val end: String,
    /** The user's seat's mana over the turn cycle and casts by turn/step/kind ([GameRunner.Outcome.cycle], `.casts`). */
    val cycleUser: List<Int>? = null,
    val castsUser: Map<String, Int>? = null,
)
