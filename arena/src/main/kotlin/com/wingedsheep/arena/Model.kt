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

/** One opt-in comparison at a BC pass, including whether the pilot's action was callable. */
@Serializable
data class RankProbeEvent(
    val step: String,
    val ownTurn: Boolean,
    val teacherFamily: String,
    val bestNonPassFamily: String,
    /** One-based rank of the pilot's action-type/source/description template; null if absent. */
    val teacherRank: Int? = null,
    /** One-based rank within the pilot action's family (casts vs casts, activations vs activations). */
    val teacherFamilyRank: Int? = null,
    val teacherManaAbility: Boolean = false,
    val bestNonPassManaAbility: Boolean = false,
    /** Highest non-mana cast/activation, ignoring land drops and mana abilities. */
    val bestSpendFamily: String = "none",
    val bestSpendSameSource: Boolean = false,
    /** Same source permanent/card, even if the action variant differs. */
    val bestSameSource: Boolean = false,
    /** Number of earlier/current BC decisions at which this spell or ability was callable. */
    val teacherAvailableWindows: Int? = null,
    /** Why the pilot's source/ability was absent from (or present in) the callable ranking. */
    val teacherCandidateStatus: String = "unknown",
    val teacherAdditionalCostType: String? = null,
    /** Logit(pass) minus logit(best callable non-pass); diagnostic, not a calibrated value. */
    val passMargin: Double? = null,
    /** Logit(pass) minus best cast/non-mana-activation, excluding land and payment setup. */
    val spendMargin: Double? = null,
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
    /** Policy-family → frozen-pilot-family counts with `-Darena.policyShadowTeacher=true`. */
    val policyTeacherFamilies: Map<String, Int> = emptyMap(),
    /** BC pass/ranking comparisons with `-Darena.policyRankProbe=true`; null when disabled. */
    val rankProbe: List<RankProbeEvent>? = null,
    /** Completed target turns with a legal land play, and how many took one by turn end. */
    val landOpportunityTurns: Int = 0,
    val landPlayedOpportunityTurns: Int = 0,
    /** Terminal/incomplete target turns kept separate from the completed-turn rate. */
    val landIncompleteOpportunityTurns: Int = 0,
    val landPlayedIncompleteOpportunityTurns: Int = 0,
    /** Complete cycles with an unchanged positive mana-source count, and all sources untapped at both ends. */
    val stableManaCycles: Int = 0,
    val fullyUntappedManaCycles: Int = 0,
    /** Stable-source cycles with zero target-seat ManaSpentEvent mana, not merely untapped endpoints. */
    val noManaSpentStableCycles: Int = 0,
) {
    val key: String get() = "$target|$opponent|$game"
}

@OptIn(ExperimentalSerializationApi::class)
val arenaJson = Json {
    ignoreUnknownKeys = true
    namingStrategy = JsonNamingStrategy.SnakeCase
    encodeDefaults = true
}
