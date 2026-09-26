package com.wingedsheep.mtg.sets.definitions.lci.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.mode
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.effects.Mode
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Kutzil's Flanker
 * {2}{W}
 * Creature — Cat Warrior
 * 3/1
 * Flash
 * When this creature enters, choose one —
 * • Put a +1/+1 counter on this creature for each creature that left the battlefield under your
 *   control this turn.
 * • You gain 2 life and scry 2.
 * • Exile target player's graveyard.
 */
val KutzilsFlanker = card("Kutzil's Flanker") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Cat Warrior"
    oracleText = "Flash\n" +
        "When this creature enters, choose one —\n" +
        "• Put a +1/+1 counter on this creature for each creature that left the battlefield under " +
        "your control this turn.\n" +
        "• You gain 2 life and scry 2.\n" +
        "• Exile target player's graveyard."
    power = 3
    toughness = 1
    keywords(Keyword.FLASH)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = ModalEffect.chooseOne(
            Mode.noTarget(
                Effects.AddDynamicCounters(
                    CounterType.PLUS_ONE_PLUS_ONE,
                    DynamicAmounts.creaturesLeftBattlefieldThisTurn(Player.You),
                    EffectTarget.Self
                ),
                "Put a +1/+1 counter on this creature for each creature that left the battlefield " +
                    "under your control this turn"
            ),
            Mode.noTarget(
                Effects.GainLife(2) then Effects.Scry(2),
                "You gain 2 life and scry 2"
            ),
            mode("Exile target player's graveyard") {
                val player = target(Targets.Player)
                effect = Effects.Pipeline {
                    val targetGraveyard = gather(CardSource.FromZone(Zone.GRAVEYARD, player.asPlayer))
                    exile(targetGraveyard, player.asPlayer)
                }
            }
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "20"
        artist = "Michele Giorgi"
        imageUri = "https://cards.scryfall.io/normal/front/d/1/d1201811-54ab-4c4e-b6e1-19b0d07e5ede.jpg?1782694597"
    }
}
