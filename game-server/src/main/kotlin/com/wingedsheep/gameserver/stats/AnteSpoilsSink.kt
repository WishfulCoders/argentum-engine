package com.wingedsheep.gameserver.stats

import com.wingedsheep.gameserver.persistence.AnteSpoilRepository
import com.wingedsheep.gameserver.persistence.AnteSpoilRow
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import java.util.UUID

/** A card that changed hands in a game played for ante, between two seats. */
data class RecordedSpoil(
    val gameId: String,
    val cardName: String,
    val imageUri: String?,
    /** Null for a guest or AI seat — they keep no profile, so they get no row. */
    val winnerUserId: UUID?,
    val winnerName: String,
    val loserUserId: UUID?,
    val loserName: String,
)

/**
 * Records ante spoils for the players' profiles. Like [MatchResultSink], which implementation is
 * wired depends on whether accounts are enabled, so the game-over path can call it unconditionally.
 */
interface AnteSpoilsSink {
    fun record(spoils: List<RecordedSpoil>)
}

/** Default: accounts disabled — nothing to put spoils on. */
@Component
@ConditionalOnProperty(name = ["accounts.enabled"], havingValue = "false", matchIfMissing = true)
class NoOpAnteSpoilsSink : AnteSpoilsSink {
    override fun record(spoils: List<RecordedSpoil>) = Unit
}

@Component
@ConditionalOnProperty(name = ["accounts.enabled"], havingValue = "true")
class JdbcAnteSpoilsSink(private val repository: AnteSpoilRepository) : AnteSpoilsSink {
    override fun record(spoils: List<RecordedSpoil>) {
        val rows = spoils.flatMap { spoil ->
            listOfNotNull(
                spoil.winnerUserId?.let {
                    AnteSpoilRow(
                        userId = it, gameId = spoil.gameId, won = true, cardName = spoil.cardName,
                        imageUri = spoil.imageUri, opponentName = spoil.loserName,
                    )
                },
                spoil.loserUserId?.let {
                    AnteSpoilRow(
                        userId = it, gameId = spoil.gameId, won = false, cardName = spoil.cardName,
                        imageUri = spoil.imageUri, opponentName = spoil.winnerName,
                    )
                },
            )
        }
        if (rows.isNotEmpty()) repository.saveAll(rows)
    }
}
