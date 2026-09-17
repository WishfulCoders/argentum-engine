package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.rollout.PlayoutPolicy
import com.wingedsheep.engine.core.GameAction
import com.wingedsheep.engine.core.PassPriority
import com.wingedsheep.engine.legalactions.LegalAction
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.model.GameRng
import java.util.concurrent.atomic.AtomicLong

/**
 * What the opponent does when a candidate of ours is on the stack and priority reaches them.
 *
 * [GameSimulator.resolveToQuietState] passes priority for both players until the stack is empty,
 * which it documents as "simulates both players choosing not to respond". Every candidate is
 * therefore scored against an opponent who never has the answer, and the AI cannot tell a threat
 * that resolves from one that eats a removal spell. `docs/27` §7.4 of `mtg-draft-ai` is the
 * argument for closing that: a static bonus for keeping mana up (`ManaReserve`) failed at every
 * weight and failed *worst* on the blue decks it was written for, because holding mana only pays
 * against a response the simulation refuses to play.
 *
 * Null is the off position, and the constructor default, so a simulator built anywhere else keeps
 * the historical horizon exactly.
 */
fun interface OpponentResponsePolicy {

    /**
     * The opponent's action at this priority window, or null to pass.
     *
     * @param enumerate the legal actions, deferred — a policy that decides from the state alone
     *   never pays for enumeration, which is the same contract [PlayoutPolicy.decide] has and the
     *   reason this is a lambda rather than a list.
     */
    fun respond(
        state: GameState,
        opponentId: EntityId,
        enumerate: () -> List<LegalAction>,
    ): GameAction?
}

/**
 * The shipped policy: one response, chosen by [PlayoutPolicy].
 *
 * [PlayoutPolicy] is the right instrument because of the hard rule at the top of its file —
 * *nothing there may simulate*. This runs inside a simulation, so a policy that simulated would
 * make the Strategist quadratic in exactly the way Phase 7 refused to be. It reads the state and
 * the enumerated actions and nothing else.
 *
 * **Deterministic.** [PlayoutPolicy] is stochastic by design, and takes its randomness as a
 * parameter; here the seed is derived from the position the way
 * `Determinizer.sampleForSearch` derives its own, so the same position always draws the same
 * response. Arena runs are checked for bit-identical replay across machines (`docs/31` §4), and a
 * policy seeded from a mutable counter would break that for no gain — the variance the softmax
 * exists to create belongs to a playout with R samples, not to a single lookahead.
 */
class PlayoutResponsePolicy(private val policy: PlayoutPolicy) : OpponentResponsePolicy {

    override fun respond(
        state: GameState,
        opponentId: EntityId,
        enumerate: () -> List<LegalAction>,
    ): GameAction? {
        val rng = GameRng.seeded(
            state.rng.state xor
                (state.turnNumber.toLong() shl 32) xor
                (state.step.ordinal.toLong() shl 16) xor
                state.stack.size.toLong() xor
                opponentId.value.hashCode().toLong()
        )
        val (action, _) = policy.decide(state, opponentId, rng, enumerate)
        return action.takeUnless { it is PassPriority }
    }

    override fun toString(): String = "playout-response"
}

/**
 * Whether [opponentId] could plausibly answer at all: untapped mana and something to spend it on.
 *
 * The gate exists to keep the cost where the value is. `docs/27` §7.4 sizes the mechanism as
 * candidates × samples × the opponent's options, and the overwhelming majority of priority windows
 * inside a simulation are ones where the opponent is tapped out or empty-handed — enumerating their
 * actions there is pure waste, and this answers from two zone reads.
 *
 * Lands only, deliberately, and it is the same approximation `BoardPresence.landSequencing` makes
 * for the same reason: a real mana-source count would have to read every permanent's abilities on a
 * path that runs once per priority window per candidate. The cost of being wrong is asymmetric and
 * small — a Treasure or a mana creature buys the opponent a response this gate does not offer them,
 * which leaves the AI exactly as blind as it is today rather than making it blinder.
 */
internal fun couldRespond(state: GameState, opponentId: EntityId): Boolean {
    if (state.getZone(opponentId, Zone.HAND).isEmpty()) return false
    return state.projectedState.getBattlefieldControlledBy(opponentId).any { entityId ->
        state.projectedState.hasType(entityId, "LAND") &&
            state.getEntity(entityId)?.has<TappedComponent>() != true
    }
}

/**
 * How often the hook is reached, survives the gate, and actually takes an action.
 *
 * Process-wide and free when unread — three [AtomicLong] increments on a path that already
 * enumerates actions. It exists because the first arena smoke changed 13 of 1,000 games where the
 * `timing` arm changed ~97, and "the mechanism is wrong" and "the mechanism never runs" are very
 * different findings that a win rate cannot tell apart.
 */
object ResponseLookaheadStats {
    val windows = AtomicLong()
    val gatePassed = AtomicLong()
    val responded = AtomicLong()

    fun reset() {
        windows.set(0); gatePassed.set(0); responded.set(0)
    }

    override fun toString(): String {
        val w = windows.get()
        val g = gatePassed.get()
        val r = responded.get()
        fun pct(part: Long, whole: Long) = if (whole == 0L) "-" else "%.1f%%".format(100.0 * part / whole)
        return "opponent-priority windows $w, gate passed $g (${pct(g, w)}), responded $r (${pct(r, g)} of those)"
    }
}
