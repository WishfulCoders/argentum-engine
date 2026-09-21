package com.wingedsheep.gym

import kotlinx.serialization.Serializable

/**
 * What the learner seat's own AI would do at the current decision, without doing it.
 *
 * An anchored override (mtg-draft-ai `docs/49` §3, R2) is only worth anything where it disagrees
 * with the pilot, so a label corpus has to know the pilot's choice at each sampled state. [actionId]
 * is the observation's ID for that choice's template — the pilot materializes targets and X, so the
 * match is on kind, description, source and ability, the same as the arena's shadow teacher. It is
 * null only if no current ID has that template, which would be a contract bug worth seeing.
 */
@Serializable
data class PilotChoice(
    val actionId: Int?,
    val kind: String,
    val description: String,
    val isManaAbility: Boolean,
    /** Whether the policy boundary lets a learner choose it: an override can only replace what it could also have played. */
    val callable: Boolean,
)
