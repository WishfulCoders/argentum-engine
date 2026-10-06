package com.wingedsheep.mtg.sets.definitions.clb.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Gut, True Soul Zealot — Commander Legends: Battle for Baldur's Gate #180
 * {2}{R} · Legendary Creature — Goblin Shaman · 2/2 · Uncommon
 *
 * Whenever you attack, you may sacrifice another creature or an artifact. If you do, create a 4/1
 * black Skeleton creature token with menace that's tapped and attacking.
 * Choose a Background
 *
 * Modeling notes:
 *  - "Whenever you attack" is [Triggers.you] `attacks()` — once per combat when you declare one or
 *    more attackers; Gut needn't be among them (ruling).
 *  - "You may sacrifice …. If you do, …" is [Effects.MayPay] with the sacrifice as the cost: no
 *    "yes" is offered without fodder, and declining makes no token. The fodder is
 *    `Creature.notSourceItself() or Artifact` rather than `excludeSource`, because "another"
 *    binds only the creature branch — if Gut has become an artifact it may sacrifice itself
 *    (ruling).
 *  - The Skeleton enters tapped and attacking (CR 508.4) but was never declared as an attacker, so
 *    "whenever a creature attacks" abilities don't see it (ruling). The engine picks its defender
 *    (the defending player in a two-player game); a choice of a planeswalker or battle instead is
 *    not offered — the shared "tapped and attacking" token limitation, not specific to this card.
 *  - Choose a Background (CR 702.124k) is a Commander deck-construction ability that works only
 *    before the game begins; it has no in-game effect, so it lives in the oracle text only.
 */
val GutTrueSoulZealot = card("Gut, True Soul Zealot") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Legendary Creature — Goblin Shaman"
    power = 2
    toughness = 2
    oracleText = "Whenever you attack, you may sacrifice another creature or an artifact. If you do, " +
        "create a 4/1 black Skeleton creature token with menace that's tapped and attacking. " +
        "(It can't be blocked except by two or more creatures.)\n" +
        "Choose a Background (You can have a Background as a second commander.)"

    triggeredAbility {
        trigger = Triggers.you.attacks()
        effect = Effects.MayPay(
            cost = Effects.SacrificeOwn(GameObjectFilter.Creature.notSourceItself() or GameObjectFilter.Artifact),
            then = Effects.CreateToken(
                power = 4,
                toughness = 1,
                colors = setOf(Color.BLACK),
                creatureTypes = setOf("Skeleton"),
                keywords = setOf(Keyword.MENACE),
                tapped = true,
                attacking = true,
                imageUri = "https://cards.scryfall.io/normal/front/c/f/cf4c245f-af2f-46a7-81f3-670a04940901.jpg?1783922321"
            )
        )
        description = "Whenever you attack, you may sacrifice another creature or an artifact. If you " +
            "do, create a 4/1 black Skeleton creature token with menace that's tapped and attacking."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "180"
        artist = "Wayne Reynolds"
        imageUri = "https://cards.scryfall.io/normal/front/3/d/3d8ca18d-9099-4f1e-95c1-f04da58a26bd.jpg?1783922736"
        ruling("2022-06-10", "You choose the player or planeswalker the Skeleton is attacking.")
        ruling("2022-06-10", "Gut doesn't have to be among the attacking creatures.")
        ruling("2022-06-10", "Although the Skeleton is an attacking creature, it was never declared as an attacking creature. This means that abilities that trigger whenever a creature attacks won't trigger when it enters the battlefield attacking.")
        ruling("2022-06-10", "Any effects that say the Skeleton can't attack (such as that of Propaganda) affect only the declaration of attackers. They won't stop the Skeleton from entering the battlefield attacking.")
        ruling("2022-06-10", "If Gut somehow becomes an artifact, you may sacrifice it to its own ability.")
    }
}
