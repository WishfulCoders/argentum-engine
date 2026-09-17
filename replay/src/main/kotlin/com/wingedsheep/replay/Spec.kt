package com.wingedsheep.replay

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNamingStrategy

/**
 * One game as written by `mtgdraft replay-export` (RL-MTG-drafts, `reconstruct/export.py`).
 *
 * Card names are canonical front-face names. Sides are `"user"` (the 17Lands uploader, whose
 * hand and draws are known card by card) and `"oppo"` (known only through what they played).
 * Half-turns are in play order; half-turn `i` is engine turn `i + 1`.
 */
@Serializable
data class GameSpec(
    val gameId: String,
    /** The 17Lands set code; its printings win where a name is printed in several sets. */
    val set: String = "ECL",
    val onPlay: Boolean,
    val won: Boolean,
    val oppColors: String = "",
    val userDeck: List<String>,
    val openingHand: List<String>,
    /** Every card the opponent was seen with, with multiplicity; the rest of their deck is filler. */
    val oppoKnown: List<String>,
    val halfTurns: List<HalfTurnSpec>,
)

@Serializable
data class HalfTurnSpec(
    val active: String,
    val turn: Int,
    /** The game's final half-turn: its snapshot is taken when the game ended, often mid-turn. */
    val last: Boolean,
    /**
     * The user's draws: on their half-turns, in order (tutors excluded); on the opponent's, the
     * cards that arrived in their hand with nothing logged (a rummage, a cantrip), inferred, so
     * possibly a card returned from the graveyard instead.
     */
    val drawn: List<String> = emptyList(),
    val tutored: List<String> = emptyList(),
    /** Cards the user played or cast that were not in their hand: from exile (an impulse draw), the graveyard, a bounce. */
    val outsideHand: List<String> = emptyList(),
    val lands: List<String> = emptyList(),
    val creatures: List<String> = emptyList(),
    val noncreatures: List<String> = emptyList(),
    val discarded: List<String> = emptyList(),
    /** Cards plotted, per side (OTJ; never logged, inferred by the exporter), cast later from exile. */
    val plotted: Map<String, List<String>> = emptyMap(),
    /** Room doors unlocked by the special action, per side (DSK; inferred from the doors' unlock triggers). */
    val unlocked: Map<String, List<String>> = emptyMap(),
    val instants: Map<String, List<String>> = emptyMap(),
    /** Prepare cards' spells cast, per side (SOS): a copy cast while the creature is prepared, not from hand. */
    val prepared: Map<String, List<String>> = emptyMap(),
    /** Permanents the non-active player added: flash casts, which the file never records. */
    val flash: Map<String, List<String>> = emptyMap(),
    /** Cards cast face down (disguise, morph), per side: the opponent's, which 17Lands logs unnamed. */
    val faceDown: Map<String, Int> = emptyMap(),
    /** What those face-down cards were, where a later turn-up or death showed it ("" if never). */
    val faceDownAs: Map<String, List<String>> = emptyMap(),
    /** What the opponent's face-down permanents that arrived uncast (manifest dread, cloak) were, where shown ("" if never). */
    val manifestedAs: Map<String, List<String>> = emptyMap(),
    /** Face-down cards turned face up, per side: the opponent's, inferred by the exporter. */
    val turnedUp: Map<String, List<String>> = emptyMap(),
    /** Activated abilities per side, in the engine's notation (`{T}, Blight 1: Surveil 1.`). */
    val activated: Map<String, List<String>> = emptyMap(),
    val attacked: List<String> = emptyList(),
    val blocked: List<String> = emptyList(),
    val blocking: List<String> = emptyList(),
    val eot: EotSpec,
)

@Serializable
data class EotSpec(
    val userHand: List<String>,
    val oppoHand: Int,
    /** Non-token permanents per side, lands included. */
    val battlefield: Map<String, List<String>>,
    val tokens: Map<String, Int> = emptyMap(),
    val life: Map<String, Double>,
)

/** One line of the harness output. */
@Serializable
data class GameResult(
    val gameId: String,
    /** `reproduced`, `failed`, or `skipped` (the game could not be set up). */
    val status: String,
    val halfTurns: Int,
    /** Half-turns, in order, for which some engine line matched the recorded snapshot. */
    val reproduced: Int,
    val failedAt: Int? = null,
    val reason: String? = null,
    /** Matching end states kept after each half-turn. */
    val beam: List<Int> = emptyList(),
    /** Search nodes expanded for each half-turn. */
    val nodes: List<Int> = emptyList(),
    val millis: Long = 0,
)

@OptIn(ExperimentalSerializationApi::class)
val specJson = Json {
    ignoreUnknownKeys = true
    namingStrategy = JsonNamingStrategy.SnakeCase
    encodeDefaults = true
}
