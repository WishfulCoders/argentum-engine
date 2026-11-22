package com.wingedsheep.engine.view

import com.wingedsheep.sdk.model.EntityId

/**
 * Computes a [StateDelta] representing the difference between two [ClientGameState] snapshots.
 *
 * Cards are diffed at the entity level — a changed card is sent in full (no per-field card diffs).
 * Zones are diffed by zoneId. Scalars are compared individually.
 * Players and legal actions are always included in the delta (small and nearly always change).
 */
object StateDiffCalculator {

    /**
     * Compute the delta from [previous] to [current].
     *
     * @param previous The last ClientGameState sent to this player
     * @param current The new ClientGameState to send
     * @return A StateDelta containing only the changes
     */
    fun computeDelta(previous: ClientGameState, current: ClientGameState): StateDelta {
        // --- Cards diff ---
        val prevCards = previous.cards
        val currCards = current.cards

        val addedCards = mutableMapOf<EntityId, ClientCard>()
        val updatedCards = mutableMapOf<EntityId, ClientCard>()
        val removedCardIds = mutableListOf<EntityId>()

        // Find added and updated cards
        for ((id, card) in currCards) {
            val prevCard = prevCards[id]
            if (prevCard == null) {
                addedCards[id] = card
            } else if (prevCard != card) {
                updatedCards[id] = card
            }
        }

        // Find removed cards
        for (id in prevCards.keys) {
            if (id !in currCards) {
                removedCardIds.add(id)
            }
        }

        // --- Zones diff ---
        val prevZoneMap = previous.zones.associateBy { it.zoneId }
        val currentZoneIds = current.zones.mapTo(HashSet()) { it.zoneId }
        val updatedZones = current.zones.filter { zone ->
            val prevZone = prevZoneMap[zone.zoneId]
            prevZone == null || prevZone != zone
        } + previous.zones
            // A zone the new state no longer has — a subgame's fresh zones (CR 729.2) replacing the
            // main game's, and back — is sent empty, so the client doesn't keep showing its cards.
            .filter { it.zoneId !in currentZoneIds && (it.size > 0 || it.cardIds.isNotEmpty()) }
            .map { it.copy(cardIds = emptyList(), size = 0, positions = it.positions?.let { emptyList() }) }

        // --- Scalars diff ---
        val phaseDelta = if (current.currentPhase != previous.currentPhase) current.currentPhase else null
        val stepDelta = if (current.currentStep != previous.currentStep) current.currentStep else null
        val activePlayerDelta = if (current.activePlayerId != previous.activePlayerId) current.activePlayerId else null
        val priorityPlayerDelta = if (current.priorityPlayerId != previous.priorityPlayerId) current.priorityPlayerId else null
        val turnNumberDelta = if (current.turnNumber != previous.turnNumber) current.turnNumber else null
        val roundNumberDelta = if (current.roundNumber != previous.roundNumber) current.roundNumber else null
        val isGameOverDelta = if (current.isGameOver != previous.isGameOver) current.isGameOver else null
        val winnerIdDelta = if (current.winnerId != previous.winnerId) current.winnerId else null
        val dayNightDelta = if (current.dayNight != previous.dayNight) current.dayNight else null
        // Only a subgame starting or ending (CR 729) takes the game back to neither day nor night.
        val dayNightCleared = previous.dayNight != null && current.dayNight == null
        val subgameChanged = current.subgame != previous.subgame
        val voidActiveDelta = if (current.voidActive != previous.voidActive) current.voidActive else null

        // --- Combat diff ---
        val combatChanged = current.combat != previous.combat
        val combatCleared = combatChanged && current.combat == null
        val combatDelta = if (combatChanged && current.combat != null) current.combat else null

        // --- Game log diff (append-only) ---
        val prevLogSize = previous.gameLog.size
        val currentLog = current.gameLog
        val newLogEntries = if (currentLog.size > prevLogSize) {
            currentLog.subList(prevLogSize, currentLog.size)
        } else {
            null
        }

        // --- Deck diff (whole list; changes only when a card's remaining count moves) ---
        val deckDelta = if (current.deck != previous.deck) current.deck else null
        val activeYieldsDelta = if (current.activeYields != previous.activeYields) current.activeYields else null

        return StateDelta(
            addedCards = addedCards.ifEmpty { null },
            removedCardIds = removedCardIds.ifEmpty { null },
            updatedCards = updatedCards.ifEmpty { null },
            updatedZones = updatedZones.ifEmpty { null },
            players = current.players,
            currentPhase = phaseDelta,
            currentStep = stepDelta,
            activePlayerId = activePlayerDelta,
            priorityPlayerId = priorityPlayerDelta,
            turnNumber = turnNumberDelta,
            roundNumber = roundNumberDelta,
            isGameOver = isGameOverDelta,
            winnerId = winnerIdDelta,
            dayNight = dayNightDelta,
            dayNightCleared = if (dayNightCleared) true else null,
            voidActive = voidActiveDelta,
            subgame = if (subgameChanged) current.subgame else null,
            subgameCleared = if (subgameChanged && current.subgame == null) true else null,
            combat = combatDelta,
            combatCleared = if (combatCleared) true else null,
            newLogEntries = newLogEntries,
            youAreHijacking = current.youAreHijacking,
            youAreHijackedBy = current.youAreHijackedBy,
            hotseat = current.hotseat,
            deck = deckDelta,
            activeYields = activeYieldsDelta,
        )
    }
}
