package com.wingedsheep.mtg.sets.definitions.mat.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.mode
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Mode
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Tranquil Frillback — March of the Machine: The Aftermath #24
 * {2}{G} · Creature — Dinosaur · Rare · 3/3
 *
 * When this creature enters, you may pay {G} up to three times. When you pay this cost one or
 * more times, choose up to that many —
 * • Destroy target artifact or enchantment.
 * • Exile target player's graveyard.
 * • You gain 4 life.
 *
 * Modeling notes:
 *  - The enters trigger goes on the stack with no modes and no targets; on resolution the
 *    controller may pay {G} one to three times ([Effects.PayRepeatedly], capped at three *and* at
 *    the green mana they can actually produce). Paying at least once fires the CR 603.12
 *    reflexive ability, whose modes and targets are chosen only then — exactly the ruling.
 *  - "Up to that many" is the modal's `dynamicChooseCount` over [DynamicAmounts.timesPaid], a
 *    ceiling with a floor of zero; modes can't repeat (CR 700.2d).
 */
val TranquilFrillback = card("Tranquil Frillback") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Dinosaur"
    power = 3
    toughness = 3
    oracleText = "When this creature enters, you may pay {G} up to three times. When you pay this " +
        "cost one or more times, choose up to that many —\n" +
        "• Destroy target artifact or enchantment.\n" +
        "• Exile target player's graveyard.\n" +
        "• You gain 4 life."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.ReflexiveTrigger(
            action = Effects.PayRepeatedly("{G}", upTo = 3),
            optional = true,
            reflexiveEffect = Effects.Modal(
                modes = listOf(
                    mode("Destroy target artifact or enchantment.") {
                        val permanent = target(TargetFilter.ArtifactOrEnchantment)
                        effect = Effects.Destroy(permanent)
                    },
                    mode("Exile target player's graveyard.") {
                        val player = target(Targets.Player)
                        effect = Effects.Pipeline {
                            val graveyard = gather(CardSource.FromZone(Zone.GRAVEYARD, player.asPlayer))
                            exile(graveyard, player.asPlayer)
                        }
                    },
                    Mode.noTarget(
                        effect = Effects.GainLife(4),
                        description = "You gain 4 life."
                    )
                ),
                dynamicChooseCount = DynamicAmounts.timesPaid()
            ),
            descriptionOverride = "You may pay {G} up to three times. When you pay this cost one or " +
                "more times, choose up to that many —\n" +
                "• Destroy target artifact or enchantment.\n" +
                "• Exile target player's graveyard.\n" +
                "• You gain 4 life."
        )
        description = "When this creature enters, you may pay {G} up to three times. When you pay " +
            "this cost one or more times, choose up to that many — Destroy target artifact or " +
            "enchantment.; Exile target player's graveyard.; You gain 4 life."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "24"
        artist = "Caio Monteiro"
        imageUri = "https://cards.scryfall.io/normal/front/5/b/5b647377-d47e-4630-8ccc-933ef6127880.jpg?1783916519"
        ruling(
            "2023-05-12",
            "Tranquil Frillback's triggered ability goes on the stack with no targets and no modes " +
                "chosen. As it resolves, you may pay {G} up to three times. That is, you may pay {G}, " +
                "{G}{G}, {G}{G}{G}, or choose not to pay. If you chose to pay, the second \"reflexive\" " +
                "triggered ability will trigger. You'll choose the modes and targets, if any, for that " +
                "second ability at that time."
        )
    }
}
