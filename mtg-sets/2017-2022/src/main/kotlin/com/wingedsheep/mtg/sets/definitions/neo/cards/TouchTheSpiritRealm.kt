package com.wingedsheep.mtg.sets.definitions.neo.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Touch the Spirit Realm — Kamigawa: Neon Dynasty #40
 * {2}{W} · Enchantment
 *
 * When this enchantment enters, exile up to one target artifact or creature until this enchantment
 * leaves the battlefield.
 * Channel — {1}{W}, Discard this card: Exile target artifact or creature. Return it to the
 * battlefield under its owner's control at the beginning of the next end step.
 *
 * - The enters half is the Banishing Light shape with an optional ("up to one") target.
 * - Channel is an activated ability from hand with the discard as part of the cost (the NEO
 *   channel-land shape, see Eiganjo, Seat of the Empire); its effect is
 *   [Patterns.Exile.exileUntilEndStep], the Voyager Staff blink. The channel target is mandatory,
 *   so it can't be activated without one just to discard the card (printed ruling).
 */
val TouchTheSpiritRealm = card("Touch the Spirit Realm") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment"
    oracleText = "When this enchantment enters, exile up to one target artifact or creature until this enchantment leaves the battlefield.\n" +
        "Channel — {1}{W}, Discard this card: Exile target artifact or creature. Return it to the battlefield under its owner's control at the beginning of the next end step."

    triggeredAbility {
        trigger = Triggers.self.enters()
        val t = target(TargetFilter.CreatureOrArtifact, optional = true)
        effect = Effects.ExileUntilLeaves(t)
        description = "When this enchantment enters, exile up to one target artifact or creature until this enchantment leaves the battlefield."
    }

    triggeredAbility {
        trigger = Triggers.self.leaves()
        effect = Effects.ReturnLinkedExileUnderOwnersControl()
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}{W}"), Costs.DiscardSelf)
        activateFromZone = Zone.HAND
        val t = target(TargetFilter.CreatureOrArtifact)
        effect = Patterns.Exile.exileUntilEndStep(t)
        description = "Channel — {1}{W}, Discard this card: Exile target artifact or creature. Return it at the beginning of the next end step."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "40"
        artist = "Marta Nael"
        imageUri = "https://cards.scryfall.io/normal/front/e/1/e16ab44e-4257-4c0c-b705-8ac1e9c1d835.jpg?1783923911"
        ruling("2022-02-18", "If Touch the Spirit Realm leaves the battlefield before its first ability resolves, the target permanent won't be exiled.")
        ruling("2022-02-18", "If a token is exiled this way, it will cease to exist and won't return to the battlefield.")
        ruling("2022-02-18", "If a channel ability requires a target, you may not activate it without a target just to discard the card.")
    }
}
