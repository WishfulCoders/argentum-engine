package com.wingedsheep.mtg.sets.definitions.dsk.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Cracked Skull
 * {2}{B}
 * Enchantment — Aura
 * Enchant creature
 * When this Aura enters, look at target player's hand. You may choose a nonland card from it.
 * That player discards that card.
 * When enchanted creature is dealt damage, destroy it.
 */
val CrackedSkull = card("Cracked Skull") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\n" +
        "When this Aura enters, look at target player's hand. You may choose a nonland card " +
        "from it. That player discards that card.\n" +
        "When enchanted creature is dealt damage, destroy it."

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    // When this Aura enters, look at target player's hand. You may choose a nonland card from it.
    // That player discards that card.
    triggeredAbility {
        trigger = Triggers.self.enters()
        val player = target(Targets.Player)
        effect = Effects.Pipeline {
            run(Effects.LookAtHand(player))
            val targetHand = gather(CardSource.FromZone(Zone.HAND, player.asPlayer))
            val toDiscard = chooseUpTo(
                1,
                from = targetHand,
                chooser = Chooser.Controller,
                filter = GameObjectFilter.Nonland,
                prompt = "You may choose a nonland card for that player to discard",
                showAllCards = true,
                alwaysPrompt = true
            )
            discard(toDiscard, player.asPlayer)
        }
    }

    // When enchanted creature is dealt damage, destroy it.
    triggeredAbility {
        trigger = Triggers.attached.isDealtDamage()
        effect = Effects.Destroy(EffectTarget.EnchantedCreature)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "88"
        artist = "Mirko Failoni"
        flavorText = "Ears ringing and head throbbing, all Tarvin could do was wait to see what would finish the job."
        imageUri = "https://cards.scryfall.io/normal/front/7/6/7616ad5e-ed30-4876-8743-6f0f9f143ea1.jpg?1726286178"
    }
}
