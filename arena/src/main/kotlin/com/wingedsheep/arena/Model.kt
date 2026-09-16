package com.wingedsheep.arena

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNamingStrategy

/**
 * One deck as written by `mtgdraft engine-export` (RL-MTG-drafts, `engine/export.py`).
 * [cards] lists canonical front-face names with multiplicity, basics included (40 entries).
 * [role] is `"target"` (a held-out deck whose engine win rate is measured) or `"opponent"`
 * (one of the fixed opponents every target plays).
 */
@Serializable
data class DeckSpec(
    val id: String,
    val role: String,
    val cards: List<String>,
)

/** One game. [game] indexes the games of a (target, opponent) pair; even games seat the target first. */
@Serializable
data class GameRecord(
    val target: String,
    val opponent: String,
    val game: Int,
    val targetSeat: Int,
    val seed: Long,
    val winnerSeat: Int? = null,
    val targetWon: Boolean? = null,
    val turns: Int = 0,
    val actions: Int = 0,
    val illegal: Int = 0,
    val life: List<Int> = emptyList(),
    /** Empty when the game ended by the rules; otherwise why the loop stopped. */
    val reason: String = "",
    /**
     * `GameRunner.StrandedProbe` at the target seat, all zero unless `-Darena.probeStranded=true`:
     * priority windows examined, nonland cards in hand whose mana value the untapped lands cover,
     * how many of those the enumerator offered no affordable cast for, and how many distinct cards
     * were ever stranded. mtg-draft-ai `docs/33` §12.
     */
    val probeWindows: Int = 0,
    val probeAffordable: Int = 0,
    val probeStranded: Int = 0,
    val probeStrandedCards: Int = 0,
    /** Windows where a land in hand would have cast something stranded, and where the AI played another. */
    val probeFixable: Int = 0,
    val probeFixMissed: Int = 0,
    val millis: Long = 0,
) {
    val key: String get() = "$target|$opponent|$game"
}

@OptIn(ExperimentalSerializationApi::class)
val arenaJson = Json {
    ignoreUnknownKeys = true
    namingStrategy = JsonNamingStrategy.SnakeCase
    encodeDefaults = true
}
