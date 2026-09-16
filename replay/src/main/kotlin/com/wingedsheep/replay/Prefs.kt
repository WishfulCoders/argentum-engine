package com.wingedsheep.replay

import com.wingedsheep.ai.engine.AiProfile
import com.wingedsheep.ai.engine.DecisionResponder
import com.wingedsheep.ai.engine.GameSimulator
import com.wingedsheep.ai.engine.SimulationResult
import com.wingedsheep.ai.engine.TargetSelection
import com.wingedsheep.ai.engine.evaluation.BoardEvaluator
import com.wingedsheep.ai.engine.evaluation.EvalWeights
import com.wingedsheep.ai.engine.evaluation.RawBoardFeatures
import com.wingedsheep.ai.engine.knowledge.IntentCatalog
import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.DeclareAttackers
import com.wingedsheep.engine.core.DeclareBlockers
import com.wingedsheep.engine.core.GameAction
import com.wingedsheep.engine.core.PlayLand
import com.wingedsheep.engine.core.SubmitDecision
import com.wingedsheep.engine.legalactions.EnumerationMode
import com.wingedsheep.engine.legalactions.LegalActionEnumerator
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.model.EntityId
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject

/** The feature order of [PrefCandidate.f], and the profile [PrefCandidate.base] scores with: the first line of a prefs file. */
@Serializable
data class PrefHeader(
    val features: List<String>,
    val baseProfile: String = AiProfile.CURRENT.id,
    /** Set when the candidates carry rollout labels ([PrefCandidate.roll], mtg-draft-ai `docs/36`). */
    val roll: RollHeader? = null,
)

/** How a rollout-labelled prefs file was produced; everything `docs/36` §2 pre-registers. */
@Serializable
data class RollHeader(
    /** The pilot in the user's seat, whose evaluator the targets are for. */
    val acting: String,
    /** Opponent pilots, cycled by rollout index; `winsByOpponent` follows this order. */
    val opponents: List<String>,
    val rollouts: Int,
    val seed: Long,
    /** Whether both libraries were reshuffled per rollout (the no-lookahead discipline). */
    val shuffle: Boolean,
    val maxTurnsPerSeat: Int,
    /** Candidates rolled out per choice at most; the rest are recorded without a label. */
    val maxCandidates: Int,
)

/** One of the user's priority choices on a rebuilt line: the move they made and the alternatives. */
@Serializable
data class PrefRoot(
    val gameId: String,
    val set: String,
    val halfTurn: Int,
    /** Index of the move in the game's line. */
    val step: Int,
    /** How the harness chose the user's move ([Reconstructor.PLAN] or [Reconstructor.SEARCH]). */
    val how: String,
    /** Legal alternatives that could not be simulated to a quiet state, and so are missing. */
    val dropped: Int,
    /** The user's move first, then every other legal non-mana action. */
    val cands: List<PrefCandidate>,
)

@Serializable
data class PrefCandidate(
    val type: String,
    /** `T` when the simulation reached a quiet state, `D` when it stopped at a decision it could not answer. */
    val result: String,
    /** [RawBoardFeatures] of the quiet state from the user's side, in [PrefHeader.features] order. */
    val f: List<Int>,
    /** The base profile's evaluator on the quiet state ([PrefHeader.baseProfile]): what that AI would compare. */
    val base: Double,
    /** Set when the candidate ends the game: whether the user won. */
    val won: Boolean? = null,
    /** Rollout label ([RolloutWriter]): null when rollouts are off, or this candidate was not sampled. */
    val roll: RollResult? = null,
)

/**
 * Preference records for fitting an evaluator to human play (the gameplay pilot, `docs/28`).
 *
 * At each user priority action on an accepted line (decisions, attacks and blocks excluded), every
 * legal alternative is simulated to its quiet state as the AI's own one-ply search does it — with
 * [TargetSelection.fillHeuristically]'s targets and [base]'s decision responder — and read as
 * [RawBoardFeatures]. One per thread, like [LineWriter].
 */
class PreferenceWriter(
    registry: CardRegistry,
    base: AiProfile = AiProfile.CURRENT,
    /** Set to label each candidate by rollout as well as by the human's choice (mtg-draft-ai `docs/36`). */
    private val rollouts: RolloutWriter? = null,
    /** With [rollouts], how many of a choice's candidates to roll out — the human's move plus a random sample. */
    private val maxCandidates: Int = Int.MAX_VALUE,
) {
    private val enumerator = LegalActionEnumerator.create(registry)
    private val simulator = GameSimulator(registry)
    private val intents = IntentCatalog.of(registry)
    private val baseline: BoardEvaluator = base.let { p ->
        EvalWeights.resolveEvaluator(
            p.evalWeightsId, IntentCatalog.NONE, p.landDropIsNotCardLoss, p.sequenceLandsByUsableMana,
            p.discountedRaceClock, p.creatureValuation, p.priceLandsInHandAsMana,
        )
    }

    init {
        val responder = DecisionResponder(simulator, baseline)
        simulator.decisionResolver = { state, decision -> responder.respond(state, decision, decision.playerId) }
    }

    fun roots(spec: GameSpec, seats: Seats, steps: List<Move>): List<PrefRoot> =
        steps.withIndex().mapNotNull { (i, move) -> root(spec, seats.user, i, move) }

    private fun root(spec: GameSpec, user: EntityId, index: Int, move: Move): PrefRoot? {
        val chosen = move.action
        if (chosen.playerId != user || move.how == Reconstructor.AI) return null
        if (chosen is SubmitDecision || chosen is DeclareAttackers || chosen is DeclareBlockers) return null
        val state = move.before
        if (state.pendingDecision != null) return null
        val legal = enumerator.enumerate(state, user, EnumerationMode.ACTIONS_ONLY)
            .filter { !it.isManaAbility && it.affordable }
        if (legal.size < 2) return null
        val cands = mutableListOf(candidate(state, chosen, chosen::class.simpleName ?: "?", user) ?: return null)
        var dropped = 0
        for (option in legal) {
            if (sameChoice(option.action, chosen)) continue
            val concrete = runCatching {
                TargetSelection.fillHeuristically(state, option, user, fillPartialRequirements = true, intents = intents)
            }.getOrNull()
            val c = concrete?.let { candidate(state, it, option.actionType, user) }
            if (c == null) dropped++ else cands += c
        }
        if (cands.size < 2) return null
        val labelled = rollouts?.let { label(it, spec, state, user, index, cands) } ?: cands.map { it.pref }
        return PrefRoot(spec.gameId, spec.set, move.halfTurn, index, move.how, dropped, labelled)
    }

    /**
     * Rolls [cands] out and returns them with their labels. The human's move (first) is always rolled out;
     * with a [maxCandidates] cap the rest are a uniform sample, so a capped choice is still an unbiased
     * comparison — taking the best-scoring alternatives instead would label exactly the candidates the
     * current evaluator already likes. Every candidate of the choice shares one seed (common random numbers).
     */
    private fun label(
        roller: RolloutWriter, spec: GameSpec, state: GameState, user: EntityId, index: Int, cands: List<Cand>,
    ): List<PrefCandidate> {
        val choiceSeed = RolloutWriter.choiceSeed(roller.seed, spec.gameId, index)
        val decklists = roller.decklists(state)
        val rest = (1 until cands.size).toMutableList()
        if (cands.size > maxCandidates) {
            val rng = java.util.Random(choiceSeed)
            while (rest.size > maxCandidates - 1) rest.removeAt(rng.nextInt(rest.size))
        }
        val picked = rest.toSet() + 0
        return cands.mapIndexed { i, c ->
            if (i !in picked) c.pref else c.pref.copy(roll = roller.play(c.quiet, user, decklists, choiceSeed))
        }
    }

    /** One candidate: the record, and the quiet state a rollout would continue from. */
    private data class Cand(val pref: PrefCandidate, val quiet: GameState)

    private fun candidate(state: GameState, action: GameAction, type: String, user: EntityId): Cand? {
        val result = runCatching { simulator.simulate(state, action) }.getOrNull() ?: return null
        val (quiet, code) = when (result) {
            is SimulationResult.Terminal -> result.state to "T"
            is SimulationResult.NeedsDecision -> result.state to "D"
            else -> return null
        }
        val projected = quiet.projectedState
        val features = lineJson.encodeToJsonElement(
            RawBoardFeatures.serializer(), RawBoardFeatures.extract(quiet, projected, user, intents),
        ).jsonObject
        return Cand(
            PrefCandidate(
                type, code, FEATURES.map { (features[it] as JsonPrimitive).int },
                baseline.evaluate(quiet, projected, user).coerceIn(-1e9, 1e9),
                won = if (quiet.gameOver) quiet.winnerId == user else null,
            ),
            quiet,
        )
    }

    /** Whether the enumerated [template] is the move [chosen] makes (before its targets and payment). */
    private fun sameChoice(template: GameAction, chosen: GameAction): Boolean = when {
        template is CastSpell && chosen is CastSpell -> template.cardId == chosen.cardId
        template is ActivateAbility && chosen is ActivateAbility ->
            template.sourceId == chosen.sourceId && template.abilityId == chosen.abilityId
        template is PlayLand && chosen is PlayLand -> template.cardId == chosen.cardId
        else -> template == chosen
    }

    companion object {
        val FEATURES: List<String> = RawBoardFeatures.names.toList()

        /** `-Dreplay.prefsProfile`: `current` (the default) or `raceclock` ([AiProfile.CURRENT_RACECLOCK]). */
        fun baseProfile(name: String?): AiProfile = when (name ?: "current") {
            "current" -> AiProfile.CURRENT
            "raceclock" -> AiProfile.CURRENT_RACECLOCK
            else -> error("unknown prefs profile $name")
        }
    }
}
