package com.wingedsheep.mtg.sets.definitions.dsk.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Stay Hidden, Stay Silent — Duskmourn: House of Horror #74
 * {1}{U} · Enchantment — Aura
 *
 * Enchant creature
 * When this Aura enters, tap enchanted creature.
 * Enchanted creature doesn't untap during its controller's untap step.
 * {4}{U}{U}: Shuffle enchanted creature into its owner's library, then manifest dread. Activate
 * only as a sorcery.
 *
 * Modeled as:
 *  - `auraTarget = Targets.Creature` (Enchant creature).
 *  - ETB trigger (`Triggers.self.enters()`, SELF binding) taps the enchanted creature
 *    ([Effects.Tap] on [EffectTarget.EnchantedCreature]).
 *  - "doesn't untap" is the [AbilityFlag.DOESNT_UNTAP] keyword granted to the enchanted creature
 *    via an unfiltered aura static (same shape as Frozen Solid). The untap step reads the flag.
 *  - The sorcery-speed activated ability composes [Effects.ShuffleIntoLibrary] (the enchanted
 *    creature goes to its owner's library — the Aura then falls off as an SBA, but the ability
 *    keeps resolving) followed by the shared [Patterns.Library.manifestDread] recipe (CR 701.62).
 */
val StayHiddenStaySilent = card("Stay Hidden, Stay Silent") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\n" +
        "When this Aura enters, tap enchanted creature.\n" +
        "Enchanted creature doesn't untap during its controller's untap step.\n" +
        "{4}{U}{U}: Shuffle enchanted creature into its owner's library, then manifest dread. " +
        "Activate only as a sorcery."

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.Tap(EffectTarget.EnchantedCreature)
        description = "When this Aura enters, tap enchanted creature."
    }

    staticAbility {
        ability = GrantKeyword(AbilityFlag.DOESNT_UNTAP.name)
    }

    activatedAbility {
        cost = Costs.Mana("{4}{U}{U}")
        timing = TimingRule.SorcerySpeed
        effect = Effects.ShuffleIntoLibrary(EffectTarget.EnchantedCreature) then
            Patterns.Library.manifestDread()
        description = "{4}{U}{U}: Shuffle enchanted creature into its owner's library, then " +
            "manifest dread. Activate only as a sorcery."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "74"
        artist = "Josu Hernaiz"
        imageUri = "https://cards.scryfall.io/normal/front/b/6/b661ffe7-4f58-43fe-a1f1-dd7a6d4d28a7.jpg?1726286129"
    }
}
