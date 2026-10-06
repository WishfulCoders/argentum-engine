package com.wingedsheep.sdk.scripting

import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.conditions.Condition
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Represents timing and conditional restrictions on when an activated ability can be used.
 * Used for abilities like "Activate only during your turn, before attackers are declared."
 *
 * The engine enforces these restrictions during legality checks.
 */
@Serializable
sealed interface ActivationRestriction {

    /**
     * Restrict activation to only during your turn.
     * Example: "Activate only during your turn."
     */
    @SerialName("OnlyDuringYourTurn")
    @Serializable
    data object OnlyDuringYourTurn : ActivationRestriction

    /**
     * Restrict activation to before a specific step.
     * Example: "Activate only before attackers are declared."
     */
    @SerialName("BeforeStep")
    @Serializable
    data class BeforeStep(val step: Step) : ActivationRestriction

    /**
     * Restrict activation to during a specific phase.
     * Example: "Activate only during combat."
     */
    @SerialName("DuringPhase")
    @Serializable
    data class DuringPhase(val phase: Phase) : ActivationRestriction

    /**
     * Restrict activation to during a specific step.
     * Example: "Activate only during the declare blockers step."
     */
    @SerialName("DuringStep")
    @Serializable
    data class DuringStep(val step: Step) : ActivationRestriction

    /**
     * Restrict activation based on a game condition.
     * Example: "Activate only if you control no creatures."
     */
    @SerialName("ActivationOnlyIfCondition")
    @Serializable
    data class OnlyIfCondition(val condition: Condition) : ActivationRestriction

    /**
     * Restrict activation to once per turn.
     * Example: "Activate only once each turn."
     */
    @SerialName("OncePerTurn")
    @Serializable
    data object OncePerTurn : ActivationRestriction

    /**
     * Restrict activation to at most [count] times per turn.
     * Example: Phyrexian Battleflies: "Activate no more than twice each turn." ([count] = 2).
     */
    @SerialName("MaxPerTurn")
    @Serializable
    data class MaxPerTurn(val count: Int) : ActivationRestriction

    /**
     * Restrict activation to only once ever (for the lifetime of the permanent).
     * Example: "Activate only once."
     */
    @SerialName("Once")
    @Serializable
    data object Once : ActivationRestriction

    /**
     * Any player may activate this ability, not just the controller.
     * Example: Lethal Vapors: "{0}: Destroy Lethal Vapors. You skip your next turn. Any player may activate this ability."
     */
    @SerialName("AnyPlayerMay")
    @Serializable
    data object AnyPlayerMay : ActivationRestriction

    /**
     * Restrict activation to when the source has been under the activating player's control
     * continuously since the beginning of their most recent turn — i.e. the "summoning sickness"
     * condition (CR 302.6) applied to a noncreature permanent. Reuses the engine's
     * summoning-sickness tracking (set on entry and on any control change, cleared at the
     * controller's untap), so a control change since their last turn re-imposes it — unlike
     * `SourceEnteredThisTurn`, which ignores control changes.
     *
     * Example: Rocket Launcher — "Activate only if you've controlled this artifact continuously
     * since the beginning of your most recent turn."
     */
    @SerialName("ControlledSinceYourMostRecentTurn")
    @Serializable
    data object ControlledSinceYourMostRecentTurn : ActivationRestriction

    /**
     * "Activate only as an instant" (CR 602.5e): the player must follow the timing rules for casting
     * an instant — i.e. hold priority (CR 304.5) — though the ability isn't an instant.
     *
     * Only meaningful on a **mana ability**. An ordinary activated ability can only ever be activated
     * with priority anyway, so on one this restriction changes nothing (it is the printed reminder
     * on Witch Engine). A mana ability, though, may normally also be activated without priority —
     * mid-cast while paying a cost, or whenever a rule or effect asks for a mana payment
     * (CR 605.3a) — and this restriction takes exactly those windows away. CR 605.1 is explicit that
     * the ability stays a mana ability "regardless of … what timing restrictions (such as 'Activate
     * only as an instant') [it] may have": it still doesn't use the stack and resolves immediately
     * (CR 605.3b), it just can't be activated while a spell is being cast or a cost paid.
     *
     * It lives here rather than in [TimingRule] because a mana ability's [TimingRule] is already
     * [TimingRule.ManaAbility] by construction (the DSL derives it from `manaAbility = true`); the
     * 602.5e clause is an extra restriction layered on top, not a different timing class.
     *
     * Example: Lion's Eye Diamond — "Discard your hand, Sacrifice this artifact: Add three mana of any
     * one color. Activate only as an instant." (2004-10-04 ruling: "it can only be activated at times
     * when you can cast an instant.")
     */
    @SerialName("OnlyAsInstant")
    @Serializable
    data object OnlyAsInstant : ActivationRestriction

    /**
     * Composite restriction requiring multiple conditions.
     * Example: "Activate only during your turn, before attackers are declared."
     */
    @SerialName("ActivationAll")
    @Serializable
    data class All(val restrictions: List<ActivationRestriction>) : ActivationRestriction {
        constructor(vararg restrictions: ActivationRestriction) : this(restrictions.toList())
    }
}
