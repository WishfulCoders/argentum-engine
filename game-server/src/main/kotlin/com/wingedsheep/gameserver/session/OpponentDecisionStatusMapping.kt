package com.wingedsheep.gameserver.session

import com.wingedsheep.engine.core.PendingDecision
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.view.DecisionEnricher
import com.wingedsheep.gameserver.protocol.ServerMessage
import com.wingedsheep.sdk.model.EntityId

/** The engine's projection of another seat's pending decision, as the wire message [viewerId] is sent. */
fun DecisionEnricher.createOpponentDecisionStatus(
    decision: PendingDecision,
    state: GameState,
    viewerId: EntityId,
): ServerMessage.OpponentDecisionStatus = opponentDecisionStatus(decision, state, viewerId).let {
    ServerMessage.OpponentDecisionStatus(
        playerId = it.playerId.value,
        decisionType = it.decisionType,
        displayText = it.displayText,
        sourceName = it.sourceName,
        sourceId = it.sourceId?.value,
        sourceImageUri = it.sourceImageUri,
    )
}
