package com.wingedsheep.mtg.sets.definitions.conflux.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TimingRule

/**
 * Noble Hierarch — Conflux #87
 * {G} · Creature — Human Druid · 0/1 · Rare
 *
 * Exalted (Whenever a creature you control attacks alone, that creature gets +1/+1 until end of turn.)
 * {T}: Add {G}, {W}, or {U}.
 *
 * Exalted (CR 702.83a) is the printed keyword; the engine derives its trigger from the projected
 * keyword (see [com.wingedsheep.sdk.scripting.Exalted]). The printed "or" is a choice between
 * three separate mana abilities sharing [Costs.Tap].
 */
val NobleHierarch = card("Noble Hierarch") {
    manaCost = "{G}"
    colorIdentity = "GWU"
    typeLine = "Creature — Human Druid"
    power = 0
    toughness = 1
    oracleText = "Exalted (Whenever a creature you control attacks alone, that creature gets +1/+1 until end of turn.)\n" +
        "{T}: Add {G}, {W}, or {U}."

    keywords(Keyword.EXALTED)

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.GREEN)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }
    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.WHITE)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }
    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.BLUE)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "87"
        artist = "Mark Zug"
        flavorText = "She protects the sacred groves from blight, drought, and the Unbeholden."
        imageUri = "https://cards.scryfall.io/normal/front/6/a/6adfe928-1305-444d-b709-1e714544daaf.jpg?1783942473"
        ruling("2020-08-07", "If you declare exactly one creature as an attacker, each exalted ability on each permanent you control (including, perhaps, the attacking creature itself) will trigger.")
        ruling("2020-08-07", "You must attack with exactly one creature for exalted abilities to trigger. Exalted abilities won't trigger if you attack a player with one creature and a planeswalker with another, for example, or if you attack with two creatures but one is removed from combat.")
        ruling("2020-08-07", "Some effects put creatures onto the battlefield attacking. Since those creatures were never declared as attackers, they're ignored by exalted abilities. They won't cause exalted abilities to trigger. If any exalted abilities have already triggered (because exactly one creature was declared as an attacker), those abilities will resolve as normal even though there may now be multiple attackers.")
    }
}
