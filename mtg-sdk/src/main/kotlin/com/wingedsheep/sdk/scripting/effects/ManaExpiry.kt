package com.wingedsheep.sdk.scripting.effects

import kotlinx.serialization.Serializable
import com.wingedsheep.sdk.dsl.firebending

/**
 * When mana produced by an effect leaves its owner's pool.
 *
 * This is the *duration* axis of mana, orthogonal to [ManaRestriction] (which controls
 * *where* the mana may be spent) and [ManaSpellRider] (which controls *what happens to a
 * spell* the mana is spent on).
 *
 * Every player's unspent mana empties as each step and phase ends (CR 500.5; see
 * `CleanupPhaseManager.emptyManaPools`), and [END_OF_TURN] — the default, despite its name — is
 * that ordinary mana. [UNTIL_END_OF_TURN] is mana that survives those emptyings and is lost as the
 * turn's cleanup step ends: "Until end of turn, you don't lose this mana as steps and phases end"
 * (Brazen Collector, Savage Ventmaw). [END_OF_COMBAT] is for mana that must be gone
 * once the combat phase ends — firebending (Avatar: The Last Airbender, CR 702.189):
 * "Until end of combat, you don't lose this mana as steps and phases end. Any of this mana
 * you still have as combat ends will be lost." Combat-duration mana is held as an
 * [ManaRestriction.AnySpend] restricted entry (so it flows through the normal spend logic)
 * tagged with this expiry, and cleared by `CombatManager.endCombat`. [UNTIL_END_OF_TURN] mana is
 * held the same way.
 */
@Serializable
enum class ManaExpiry {
    /** Ordinary mana (the default): lost as the current step or phase ends. */
    END_OF_TURN,

    /** Kept as steps and phases end; lost as the turn ends ("until end of turn, you don't lose this mana"). */
    UNTIL_END_OF_TURN,

    /** Mana is discarded when the combat phase ends (firebending). */
    END_OF_COMBAT,
}
