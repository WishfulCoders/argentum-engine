package com.wingedsheep.gameserver.matchmaking

import com.wingedsheep.sdk.core.DeckFormat
import com.wingedsheep.sdk.model.EntityId
import kotlinx.serialization.Serializable
import java.util.UUID
import kotlin.math.abs

/**
 * What kind of game a queue makes. Only [CONSTRUCTED] brings a deck (and names a [QueueKey.format]);
 * the other three hand every seat its cards, so a matched pair has nothing to prepare.
 */
@Serializable
enum class MatchmakingMode(
    /** Whether a ranked queue exists for this mode. */
    val rankable: Boolean,
) {
    /** A sealed pool each, opened and built by the server from one random set shared by both. */
    RANDOM_DECK(rankable = true),

    /** Each player picks two Jumpstart packs and plays the 40 cards they make. */
    JUMP_IN(rankable = false),

    /** Sixty basics and the Momir Vig avatar, flipping random creatures. */
    MOMIR_BASIC(rankable = false),

    /** Bring a deck legal in [QueueKey.format]. */
    CONSTRUCTED(rankable = true),
}

/**
 * What a player is queueing for: the [mode], the deck format for a [MatchmakingMode.CONSTRUCTED] queue
 * (null for every other mode), and whether the game counts toward their rating. Two players only ever
 * pair when their keys are equal, so a casual and a ranked queue for the same game are separate pools.
 * Build one through [of], which is what keeps those three fields consistent.
 */
@Serializable
data class QueueKey(val mode: MatchmakingMode, val format: DeckFormat?, val ranked: Boolean) {
    companion object {
        /**
         * The queue a join request names, or why it names none. A request without a [mode] (an older
         * client) means what it used to: a format is Constructed, no format the random-pool queue.
         */
        fun of(mode: MatchmakingMode?, format: DeckFormat?, ranked: Boolean): Result {
            val resolved = mode ?: if (format != null) MatchmakingMode.CONSTRUCTED else MatchmakingMode.RANDOM_DECK
            if (resolved == MatchmakingMode.CONSTRUCTED && format == null) return Result.Invalid("Pick a format to search for")
            if (ranked && !resolved.rankable) return Result.Invalid("${displayName(resolved)} has no ranked queue")
            val key = QueueKey(resolved, format.takeIf { resolved == MatchmakingMode.CONSTRUCTED }, ranked)
            return Result.Valid(key)
        }

        fun displayName(mode: MatchmakingMode): String = when (mode) {
            MatchmakingMode.RANDOM_DECK -> "Random deck"
            MatchmakingMode.JUMP_IN -> "Jump In"
            MatchmakingMode.MOMIR_BASIC -> "Momir Basic"
            MatchmakingMode.CONSTRUCTED -> "Constructed"
        }
    }

    sealed interface Result {
        data class Valid(val key: QueueKey) : Result
        data class Invalid(val reason: String) : Result
    }
}

/** One searching player. [rating] is the player's rating for the key's mode, snapshotted at join. */
data class QueueEntry(
    val playerId: EntityId,
    val userId: UUID?,
    val playerName: String,
    val key: QueueKey,
    val rating: Double,
    val joinedAt: Long,
)

/** Two players the queue paired, waiting for both to accept before a lobby is made for them. */
data class PendingMatch(
    val matchId: String,
    val first: QueueEntry,
    val second: QueueEntry,
    val expiresAt: Long,
    val accepted: Set<EntityId> = emptySet(),
) {
    val players: List<QueueEntry> get() = listOf(first, second)
    fun opponentOf(playerId: EntityId): QueueEntry = if (first.playerId == playerId) second else first
    fun involves(playerId: EntityId): Boolean = first.playerId == playerId || second.playerId == playerId
}

/** What the queue tells its owner to do; the owner turns these into messages and lobbies. */
sealed interface MatchmakingEvent {
    /** [entry] is (back) in the queue. [notice] explains a requeue, e.g. the opponent declined. */
    data class Searching(val entry: QueueEntry, val notice: String? = null) : MatchmakingEvent

    /** [playerId] is no longer queued or matched. [notice] explains why when it wasn't their own leave. */
    data class Idle(val playerId: EntityId, val notice: String? = null) : MatchmakingEvent

    /** A pairing was proposed; each player is asked to accept. */
    data class Found(val match: PendingMatch) : MatchmakingEvent

    /** [playerId] accepted; their opponent hasn't yet. */
    data class Accepted(val match: PendingMatch, val playerId: EntityId) : MatchmakingEvent

    /** Both accepted — the owner seats them together. */
    data class Confirmed(val match: PendingMatch) : MatchmakingEvent
}

/**
 * The matchmaking queue as a pure state machine: no sockets, no Spring, no clock of its own. Every
 * operation returns the [MatchmakingEvent]s it caused and the owner (`MatchmakingService`) serializes
 * calls, so this class is not thread-safe on its own.
 *
 * Pairing, per [QueueKey]:
 *  - **Casual** is first come, first served — the two longest-waiting players pair.
 *  - **Ranked** pairs the longest-waiting player with the closest-rated candidate whose rating is within
 *    the wider of the two players' [ratingBand]s. The band grows with time waited and opens fully after
 *    [OPEN_BAND_AFTER_MS], so a small population always finds a game eventually.
 *
 * Two players where either has blocked the other are never paired, however long they wait.
 *
 * A found pair leaves the queue and waits [acceptWindowMs] for both players to accept. A decline, a
 * timeout or a disconnect drops the player who didn't accept; whoever did goes back in the queue at
 * their original join time, so they don't lose their place for someone else's absence.
 */
class MatchmakingQueue(
    private val acceptWindowMs: Long = DEFAULT_ACCEPT_WINDOW_MS,
    private val newMatchId: () -> String = { UUID.randomUUID().toString() },
) {
    private val entries = LinkedHashMap<EntityId, QueueEntry>()
    private val pending = LinkedHashMap<String, PendingMatch>()

    fun entryOf(playerId: EntityId): QueueEntry? = entries[playerId]
    fun pendingMatchOf(playerId: EntityId): PendingMatch? = pending.values.firstOrNull { it.involves(playerId) }
    fun isBusy(playerId: EntityId): Boolean = playerId in entries || pendingMatchOf(playerId) != null

    /** Searching players per queue. Players waiting on an accept prompt aren't counted. */
    fun counts(): Map<QueueKey, Int> = entries.values.groupingBy { it.key }.eachCount()

    /**
     * Put [entry] in the queue, replacing any earlier entry for the same player (switching queues).
     * Returns no events when the player is mid-accept — that prompt has to be answered first.
     */
    fun join(entry: QueueEntry): List<MatchmakingEvent> {
        if (pendingMatchOf(entry.playerId) != null) return emptyList()
        entries[entry.playerId] = entry
        return listOf(MatchmakingEvent.Searching(entry))
    }

    /** Leave the queue, or decline a pending match (which requeues the opponent if they had accepted). */
    fun leave(playerId: EntityId): List<MatchmakingEvent> {
        pendingMatchOf(playerId)?.let { return drop(it, setOf(playerId), notice = null) }
        return if (entries.remove(playerId) != null) listOf(MatchmakingEvent.Idle(playerId)) else emptyList()
    }

    fun respond(playerId: EntityId, matchId: String, accept: Boolean): List<MatchmakingEvent> {
        val match = pending[matchId]?.takeIf { it.involves(playerId) } ?: return emptyList()
        if (!accept) return drop(match, setOf(playerId), notice = null)
        val updated = match.copy(accepted = match.accepted + playerId)
        if (updated.accepted.size == 2) {
            pending.remove(matchId)
            return listOf(MatchmakingEvent.Confirmed(updated))
        }
        pending[matchId] = updated
        return listOf(MatchmakingEvent.Accepted(updated, playerId))
    }

    /** Put a player back at their original place, e.g. when their confirmed opponent vanished. */
    fun requeue(entry: QueueEntry, notice: String): List<MatchmakingEvent> {
        entries[entry.playerId] = entry
        return listOf(MatchmakingEvent.Searching(entry, notice))
    }

    /**
     * Drop players [isAvailable] reports gone (disconnected, or now in a lobby or game elsewhere), expire
     * unanswered prompts, then pair whoever can be paired at [now].
     */
    fun tick(
        now: Long,
        isAvailable: (EntityId) -> Boolean,
        isBlocked: (QueueEntry, QueueEntry) -> Boolean = { _, _ -> false },
    ): List<MatchmakingEvent> {
        val events = mutableListOf<MatchmakingEvent>()

        for (playerId in entries.keys.filterNot(isAvailable)) {
            entries.remove(playerId)
            events += MatchmakingEvent.Idle(playerId)
        }

        for (match in pending.values.toList()) {
            val gone = match.players.map { it.playerId }.filterNot(isAvailable)
            when {
                gone.isNotEmpty() -> events += drop(match, gone.toSet(), notice = null)
                now >= match.expiresAt -> {
                    val missed = match.players.map { it.playerId }.filterNot { it in match.accepted }.toSet()
                    events += drop(match, missed, notice = "You didn't accept the match in time")
                }
            }
        }

        events += pair(now, isBlocked)
        return events
    }

    private fun pair(now: Long, isBlocked: (QueueEntry, QueueEntry) -> Boolean): List<MatchmakingEvent> {
        val events = mutableListOf<MatchmakingEvent>()
        for ((key, group) in entries.values.groupBy { it.key }) {
            val waiting = group.sortedBy { it.joinedAt }.toMutableList()
            while (waiting.size >= 2) {
                val first = waiting.removeAt(0)
                val candidates = waiting.filter { canPair(first, it, now) && !isBlocked(first, it) }
                val second = if (key.ranked) {
                    candidates.minByOrNull { abs(it.rating - first.rating) }
                } else {
                    candidates.firstOrNull()
                } ?: continue
                waiting.remove(second)
                entries.remove(first.playerId)
                entries.remove(second.playerId)
                val match = PendingMatch(newMatchId(), first, second, expiresAt = now + acceptWindowMs)
                pending[match.matchId] = match
                events += MatchmakingEvent.Found(match)
            }
        }
        return events
    }

    private fun canPair(a: QueueEntry, b: QueueEntry, now: Long): Boolean {
        if (a.playerId == b.playerId) return false
        // One account in two tabs never plays itself.
        if (a.userId != null && a.userId == b.userId) return false
        if (!a.key.ranked) return true
        val band = maxOf(ratingBand(now - a.joinedAt), ratingBand(now - b.joinedAt))
        return abs(a.rating - b.rating) <= band
    }

    /**
     * End [match]: each player in [dropped] goes idle with [notice]; the other player goes back in the
     * queue at their original join time.
     */
    private fun drop(match: PendingMatch, dropped: Set<EntityId>, notice: String?): List<MatchmakingEvent> {
        pending.remove(match.matchId)
        return match.players.map { entry ->
            if (entry.playerId in dropped) {
                MatchmakingEvent.Idle(entry.playerId, notice)
            } else {
                entries[entry.playerId] = entry
                MatchmakingEvent.Searching(entry, "Your opponent didn't accept — back in the queue")
            }
        }
    }

    companion object {
        const val DEFAULT_ACCEPT_WINDOW_MS: Long = 60_000
        const val BASE_RATING_BAND: Double = 100.0
        const val BAND_STEP: Double = 50.0
        const val BAND_STEP_MS: Long = 10_000
        const val OPEN_BAND_AFTER_MS: Long = 120_000

        /** How far apart two ranked ratings may be after waiting [waitedMs]. */
        fun ratingBand(waitedMs: Long): Double =
            if (waitedMs >= OPEN_BAND_AFTER_MS) Double.POSITIVE_INFINITY
            else BASE_RATING_BAND + BAND_STEP * (waitedMs.coerceAtLeast(0) / BAND_STEP_MS)
    }
}
