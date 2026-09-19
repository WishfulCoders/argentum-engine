package com.wingedsheep.gym.service

import com.wingedsheep.ai.engine.AiProfiles
import com.wingedsheep.gym.contract.ActionParams
import com.wingedsheep.sdk.model.EntityId
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Everything needed to spin up a new gym environment.
 *
 * Designed so a JSON payload can drive the HTTP layer in Phase 3 without
 * any translation.
 */
@Serializable
data class EnvConfig(
    val players: List<PlayerSpec>,

    /** Opening-hand size. Standard MTG = 7. */
    val startingHandSize: Int = 7,

    /**
     * Skip mulligan phase. Default `true` — training loops rarely care about
     * mulligans and the extra decision points slow rollouts down.
     */
    val skipMulligans: Boolean = true,

    /** MTGA-style hand smoothing (only useful for realistic play-feel runs). */
    val useHandSmoother: Boolean = false,

    /**
     * Which player goes first (0-indexed). `null` = random — which should be
     * the default for training diversity. Set explicitly only for reproducible
     * scenarios.
     */
    val startingPlayerIndex: Int? = null,

    /**
     * Which player's information-set the default [com.wingedsheep.gym.contract.TrainingObservation]
     * represents. Callers can still override per-request when observing.
     */
    val perspectivePlayerIndex: Int = 0,

    /**
     * If `true`, opponent hand and libraries are revealed — debug only,
     * must never be enabled in production self-play.
     */
    val revealAll: Boolean = false,

    /**
     * RNG seed for shuffles, coin flips and every other "at random" choice. `null` draws fresh
     * entropy, which is the right default for training diversity; the drawn seed is reported on
     * [com.wingedsheep.gym.EnvStatus] either way, so any episode can be replayed exactly.
     */
    val seed: Long? = null,

    /** When to cut an episode short. See [EnvLimits]. */
    val limits: EnvLimits = EnvLimits(),
) {
    init {
        require(players.size >= 2) { "Need at least 2 players" }
        require(perspectivePlayerIndex in players.indices) {
            "perspectivePlayerIndex=$perspectivePlayerIndex out of range for ${players.size} players"
        }
        require(players.any { it.agent is AgentSpec.Learner }) {
            "At least one seat must be a Learner; an env with none would never return"
        }
        // Observing from a seat nobody is learning is a silent way to train on the wrong
        // information set, so it is refused rather than defaulted.
        require(players[perspectivePlayerIndex].agent is AgentSpec.Learner) {
            "perspectivePlayerIndex=$perspectivePlayerIndex is a ${players[perspectivePlayerIndex].agent} seat, " +
                "not a Learner"
        }
    }
}

/**
 * Bounds that stop an env running forever. The defaults mirror the arena's, so a policy truncates
 * at the same place whether it is being trained or evaluated.
 *
 * These are not paranoia. A learned policy that has not yet learned when to stop passing will churn
 * priority indefinitely — one behaviour-cloned checkpoint played **zero** games in five minutes on
 * the arena schedule. Without a bound the env simply never returns.
 */
@Serializable
data class EnvLimits(
    /** Turn cap per seat; compared against the engine's turn counter times the seat count. */
    val maxTurnsPerSeat: Int = 50,

    /** Total actions submitted in one episode. */
    val maxActions: Int = 20_000,

    /**
     * Actions allowed without the active player changing. This is the arena's stuck detector: a
     * loop that never passes the turn is not a long game, it is a stuck one.
     */
    val maxActionsWithoutProgress: Int = 300,
) {
    init {
        require(maxTurnsPerSeat > 0) { "maxTurnsPerSeat must be positive" }
        require(maxActions > 0) { "maxActions must be positive" }
        require(maxActionsWithoutProgress > 0) { "maxActionsWithoutProgress must be positive" }
    }
}

/**
 * Everything needed to spin up a new **deckbuild** env (`POST /envs/deckbuild`).
 *
 * Opens [boosterCount] boosters from [setCode] into a sealed pool, then hands the agent
 * an enumerated build interface (add / remove / finalize) until it commits a [targetSize]-card
 * deck. The finished list is exposed on the terminal observation for the caller to feed into a
 * game env via [DeckSpec.Explicit].
 */
@Serializable
data class DeckbuildConfig(
    /** Set to open boosters from (e.g. "BLB"). Must be sealed-supported in the booster generator. */
    val setCode: String,

    /** Number of boosters to open (pack size follows the set's booster strategy). Tournament sealed = 6. */
    val boosterCount: Int = 6,

    /** Minimum legal deck size; `FINALIZE` unlocks once the build reaches it. */
    val targetSize: Int = 40
) {
    init {
        require(boosterCount > 0) { "boosterCount must be positive" }
        require(targetSize > 0) { "targetSize must be positive" }
    }
}

/** A single player's identity + deck. */
@Serializable
data class PlayerSpec(
    val name: String,
    val deck: DeckSpec,
    val startingLife: Int = 20,
    val playerId: EntityId? = null,

    /** Who acts for this seat. Defaults to the caller, which is how every env behaved before. */
    val agent: AgentSpec = AgentSpec.Learner()
)

/**
 * Who answers for a seat.
 *
 * A gym env used to hand every seat's decision to its caller. That is right for search and for
 * symmetric self-play, and wrong for training one policy against a fixed opponent: the opponent
 * would have to be reimplemented outside the engine that defines it. A [Pilot] seat is played
 * inside the env by the engine's own AI, so the trainer sees only the seat it is learning.
 */
@Serializable
sealed interface AgentSpec {

    /**
     * The caller acts for this seat.
     *
     * [decisionProfile] answers the structured pending decisions the flat action space cannot
     * express — targets, damage assignment, ordering, library search. The learned policy has no
     * heads for those, so the engine's AI takes them, exactly as it does in the arena; set it to
     * the same profile as the opponent so the two seats differ only where the policy actually acts.
     */
    @Serializable
    @SerialName("Learner")
    data class Learner(val decisionProfile: String = "current") : AgentSpec

    /** The engine's AI plays this seat inside the env, under [AiProfiles.parse]'s profile name. */
    @Serializable
    @SerialName("Pilot")
    data class Pilot(val profile: String = "current") : AgentSpec
}

/** A single environment's `step()` input — batched into [com.wingedsheep.gym.service.MultiEnvService.stepBatch]. */
@Serializable
data class StepRequest(
    val envId: EnvId,
    val actionId: Int,
    /** Choices the action ID can't carry — attackers, blockers, targets, X. See [ActionParams]. */
    val params: ActionParams = ActionParams.EMPTY
)

/** Result of deck validation. Surfaced by [DeckResolver.validate]. */
@Serializable
data class DeckValidation(
    val ok: Boolean,
    val errors: List<String> = emptyList(),
    val totalCards: Int = 0
)
