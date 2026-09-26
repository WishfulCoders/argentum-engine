package com.wingedsheep.mtg.sets.definitions.c14.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Unstable Obelisk
 * {3}
 * Artifact
 *
 * {T}: Add {C}.
 * {7}, {T}, Sacrifice this artifact: Destroy target permanent.
 */
val UnstableObelisk = card("Unstable Obelisk") {
    manaCost = "{3}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "{T}: Add {C}.\n{7}, {T}, Sacrifice this artifact: Destroy target permanent."

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddColorlessMana(1)
        manaAbility = true
    }

    activatedAbility {
        val permanent = target(TargetFilter.Permanent)
        cost = Costs.Composite(Costs.Mana("{7}"), Costs.Tap, Costs.SacrificeSelf)
        effect = Effects.Destroy(permanent)
        description = "{7}, {T}, Sacrifice this artifact: Destroy target permanent."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "58"
        artist = "William Wu"
        flavorText = "Its collapse is like the lashing out of a long-dead civilization that resents being forgotten."
        imageUri = "https://cards.scryfall.io/normal/front/b/3/b331b231-f31f-479f-80a9-f32c64d35096.jpg?1783938864"
    }
}
