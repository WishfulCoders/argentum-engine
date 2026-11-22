package com.wingedsheep.gameserver.matchmaking

import com.wingedsheep.gameserver.persistence.UserRatingRepository
import com.wingedsheep.gameserver.ranking.Elo
import com.wingedsheep.gameserver.ranking.RankedMode
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import java.util.UUID

/**
 * A player's current rating in a [RankedMode], for pairing ranked queue entries. Wired like
 * [com.wingedsheep.gameserver.ranking.RankedResultSink]: with accounts disabled nobody has a rating
 * and everyone sits at the starting value.
 */
interface RatingLookup {
    fun ratingOf(userId: UUID, mode: RankedMode): Double
}

@Component
@ConditionalOnProperty(name = ["accounts.enabled"], havingValue = "false", matchIfMissing = true)
class StartingRatingLookup : RatingLookup {
    override fun ratingOf(userId: UUID, mode: RankedMode): Double = Elo.STARTING_RATING
}

@Component
@ConditionalOnProperty(name = ["accounts.enabled"], havingValue = "true")
class JdbcRatingLookup(private val userRatings: UserRatingRepository) : RatingLookup {
    override fun ratingOf(userId: UUID, mode: RankedMode): Double =
        userRatings.findByUserIdAndMode(userId, mode.name)?.rating ?: Elo.STARTING_RATING
}
