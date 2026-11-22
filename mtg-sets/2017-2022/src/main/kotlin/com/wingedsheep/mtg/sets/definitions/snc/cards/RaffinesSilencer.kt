package com.wingedsheep.mtg.sets.definitions.snc.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.unaryMinus
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Raffine's Silencer (SNC #90)
 * {2}{B}  Creature — Human Assassin  1/1
 *
 * When this creature enters, it connives.
 * When this creature dies, target creature an opponent controls gets -X/-X until end of turn,
 * where X is this creature's power.
 *
 * X reads the Silencer's power as it last existed on the battlefield (ruling 2022-04-29): the
 * source-power amount falls back to the last-known snapshot once the Silencer is in the
 * graveyard, so a +1/+1 counter from conniving still counts.
 */
val RaffinesSilencer = card("Raffine's Silencer") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Human Assassin"
    power = 1
    toughness = 1
    oracleText = "When this creature enters, it connives. (Draw a card, then discard a card. If you " +
        "discarded a nonland card, put a +1/+1 counter on this creature.)\n" +
        "When this creature dies, target creature an opponent controls gets -X/-X until end of turn, " +
        "where X is this creature's power."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.Connive()
    }

    triggeredAbility {
        trigger = Triggers.self.dies()
        val victim = target(TargetFilter.CreatureOpponentControls)
        val shrink = -DynamicAmounts.sourcePower()
        effect = Effects.ModifyStats(shrink, shrink, victim)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "90"
        artist = "Fajareka Setiawan"
        imageUri = "https://cards.scryfall.io/normal/front/4/b/4ba72399-c827-43f3-b072-08597dbafb2d.jpg?1783923129"
        ruling(
            "2022-04-29",
            "Use Raffine's Silencer's power as it last existed on the battlefield to determine the value of X."
        )
        ruling(
            "2022-04-29",
            "If a resolving spell or ability instructs a specific creature to connive but that creature has " +
                "left the battlefield, the creature still connives. If you discard a nonland card this way, " +
                "you won't put a +1/+1 counter on anything."
        )
    }
}
