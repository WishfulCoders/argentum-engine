package com.wingedsheep.ai.engine.evaluation

import com.wingedsheep.engine.mechanics.layers.ProjectedState
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.sdk.model.EntityId
import kotlinx.serialization.Serializable

/**
 * A fitted card -> value table, in the units of the evaluator it is added to (mtg-draft-ai `docs/40`).
 *
 * [RawBoardFeatures] counts permanents without ever knowing what they are (`docs/33` §10), so two
 * candidate actions that play different cards can share a feature vector and no function of those
 * features can rank them. This term ends that blindness the cheapest way the hot path allows: a name
 * lookup and an addition per card in the zones the acting side may see, added to
 * [RawEvaluationWeights.toCorrection]'s score.
 *
 *     sum over zones  sum over cards in that zone  +/- value(card, zone)
 *
 * with `+` for the acting side's zones and `-` for the opponent's, which is exactly what
 * `fit_cards.py`'s `zone_counts` fits: battlefield and graveyard as differences, the acting side's
 * hand as a level. **The opponent's hand and both libraries are absent by construction** — the fit
 * was never shown them (`PrefCards`), so nothing here can be played dishonestly.
 *
 * The names are the recorder's: a permanent's [CardComponent] name, and `token:<name>` for a token.
 */
@Serializable
data class CardValueTable(
    /** Must be [ZONES]: the fit writes them in that order and a row is read positionally. */
    val zones: List<String>,
    /** Card name -> one value per zone. A card absent here is worth 0, which most cards are. */
    val values: Map<String, List<Double>>,
) {
    /** Per zone, name -> value: the hot path does one lookup per card and never indexes a list. */
    private val byZone: List<Map<String, Double>> by lazy {
        zones.indices.map { z ->
            values.mapNotNull { (name, row) -> row.getOrNull(z)?.let { name to it } }
                .filter { it.second != 0.0 }
                .toMap()
        }
    }

    fun isValid(): Boolean =
        zones == ZONES &&
            values.values.all { row -> row.size == zones.size && row.all(Double::isFinite) }

    /**
     * The card term of [playerId]'s score of [state], from that side's point of view.
     *
     * Battlefield membership is read from [projected], as [RawBoardFeatures] and the recorder both
     * read it, so a permanent that changed control or type is counted where it actually is.
     */
    fun score(state: GameState, projected: ProjectedState, playerId: EntityId): Double {
        if (byZone.all(Map<String, Double>::isEmpty)) return 0.0
        val opponent = state.getOpponents(playerId).firstOrNull()
        var total = 0.0
        val battlefield = byZone[BATTLEFIELD]
        if (battlefield.isNotEmpty()) {
            for (id in projected.getBattlefieldControlledBy(playerId)) total += battlefield.value(state, id)
            if (opponent != null) {
                for (id in projected.getBattlefieldControlledBy(opponent)) total -= battlefield.value(state, id)
            }
        }
        val graveyard = byZone[GRAVEYARD]
        if (graveyard.isNotEmpty()) {
            for (id in state.getGraveyard(playerId)) total += graveyard.value(state, id)
            if (opponent != null) {
                for (id in state.getGraveyard(opponent)) total -= graveyard.value(state, id)
            }
        }
        val hand = byZone[HAND]
        if (hand.isNotEmpty()) {
            for (id in state.getHand(playerId)) total += hand.value(state, id)
        }
        return total
    }

    private fun Map<String, Double>.value(state: GameState, id: EntityId): Double =
        name(state, id)?.let { this[it] } ?: 0.0

    companion object {
        const val BATTLEFIELD = 0
        const val GRAVEYARD = 1
        const val HAND = 2

        /** `fit_cards.py`'s `ZONES`, in its order. */
        val ZONES: List<String> = listOf("battlefield", "graveyard", "hand")

        /** The recorder's naming (`PreferenceWriter.name`), which the fitted keys are in. */
        fun name(state: GameState, id: EntityId): String? {
            val entity = state.getEntity(id) ?: return null
            val card = entity.get<CardComponent>() ?: return null
            return if (entity.has<TokenComponent>()) "token:${card.name}" else card.name
        }
    }
}
