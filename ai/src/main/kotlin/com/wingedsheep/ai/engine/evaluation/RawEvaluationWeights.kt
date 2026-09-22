package com.wingedsheep.ai.engine.evaluation

import com.wingedsheep.ai.engine.knowledge.IntentCatalog
import com.wingedsheep.engine.mechanics.layers.ProjectedState
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.model.EntityId
import kotlinx.serialization.Serializable

/** A fitted linear model over the unaggregated Phase 9 position facts. */
@Serializable
data class RawEvaluationWeights(
    val intercept: Double,
    val weights: Map<String, Double>,
    val winProbabilityScale: Double = 1.0,
    /**
     * An optional card -> value term added to the linear one (`docs/40`; schema 2 of
     * [com.wingedsheep.ai.training.ApprenticeArtifact]). Absent in every fit before it, and 0 for
     * every card the fit never saw move, so a model without one is unchanged.
     */
    val cards: CardValueTable? = null,
) {
    fun isValid(): Boolean =
        intercept.isFinite() &&
            winProbabilityScale.isFinite() && winProbabilityScale > 0.0 &&
            weights.keys == RawBoardFeatures.names &&
            weights.values.all(Double::isFinite) &&
            (cards == null || cards.isValid())

    fun evaluate(features: RawBoardFeatures): Double = intercept + features.weightedSum(weights)

    /** The card term on this position, or 0.0 when no table is installed. */
    fun cardTerm(state: GameState, projected: ProjectedState, playerId: EntityId): Double =
        cards?.score(state, projected, playerId) ?: 0.0

    fun toEvaluator(intents: IntentCatalog): BoardEvaluator = RawBoardEvaluator(this, intents)

    /**
     * This model as a term added to another evaluator's score (the gameplay pilot's correction,
     * mtg-draft-ai `docs/28` §5). 0 once the game is over, which the other evaluator scores.
     */
    fun toCorrection(intents: IntentCatalog): BoardEvaluator = BoardEvaluator { state, projected, playerId ->
        if (terminalScore(state, playerId) != null) 0.0
        else evaluate(RawBoardFeatures.extract(state, projected, playerId, intents)) +
            cardTerm(state, projected, playerId)
    }
}

private class RawBoardEvaluator(
    private val fitted: RawEvaluationWeights,
    private val intents: IntentCatalog,
) : BoardEvaluator {
    override fun evaluate(state: GameState, projected: ProjectedState, playerId: EntityId): Double {
        terminalScore(state, playerId)?.let { return it }
        return fitted.evaluate(RawBoardFeatures.extract(state, projected, playerId, intents)) +
            fitted.cardTerm(state, projected, playerId)
    }
}

/** Shared terminal contract for composite and fitted evaluators. */
internal fun terminalScore(state: GameState, playerId: EntityId): Double? = when {
    state.gameOver -> when {
        state.winnerId == null -> 0.0
        state.winnerId in state.teamOf(playerId) -> Double.MAX_VALUE / 2
        else -> -(Double.MAX_VALUE / 2)
    }
    state.teamActivePlayers(playerId).isEmpty() -> -(Double.MAX_VALUE / 2)
    else -> null
}
