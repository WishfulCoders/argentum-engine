package com.wingedsheep.mtg.sets.definitions.inv.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.AbilityId
import com.wingedsheep.sdk.scripting.ActivatedAbility
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Defiling Tears
 * {2}{B}
 * Instant
 * Until end of turn, target creature becomes black, gets +1/-1, and gains "{B}: Regenerate this creature."
 */
val DefilingTears = card("Defiling Tears") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Instant"
    oracleText = "Until end of turn, target creature becomes black, gets +1/-1, and gains \"{B}: Regenerate this creature.\""

    spell {
        val t = target(TargetFilter.Creature)
        effect = Effects.ChangeColor(t, setOf(Color.BLACK)) then
            Effects.ModifyStats(1, -1, t) then
            Effects.GrantActivatedAbility(
                ability = ActivatedAbility(
                    id = AbilityId.next(),
                    cost = Costs.Mana(ManaCost.parse("{B}")),
                    effect = Effects.Regenerate(EffectTarget.Self)
                ),
                target = t
            )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "99"
        artist = "rk post"
        flavorText = "Serra's living warriors looked on in horror. \"Where is our Lady now?\" they cried."
        imageUri = "https://cards.scryfall.io/normal/front/d/b/db7cba29-9472-4874-bd54-37edf70645b2.jpg?1562939101"
    }
}
