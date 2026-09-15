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
    val millis: Long = 0,
    /** [GameRunner.Outcome.holding], seat by seat, with `-Darena.holding=true`. */
    val holding: List<List<Int>>? = null,
    /** [GameRunner.Outcome.cycle] and [GameRunner.Outcome.casts], with `-Darena.holding=true`. */
    val cycle: List<List<Int>>? = null,
    val casts: List<Map<String, Int>>? = null,
) {
    val key: String get() = "$target|$opponent|$game"
}

@OptIn(ExperimentalSerializationApi::class)
val arenaJson = Json {
    ignoreUnknownKeys = true
    namingStrategy = JsonNamingStrategy.SnakeCase
    encodeDefaults = true
}
