package com.wingedsheep.mtg.sets.definitions.mh2.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.MustAttack
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Dragon's Rage Channeler
 * {R} — Creature — Human Shaman 1/1 (Uncommon) — Modern Horizons 2 #121
 * Artist: Martina Fačková
 *
 * Whenever you cast a noncreature spell, surveil 1.
 * Delirium — As long as there are four or more card types among cards in your graveyard, this
 * creature gets +2/+2, has flying, and attacks each combat if able.
 *
 * The delirium clause is three [ConditionalStaticAbility]s over one [Conditions.Delirium] gate
 * (stats, flying, and the [MustAttack] requirement, as Zurgo Helmsmasher gates its own), so all
 * three switch on and off together as the graveyard changes.
 */
val DragonsRageChanneler = card("Dragon's Rage Channeler") {
    manaCost = "{R}"
    colorIdentity = "R"
    typeLine = "Creature — Human Shaman"
    power = 1
    toughness = 1
    oracleText = "Whenever you cast a noncreature spell, surveil 1. (Look at the top card of your library. " +
        "You may put that card into your graveyard.)\n" +
        "Delirium — As long as there are four or more card types among cards in your graveyard, this " +
        "creature gets +2/+2, has flying, and attacks each combat if able."

    triggeredAbility {
        trigger = Triggers.you.casts(GameObjectFilter.Noncreature)
        effect = Effects.Surveil(1)
    }

    val delirium = Conditions.Delirium()

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = ModifyStats(2, 2, GroupFilter.source()),
            condition = delirium
        )
    }

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = GrantKeyword(Keyword.FLYING, GroupFilter.source()),
            condition = delirium
        )
    }

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = MustAttack(GroupFilter.source()),
            condition = delirium
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "121"
        artist = "Martina Fačková"
        imageUri = "https://cards.scryfall.io/normal/front/4/c/4ced112a-e775-4f97-97b3-74877e9dce12.jpg?1783926848"
        ruling("2021-06-18", "Because damage remains marked on a creature until the cleanup step or an effect removes that damage, nonlethal damage dealt to Dragon's Rage Channeler may become lethal if the number of card types found in your graveyard drops below four.")
    }
}
