package com.wingedsheep.mtg.sets.definitions.lrw.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Inner-Flame Acolyte
 * {1}{R}{R}
 * Creature — Elemental Shaman
 * 2/2
 * When this creature enters, target creature gets +2/+0 and gains haste until end of turn.
 * Evoke {R}
 */
val InnerFlameAcolyte = card("Inner-Flame Acolyte") {
    manaCost = "{1}{R}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Elemental Shaman"
    power = 2
    toughness = 2
    oracleText = "When this creature enters, target creature gets +2/+0 and gains haste until end " +
        "of turn.\nEvoke {R} (You may cast this spell for its evoke cost. If you do, it's " +
        "sacrificed when it enters.)"

    evoke = "{R}"

    triggeredAbility {
        trigger = Triggers.self.enters()
        val creature = target(TargetFilter.Creature)
        effect = Effects.ModifyStats(2, 0, creature) then Effects.GrantKeyword(Keyword.HASTE, creature)
        description = "target creature gets +2/+0 and gains haste until end of turn."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "181"
        artist = "Ron Spears"
        imageUri = "https://cards.scryfall.io/normal/front/f/9/f99a113a-013a-411b-9f16-975cebd5ea5f.jpg?1783942873"
    }
}
