package com.wingedsheep.mtg.sets.definitions.tla.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.grantedActivatedAbility
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantActivatedAbility
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Trusty Boomerang
 * {1}
 * Artifact — Equipment
 *
 * Equipped creature has "{1}, {T}: Tap target creature. Return Trusty Boomerang to its owner's hand."
 * Equip {1}
 *
 * The granted ability lives on the *equipped creature* (CR 113.7: its source is the host, so the
 * `{T}` taps that creature and `EffectTarget.ContextTarget(0)` is "target creature"). "Return Trusty
 * Boomerang" names the Equipment, a different object — resolved via
 * [EffectTarget.GrantingSource], which points at the permanent whose static granted the ability.
 * If the Equipment has already left the battlefield by resolution, the move no-ops (CR 113.7a).
 */
val TrustyBoomerang = card("Trusty Boomerang") {
    manaCost = "{1}"
    typeLine = "Artifact — Equipment"
    oracleText = "Equipped creature has \"{1}, {T}: Tap target creature. Return Trusty Boomerang to " +
        "its owner's hand.\"\n" +
        "Equip {1} ({1}: Attach to target creature you control. Equip only as a sorcery.)"

    staticAbility {
        ability = GrantActivatedAbility(
            ability = grantedActivatedAbility {
                cost = Costs.Composite(Costs.Mana("{1}"), Costs.Tap)
                val creature = target(TargetFilter.Creature)
                effect = Effects.Tap(creature) then Effects.ReturnToHand(EffectTarget.GrantingSource)
            }
            // filter defaults to GroupFilter.attachedCreature() — "equipped creature has ..."
        )
    }

    equipAbility("{1}")

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "260"
        artist = "Toni Infante"
        flavorText = "\"Boomerang! You do always come back!\"\n—Sokka"
        imageUri = "https://cards.scryfall.io/normal/front/d/f/df99a166-6d58-4d92-9037-2fdfbaf65629.jpg?1764121919"
    }
}
