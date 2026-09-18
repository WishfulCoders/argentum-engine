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
    /** Turns where a land in hand would have cast something stranded, and those where the AI never played one. */
    val probeFixable: Int = 0,
    val probeFixMissed: Int = 0,
    val millis: Long = 0,
    /** [GameRunner.Outcome.holding], seat by seat, with `-Darena.holding=true`. */
    val holding: List<List<Int>>? = null,
    /** [GameRunner.Outcome.cycle] and [GameRunner.Outcome.casts], with `-Darena.holding=true`. */
    val cycle: List<List<Int>>? = null,
    val casts: List<Map<String, Int>>? = null,
    val tappedOut: List<List<Int>>? = null,
    /** [GameRunner.Outcome.cards], seat by seat, with `-Darena.cards=true`; fields [GameRunner.CARD_FIELDS]. */
    val cards: List<Map<String, List<Int>>>? = null,
    /** [GameRunner.Outcome.lastWindow], with `-Darena.cards=true`; fields [GameRunner.LAST_WINDOW_FIELDS]. */
    val lastWindow: List<List<Int>>? = null,
    /** [GameRunner.Outcome.gaps], with `-Darena.cards=true -Darena.gaps=true`; bins [GameRunner.GAP_BINS]. */
    val gaps: List<Map<String, List<Int>>>? = null,
    /** Learned target-policy telemetry; zero for ordinary heuristic arena runs. */
    val policyActions: Int = 0,
    val policyPasses: Int = 0,
    val policyCasts: Int = 0,
    val policyLandPlays: Int = 0,
    val policyActivations: Int = 0,
    val policyOtherActions: Int = 0,
    val policyPaymentActions: Int = 0,
    val policyAttacks: Int = 0,
    val policyAttackers: Int = 0,
    val policyBlocks: Int = 0,
    val policyBlockers: Int = 0,
    val policyOrderMismatches: Int = 0,
    val policyFailures: Int = 0,
    val policyFirstFailure: String? = null,
    val policyIllegal: Int = 0,
    val policyFirstRejection: String? = null,
) {
    val key: String get() = "$target|$opponent|$game"
}

@OptIn(ExperimentalSerializationApi::class)
val arenaJson = Json {
    ignoreUnknownKeys = true
    namingStrategy = JsonNamingStrategy.SnakeCase
    encodeDefaults = true
}
