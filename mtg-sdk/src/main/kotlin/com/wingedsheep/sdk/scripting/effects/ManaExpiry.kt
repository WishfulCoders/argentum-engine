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
 * The engine empties mana pools as each step and phase ends (CR 500.5), and [END_OF_TURN] — the
 * default, despite its name — is that ordinary mana: lost at the next step/phase boundary.
 * [KEPT_UNTIL_END_OF_TURN] is "Until end of turn, you don't lose this mana as steps and phases
 * end" (Brazen Collector, Savage Ventmaw): it survives every boundary this turn and becomes
 * ordinary mana once end-of-turn cleanup ends the effect. [END_OF_COMBAT] is for mana that must be gone
 * once the combat phase ends — firebending (Avatar: The Last Airbender, CR 702.189):
 * "Until end of combat, you don't lose this mana as steps and phases end. Any of this mana
 * you still have as combat ends will be lost." Combat-duration mana is held as an
 * [ManaRestriction.AnySpend] restricted entry (so it flows through the normal spend logic)
 * tagged with this expiry, and cleared by `CombatManager.endCombat`. Turn-duration mana is held
 * the same way.
 */
@Serializable
enum class ManaExpiry {
    /** Ordinary mana, lost as the current step or phase ends (the default for all mana). */
    END_OF_TURN,

    /**
     * "Until end of turn, you don't lose this mana as steps and phases end." Kept through every
     * step/phase boundary this turn; end-of-turn cleanup downgrades it to [END_OF_TURN], so it is
     * lost as the cleanup step ends like any other mana.
     */
    KEPT_UNTIL_END_OF_TURN,

    /** Mana is discarded when the combat phase ends (firebending). */
    END_OF_COMBAT,
}
