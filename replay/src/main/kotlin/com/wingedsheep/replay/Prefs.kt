package com.wingedsheep.replay

import com.wingedsheep.ai.engine.AiProfile
import com.wingedsheep.ai.engine.DecisionResponder
import com.wingedsheep.ai.engine.GameSimulator
import com.wingedsheep.ai.engine.SimulationResult
import com.wingedsheep.ai.engine.TargetSelection
import com.wingedsheep.ai.engine.evaluation.BoardEvaluator
import com.wingedsheep.ai.engine.evaluation.CardValueTable
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
import com.wingedsheep.engine.mechanics.layers.ProjectedState
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
    /** Where the opponent's unseen cards came from ([OppoDeckMode]), lower-case. */
    val oppoDeck: String = "stub",
    /** With the donor mode, the donor spec files and how many whole decks they held. */
    val donors: List<String> = emptyList(),
    val donorDecks: Int = 0,
    /** Permanents on the battlefield past which a rollout stops undecided; null = no cap. */
    val maxPermanents: Int? = null,
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

/**
 * The card identities of a candidate's quiet state, by zone, from the user's side (mtg-draft-ai `docs/40`).
 *
 * `RawBoardFeatures` counts permanents without knowing what they are, which is `docs/33` §10's blindness:
 * two candidates that play different cards can share a feature vector. These lists are what a card-aware
 * evaluator would read, and only from zones that side can see — the opponent's hand and both libraries are
 * deliberately absent, so a model fit on them can be played honestly. Tokens are named `token:<name>`.
 */
@Serializable
data class PrefCards(
    /** The card the candidate's action casts, plays or activates; null for a pass. */
    val act: String? = null,
    val myBattlefield: List<String> = emptyList(),
    val opponentBattlefield: List<String> = emptyList(),
    val myHand: List<String> = emptyList(),
    val myGraveyard: List<String> = emptyList(),
    val opponentGraveyard: List<String> = emptyList(),
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
    /** Card identities of the quiet state ([PrefCards]): null when `-Dreplay.prefCards=false`. */
    val cards: PrefCards? = null,
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
    /** Whether each candidate also records the card identities of its quiet state ([PrefCards]). */
    private val emitCards: Boolean = true,
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
            if (i !in picked) c.pref else c.pref.copy(roll = roller.play(c.quiet, user, decklists, choiceSeed, spec))
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
                cards = if (emitCards) cards(quiet, projected, user, state, action) else null,
            ),
            quiet,
        )
    }

    /** [PrefCards] of the quiet state; [before] is the state the action was taken in, where its card is known. */
    private fun cards(
        quiet: GameState, projected: ProjectedState, user: EntityId, before: GameState, action: GameAction,
    ): PrefCards {
        val opponent = quiet.getOpponents(user).firstOrNull()
        return PrefCards(
            act = when (action) {
                is CastSpell -> name(before, action.cardId)
                is PlayLand -> name(before, action.cardId)
                is ActivateAbility -> name(before, action.sourceId)
                else -> null
            },
            myBattlefield = names(quiet, projected.getBattlefieldControlledBy(user)),
            opponentBattlefield = opponent?.let { names(quiet, projected.getBattlefieldControlledBy(it)) }.orEmpty(),
            myHand = names(quiet, quiet.getHand(user)),
            myGraveyard = names(quiet, quiet.getGraveyard(user)),
            opponentGraveyard = opponent?.let { names(quiet, quiet.getGraveyard(it)) }.orEmpty(),
        )
    }

    /** Sorted, because a zone is a bag of cards to the model and a canonical order keeps the files diffable. */
    private fun names(state: GameState, ids: Collection<EntityId>): List<String> =
        ids.mapNotNull { name(state, it) }.sorted()

    /** The evaluator's own naming ([CardValueTable.name]), so a fitted table keys on these records. */
    private fun name(state: GameState, id: EntityId): String? = CardValueTable.name(state, id)

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
