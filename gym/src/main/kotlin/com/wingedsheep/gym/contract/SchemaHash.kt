package com.wingedsheep.gym.contract

/**
 * Contract-schema hash. Python clients compare this against the value they
 * were built with; a mismatch warns (or hard-fails in strict mode) that the
 * DTO shape has drifted.
 *
 * Bump [CURRENT] whenever any `@Serializable` data class in this package
 * changes shape — or a field changes what it means — in a way that would
 * break a downstream consumer. The value itself is arbitrary; uniqueness is
 * what matters.
 */
object SchemaHash {
    // Fork v1.8 (`structuredPayload`) merged with upstream's v1.8 (`costChoices`, the deciding seat's
    // candidateIds / candidateInfo / targetRequirements / modes, FLIP_ONTO_BATTLEFIELD and
    // ORDER_TRIGGERS kinds) and the ANTE per-player zone appended to TRAINING_OBSERVATION_ZONE_ORDER.
    // A new name because neither side's v1.8 describes this shape (each player now has 8 zones).
    const val CURRENT: String = "argentum-gym-contract@v1.8-structured-decisions+cost-choices-ante"
}
