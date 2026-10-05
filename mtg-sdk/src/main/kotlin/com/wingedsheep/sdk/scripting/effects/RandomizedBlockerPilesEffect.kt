package com.wingedsheep.sdk.scripting.effects

import com.wingedsheep.sdk.scripting.Duration
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Replace blocker declarations with optional creature piles assigned randomly to attackers. */
@Serializable
@SerialName("RandomizedBlockerPiles")
data class RandomizedBlockerPilesEffect(val duration: Duration = Duration.EndOfTurn) : Effect {
    override val description: String = "Divide blockers into randomly assigned piles"
}
