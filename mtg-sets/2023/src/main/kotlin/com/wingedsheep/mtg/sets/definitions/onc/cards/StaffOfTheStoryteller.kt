package com.wingedsheep.mtg.sets.definitions.onc.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Staff of the Storyteller — Phyrexia: All Will Be One Commander #10
 * {1}{W}
 * Artifact
 *
 * When this artifact enters, create a 1/1 white Spirit creature token with flying.
 * Whenever you create one or more creature tokens, put a story counter on this artifact.
 * {W}, {T}, Remove a story counter from this artifact: Draw a card.
 *
 * "One or more" is the batched token-creation trigger (`createsToken(Creature, batch = true)`): one
 * story counter per simultaneous creation however many creature tokens it made (CR 603.2c), and
 * none for a token copy of a permanent spell, which is not "created" (CR 111.13). The Staff's own
 * enter trigger creates a Spirit, so it puts its first story counter on itself.
 */
val StaffOfTheStoryteller = card("Staff of the Storyteller") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Artifact"
    oracleText = "When this artifact enters, create a 1/1 white Spirit creature token with flying.\n" +
        "Whenever you create one or more creature tokens, put a story counter on this artifact.\n" +
        "{W}, {T}, Remove a story counter from this artifact: Draw a card."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = setOf(Color.WHITE),
            creatureTypes = setOf("Spirit"),
            keywords = setOf(Keyword.FLYING),
            imageUri = "https://cards.scryfall.io/normal/front/a/1/a171254d-7616-42c4-bd78-b2787fb973aa.jpg?1783918176"
        )
        description = "When this artifact enters, create a 1/1 white Spirit creature token with flying."
    }

    triggeredAbility {
        trigger = Triggers.you.createsToken(GameObjectFilter.Creature, batch = true)
        effect = Effects.AddCounters(CounterType.STORY, 1, EffectTarget.Self)
        description = "Whenever you create one or more creature tokens, put a story counter on this artifact."
    }

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{W}"),
            Costs.Tap,
            Costs.RemoveCounterFromSelf(CounterType.STORY, 1)
        )
        effect = Effects.DrawCards(1)
        description = "{W}, {T}, Remove a story counter from this artifact: Draw a card."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "10"
        artist = "Dan Murayama Scott"
        imageUri = "https://cards.scryfall.io/normal/front/a/b/ab1d1461-1625-4163-aacd-a939f4871fad.jpg?1783918162"
    }
}
