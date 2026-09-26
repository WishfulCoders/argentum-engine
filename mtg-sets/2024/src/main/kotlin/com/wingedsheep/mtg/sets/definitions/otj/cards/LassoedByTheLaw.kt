package com.wingedsheep.mtg.sets.definitions.otj.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.grantedActivatedAbility
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Lassoed by the Law
 * {3}{W}
 * Enchantment
 * When this enchantment enters, exile target nonland permanent an opponent controls
 * until this enchantment leaves the battlefield.
 * When this enchantment enters, create a 1/1 red Mercenary creature token with
 * "{T}: Target creature you control gets +1/+0 until end of turn. Activate only as a sorcery."
 *
 * Banishing Light's exile-until-leaves shape ([Effects.ExileUntilLeaves] /
 * [Effects.ReturnLinkedExileUnderOwnersControl]) plus a second, independent ETB trigger that
 * makes the standard OTJ 1/1 red Mercenary token (matching Hellspur Posse Boss) — a sorcery-speed
 * tap-to-pump body via [CreateTokenEffect.activatedAbilities]. The two ETB triggers are separate
 * (CR 603.3b — both go on the stack the same way), so exile and token creation are independent.
 */
val LassoedByTheLaw = card("Lassoed by the Law") {
    manaCost = "{3}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment"
    oracleText = "When this enchantment enters, exile target nonland permanent an opponent controls " +
        "until this enchantment leaves the battlefield.\n" +
        "When this enchantment enters, create a 1/1 red Mercenary creature token with \"{T}: Target " +
        "creature you control gets +1/+0 until end of turn. Activate only as a sorcery.\""

    triggeredAbility {
        trigger = Triggers.self.enters()
        val permanent = target(TargetFilter.NonlandPermanentOpponentControls)
        effect = Effects.ExileUntilLeaves(permanent)
    }

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.CreateToken(
            count = 1,
            power = 1,
            toughness = 1,
            colors = setOf(Color.RED),
            creatureTypes = setOf("Mercenary"),
            activatedAbilities = listOf(
                grantedActivatedAbility {
                    cost = AbilityCost.Tap
                    val creatureYouControl = target(TargetFilter.CreatureYouControl)
                    effect = Effects.ModifyStats(1, 0, creatureYouControl)
                    timing = TimingRule.SorcerySpeed
                }
            ),
            imageUri = "https://cards.scryfall.io/normal/front/5/f/5f04607f-eed2-462e-897f-82e41e5f7049.jpg?1712316319"
        )
        description = "When this enchantment enters, create a 1/1 red Mercenary creature token with " +
            "\"{T}: Target creature you control gets +1/+0 until end of turn. Activate only as a sorcery.\""
    }

    triggeredAbility {
        trigger = Triggers.self.leaves()
        effect = Effects.ReturnLinkedExileUnderOwnersControl()
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "18"
        artist = "Leanna Crossan"
        imageUri = "https://cards.scryfall.io/normal/front/e/a/ea96eeac-c316-4247-a81f-0ddf52675ebf.jpg?1712355295"
    }
}
