package com.wingedsheep.gym

import kotlinx.serialization.Serializable

/**
 * Asks for a paired outcome label at the learner's current priority decision (mtg-draft-ai `docs/50`).
 *
 * @property worldSeeds one determinized world per seed. Every candidate is played out in every world,
 *   which is the pairing: within a world the candidates differ only by the action.
 * @property maxCandidates label at most this many candidates. The pass and the pilot's own choice are
 *   always among them; the rest are a uniform sample, so a capped decision is still an unbiased
 *   comparison (taking the best-scoring ones would label only what the evaluator already likes).
 * @property sampleSeed seeds that sample.
 * @property timeBudgetSeconds stop starting new worlds once the label has taken this long, and return
 *   the worlds that finished; 0 is no budget. A pathological position can take hours (mtg-draft-ai
 *   `docs/36` §9 lost days to one), and a budget bounds it without discarding the work already done.
 *   Worlds are played in the outer loop, so a cut-off leaves every candidate the same worlds and the
 *   labels stay paired.
 */
@Serializable
data class ValueLabelRequest(
    val worldSeeds: List<Long>,
    val maxCandidates: Int = Int.MAX_VALUE,
    val sampleSeed: Long = 0,
    val timeBudgetSeconds: Double = 0.0,
)

/**
 * One decision, labelled. The shape is `PrefRoot`'s candidate list, so `fit_rollout.py` and
 * `fit_cards.py` read it once the driver wraps it with a game id.
 *
 * @property features [RawBoardFeatures] names, in the order of every candidate's `f`.
 * @property candidates the scored candidates that were labelled, pass first when present.
 * @property worldSeeds the worlds actually played, a prefix of the request's when a budget cut it short.
 * @property unlabelled scored candidates left out by [ValueLabelRequest.maxCandidates].
 * @property pilotIndex index into [candidates] of the pilot's choice.
 */
@Serializable
data class ValueLabel(
    val turnNumber: Int,
    val step: String,
    val activeTurn: Boolean,
    val features: List<String>,
    val candidates: List<ValueCandidate>,
    val unlabelled: Int,
    val pilotIndex: Int,
    val worldSeeds: List<Long>,
    val branches: Int,
    val seconds: Double,
)

/**
 * @property type the enumerated action type (`PassPriority`, `CastSpell`, ...), as prefs records name it.
 * @property f the quiet state's [RawBoardFeatures] from the learner's side.
 * @property base the score the Strategist compared (adjusted, bonus included; the discounted
 *   threshold for the pass). The action-only correction is added to this.
 * @property raw the candidate evaluator's leaf score before adjustments.
 * @property won set when the one-ply simulation already ended the game.
 * @property roll the outcome label in `RollResult`'s shape: `outcomes` is `W`/`L`/`U` per world, in
 *   [ValueLabel.worldSeeds] order, paired across candidates by index.
 */
@Serializable
data class ValueCandidate(
    val type: String,
    val description: String,
    val result: String,
    val f: List<Int>,
    val base: Double,
    val raw: Double,
    val won: Boolean? = null,
    val cards: ValueCards,
    val roll: ValueRoll,
)

/** `PrefCards` (replay), duplicated because the gym does not depend on the replay module. */
@Serializable
data class ValueCards(
    val act: String? = null,
    val myBattlefield: List<String> = emptyList(),
    val opponentBattlefield: List<String> = emptyList(),
    val myHand: List<String> = emptyList(),
    val myGraveyard: List<String> = emptyList(),
    val opponentGraveyard: List<String> = emptyList(),
)

/**
 * @property illegal branches where the candidate's action was refused in that world; scored `U`.
 * @property truncated branches a gym limit stopped undecided; scored `U`.
 */
@Serializable
data class ValueRoll(
    val n: Int,
    val wins: Int,
    val undecided: Int,
    val illegal: Int,
    val truncated: Int,
    val turns: Int,
    val outcomes: String,
)
