package com.wingedsheep.ai.engine.evaluation

import com.wingedsheep.ai.engine.knowledge.CardIntent
import com.wingedsheep.ai.engine.knowledge.IntentCatalog
import com.wingedsheep.ai.engine.knowledge.IntentTag
import com.wingedsheep.ai.engine.knowledge.Speed
import com.wingedsheep.engine.mechanics.layers.ProjectedState
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.model.EntityId
import java.util.concurrent.ConcurrentHashMap

/**
 * "Keep mana up for the answer in your hand" as a board score — mtg-draft-ai `docs/27` §3.
 *
 * [com.wingedsheep.ai.engine.knowledge.HoldPolicy] judges *when to cast* an instant: no counterspell
 * on an empty stack, removal in response or at the end step, a flash creature at the ambush. Nothing
 * judges the other half — a main-phase play that taps below what the held answer costs, so the answer
 * is dead on the opponent's turn. This is that half: [weight] board points for a position, on our own
 * turn, in which the untapped lands still pay for the cheapest instant-speed answer in hand, while the
 * opponent has something for it to answer (a card in hand or a creature).
 *
 * A state score rather than a per-action charge (`docs/27` §1): the pass keeps it, a creature that taps
 * out loses it, a land drop can earn it, and on the opponent's turn it is zero, so spending the mana
 * there — the point of holding it — costs nothing. Colours are not checked, only the land count.
 *
 * With [scaleWithDeck] the weight is multiplied by the deck's count of such answers over [TYPICAL_ANSWERS]
 * (capped at 2): a deck built on instant-speed answers holds up mana, one with a single counterspell
 * mostly does not — the first per-deck conditioning `docs/27` §3 asks for.
 */
class ManaReserve(
    private val intents: IntentCatalog,
    private val weight: Double,
    private val scaleWithDeck: Boolean = false,
) : BoardEvaluator {
    private val deckScale = ConcurrentHashMap<EntityId, Double>()

    override fun evaluate(state: GameState, projected: ProjectedState, playerId: EntityId): Double {
        if (weight == 0.0 || !holdsUpAnswer(state, projected, playerId, intents)) return 0.0
        val scale = if (scaleWithDeck) deckScale.getOrPut(playerId) { answersInDeck(state, playerId) / TYPICAL_ANSWERS } else 1.0
        return weight * scale.coerceAtMost(2.0)
    }

    /** Instant-speed answers among every card the player owns, counted once per game (the deck). */
    private fun answersInDeck(state: GameState, playerId: EntityId): Double {
        // Library, hand, graveyard and exile are the player's own zones; on the battlefield, skip
        // what they control but do not own.
        val owned = state.getLibrary(playerId) + state.getHand(playerId) + state.getGraveyard(playerId) +
            state.getExile(playerId) +
            state.projectedState.getBattlefieldControlledBy(playerId).filter { id ->
                state.getEntity(id)?.get<CardComponent>()?.ownerId.let { it == null || it == playerId }
            }
        return owned.count { id ->
            state.getEntity(id)?.get<CardComponent>()?.let { isAnswer(intents.forName(it.name)) } == true
        }.toDouble()
    }

    companion object {
        /**
         * The position this score rewards, and the one `AiProfile.rolloutsOnlyWhenHolding` searches: our
         * turn, an instant-speed answer in hand that our untapped lands still pay for, and something across
         * the table for it to answer.
         */
        fun holdsUpAnswer(state: GameState, projected: ProjectedState, playerId: EntityId, intents: IntentCatalog): Boolean {
            if (state.gameOver || state.activePlayerId != playerId) return false
            val opponent = state.getOpponents(playerId).firstOrNull() ?: return false
            val opponentHasSomething = state.getHand(opponent).isNotEmpty() ||
                projected.getBattlefieldControlledBy(opponent).any { projected.hasType(it, "CREATURE") }
            if (!opponentHasSomething) return false

            val cheapest = state.getHand(playerId).mapNotNull { id ->
                val card = state.getEntity(id)?.get<CardComponent>() ?: return@mapNotNull null
                card.manaValue.takeIf { isAnswer(intents.forName(card.name)) }
            }.minOrNull() ?: return false

            val untappedLands = projected.getBattlefieldControlledBy(playerId)
                .filter { projected.hasType(it, "LAND") }
                .count { state.getEntity(it)?.has<TappedComponent>() != true }
            return untappedLands >= cheapest
        }

        private fun isAnswer(intent: CardIntent?): Boolean =
            intent != null && intent.speed == Speed.INSTANT &&
                (intent.flashPermanent || intent.tags.any { it in ANSWER_TAGS })

        /** Combat tricks are left out: they are spent on our own turn as often as on theirs. */
        private val ANSWER_TAGS = setOf(IntentTag.COUNTERSPELL, IntentTag.REMOVAL, IntentTag.EXILE_REMOVAL)

        /** Roughly a limited deck's instant-speed answers; the deck scale is 1 at this count. */
        const val TYPICAL_ANSWERS = 4.0
    }
}
