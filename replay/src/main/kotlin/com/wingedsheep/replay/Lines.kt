package com.wingedsheep.replay

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.CrewVehicle
import com.wingedsheep.engine.core.DeclareAttackers
import com.wingedsheep.engine.core.DeclareBlockers
import com.wingedsheep.engine.core.GameAction
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.core.SaddleMount
import com.wingedsheep.engine.core.SubmitDecision
import com.wingedsheep.engine.legalactions.EnumerationMode
import com.wingedsheep.engine.legalactions.LegalAction
import com.wingedsheep.engine.legalactions.LegalActionEnumerator
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.gym.contract.ActionParameterizer
import com.wingedsheep.gym.contract.ActionParams
import com.wingedsheep.gym.contract.ObservationBuilder
import com.wingedsheep.gym.contract.TrainingObservation
import com.wingedsheep.sdk.model.EntityId
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

/** One action on a rebuilt game's line: the state it was taken in, and how it was chosen. */
class Move(
    val before: GameState,
    val action: GameAction,
    val halfTurn: Int,
    /** [Reconstructor.PLAN], [Reconstructor.SEARCH] or [Reconstructor.AI]. */
    val how: String,
    val prev: Move?,
) {
    fun toList(): List<Move> = generateSequence(this) { it.prev }.toList().asReversed()
}

/**
 * A rebuilt game's accepted line, one record per action, in the gym's terms: the observation the
 * acting player had ([ObservationBuilder], hidden cards masked), the gym action id and
 * [ActionParams] that make the same move, and the engine action itself.
 */
@Serializable
data class GameLine(
    val gameId: String,
    val set: String,
    /** The harness result: `reproduced`, or `failed` with the line up to [through]. */
    val status: String,
    val halfTurns: Int,
    /** Half-turns the line covers, each matching its recorded snapshot. */
    val through: Int,
    val user: EntityId,
    val oppo: EntityId,
    val steps: List<LineStep>,
    /** Half-turns before [through] with no steps: skipped by a resync (C2), the state rewritten to their snapshot. */
    val gaps: List<Int> = emptyList(),
)

@Serializable
data class LineStep(
    val halfTurn: Int,
    /** `user` (the 17Lands player) or `oppo` (their opponent, whose hidden cards are partly invented). */
    val side: String,
    /** `plan`: the action is one the record names (a land, cast, activation, attack or block);
     *  `search`: the search chose it, consistently with the snapshots; `ai`: the AI's responder did. */
    val how: String,
    /** `action` (a priority action), or `decision` (an answer to a pending decision). */
    val kind: String,
    /** Options the gym offers at this step (mana abilities excluded); 1 means the move was forced. */
    val choices: Int,
    /** Index into [obs]'s legal actions (the gym action id); null when no option matches. */
    val actionId: Int? = null,
    /** Choices completing an action template: targets, X, attackers, blockers. */
    val params: ActionParams? = null,
    /** True when [actionId] and [params] make exactly [engine] (no payment the gym cannot express). */
    val exact: Boolean = false,
    /** The engine's [GameAction]. */
    val engine: JsonElement,
    /**
     * The acting player's observation, wherever the player chose something: more than one option,
     * a declaration with candidates, a template's targets or X, or a structured decision.
     */
    val obs: TrainingObservation? = null,
)

/** Turns a [Move] line into [GameLine] records; one per thread, like [Reconstructor]. */
class LineWriter(registry: CardRegistry) {
    private val enumerator = LegalActionEnumerator.create(registry)
    private val observations = ObservationBuilder(registry)

    fun line(spec: GameSpec, result: GameResult, seats: Seats, steps: List<Move>, through: Int, gaps: List<Int> = emptyList()): GameLine =
        GameLine(spec.gameId, spec.set, result.status, spec.halfTurns.size, through, seats.user, seats.oppo,
            steps.map { step(it, seats) }, gaps)

    private fun step(step: Move, seats: Seats): LineStep {
        val s = step.before
        val action = step.action
        val engine = lineJson.encodeToJsonElement(GameAction.serializer(), action)
        val actor = action.playerId
        if (action is SubmitDecision) {
            val built = observations.build(s, actor, emptyList())
            val options = built.registry.decisionResponses
            val id = options.firstOrNull { it.second == action.response }?.first
            val observation = built.observation as TrainingObservation
            val structured = observation.pendingDecision?.requiresStructuredResponse == true
            return LineStep(step.halfTurn, seats.sideOf(actor), step.how, "decision", options.size,
                actionId = id, exact = id != null, engine = engine,
                obs = observation.takeIf { options.size > 1 || structured })
        }
        val legal = enumerator.enumerate(s, actor, EnumerationMode.ACTIONS_ONLY)
        val choices = legal.count { !it.isManaAbility }
        val id = legal.indices.maxByOrNull { match(legal[it].action, action) }?.takeIf { match(legal[it].action, action) > 0 }
        val params = params(action)
        val exact = id != null && runCatching { ActionParameterizer.apply(legal[id].action, params, s) == action }.getOrDefault(false)
        val chose = choices > 1 || id != null && hasChoices(legal[id])
        val obs = if (chose) observations.build(s, actor, legal).observation as TrainingObservation else null
        return LineStep(step.halfTurn, seats.sideOf(actor), step.how, "action", choices,
            actionId = id, params = params.takeUnless { it.isEmpty }, exact = exact, engine = engine, obs = obs)
    }

    /** A single enumerated template can still leave a choice: who attacks or blocks, targets, X. */
    private fun hasChoices(template: LegalAction): Boolean =
        !template.validAttackers.isNullOrEmpty() || !template.validBlockers.isNullOrEmpty() ||
            !template.validTargets.isNullOrEmpty() || !template.targetRequirements.isNullOrEmpty() ||
            template.hasXCost

    /** 2 when [template] is [chosen], 1 when it is [chosen] before its choices were made, else 0. */
    private fun match(template: GameAction, chosen: GameAction): Int = when {
        template == chosen -> 2
        bare(template) == bare(chosen) -> 1
        else -> 0
    }

    /** [action] with the choices a player makes on top of an enumerated template cleared. */
    private fun bare(action: GameAction): GameAction = when (action) {
        is CastSpell -> action.copy(targets = emptyList(), xValue = null, paymentStrategy = PaymentStrategy.AutoPay,
            alternativePayment = null, additionalCostPayment = null, damageDistribution = null)
        is ActivateAbility -> action.copy(targets = emptyList(), xValue = null, paymentStrategy = PaymentStrategy.AutoPay,
            alternativePayment = null, costPayment = null, manaColorChoice = null, damageDistribution = null)
        is DeclareAttackers -> action.copy(attackers = emptyMap())
        is DeclareBlockers -> action.copy(blockers = emptyMap())
        is CrewVehicle -> action.copy(crewCreatures = emptyList())
        is SaddleMount -> action.copy(saddleCreatures = emptyList())
        else -> action
    }

    private fun params(action: GameAction): ActionParams = when (action) {
        is DeclareAttackers -> ActionParams(attackers = action.attackers)
        is DeclareBlockers -> ActionParams(blockers = action.blockers)
        is CastSpell -> ActionParams(targets = action.targets.map(::targetId), xValue = action.xValue)
        is ActivateAbility -> ActionParams(targets = action.targets.map(::targetId), xValue = action.xValue)
        else -> ActionParams.EMPTY
    }

    private fun targetId(target: ChosenTarget): EntityId = when (target) {
        is ChosenTarget.Player -> target.playerId
        is ChosenTarget.Permanent -> target.entityId
        is ChosenTarget.Card -> target.cardId
        is ChosenTarget.Spell -> target.spellEntityId
    }
}

/** The gym's wire format ([ObservationBuilder]'s field names), without defaults, to keep lines small. */
val lineJson = Json {
    encodeDefaults = false
    explicitNulls = false
}
