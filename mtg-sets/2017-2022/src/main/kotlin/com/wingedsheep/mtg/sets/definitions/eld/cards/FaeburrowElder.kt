package com.wingedsheep.mtg.sets.definitions.eld.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantDynamicStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Faeburrow Elder — Throne of Eldraine #190 (canonical printing)
 * {1}{G}{W} · Creature — Treefolk Druid · 0/0
 *
 * Vigilance
 * This creature gets +1/+1 for each color among permanents you control.
 * {T}: For each color among permanents you control, add one mana of that color.
 *
 * The bonus is Earthen Ally's layer-7c [GrantDynamicStats] over [GroupFilter.source], counting
 * [DynamicAmounts.colorsAmongPermanents] (projected colors, so the Elder's own green and white
 * count). The mana ability is Bloom Tender's [Effects.AddOneManaOfEachColorAmong]. Colorless isn't
 * a color, so both cap at five (ruling).
 */
val FaeburrowElder = card("Faeburrow Elder") {
    manaCost = "{1}{G}{W}"
    colorIdentity = "GW"
    typeLine = "Creature — Treefolk Druid"
    power = 0
    toughness = 0
    oracleText = "Vigilance\n" +
        "This creature gets +1/+1 for each color among permanents you control.\n" +
        "{T}: For each color among permanents you control, add one mana of that color."

    keywords(Keyword.VIGILANCE)

    staticAbility {
        ability = GrantDynamicStats(
            filter = GroupFilter.source(),
            powerBonus = DynamicAmounts.colorsAmongPermanents(),
            toughnessBonus = DynamicAmounts.colorsAmongPermanents()
        )
    }

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddOneManaOfEachColorAmong(GameObjectFilter.Permanent.youControl())
        manaAbility = true
        description = "{T}: For each color among permanents you control, add one mana of that color."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "190"
        artist = "Raoul Vitale"
        flavorText = "Its wide-reaching roots draw more than water."
        imageUri = "https://cards.scryfall.io/normal/front/1/c/1ca29912-88b1-413f-ad9d-63d7d1b1ca16.jpg?1783932597"
        ruling("2019-10-04", "Faeburrow Elder's middle ability can give it at most +5/+5, that is, +1/+1 each for white, blue, black, red, and green. \"Gold,\" \"multicolor,\" and \"colorless\" aren't colors. Similarly, Faeburrow Elder's last ability can produce at most five mana.")
        ruling("2019-10-04", "Since Faeburrow Elder is a green and white permanent, its middle ability usually gives it at least +2/+2 and its last ability usually produces at least {G}{W}.")
    }
}
