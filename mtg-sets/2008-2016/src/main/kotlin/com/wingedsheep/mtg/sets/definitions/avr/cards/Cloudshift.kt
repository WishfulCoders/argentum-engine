package com.wingedsheep.mtg.sets.definitions.avr.cards

import com.wingedsheep.sdk.dsl.*
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

val Cloudshift = card("Cloudshift") {
    manaCost = "{W}"
    typeLine = "Instant"
    oracleText = "Exile target creature you control, then return that card to the battlefield under your control."
    colorIdentity = "W"

    spell {
        val creature = target(TargetFilter.CreatureYouControl)
        effect = Effects.Exile(creature) then Effects.PutOntoBattlefieldUnderYourControl(creature)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "12"
        artist = "Howard Lyon"
        flavorText = "\"Even storm clouds bow to worship Avacyn.\"\n—Elder Rimheit"
        imageUri = "https://cards.scryfall.io/normal/front/3/5/35b06c8f-5f08-43bd-a548-2a98ba30fd41.jpg?1783940738"
        ruling("2018-03-16", "The returned card won't be the target of any spells or abilities that targeted it before. Any spells that don't target, such as Akroma's Vengeance, will still affect it.")
        ruling("2018-03-16", "Once the exiled permanent returns, it's considered a new object with no relation to the object that it was. Auras attached to the exiled permanent will be put into their owners' graveyards. Equipment attached to the exiled permanent will become unattached and remain on the battlefield. Any counters on the exiled permanent will cease to exist.")
        ruling("2018-03-16", "If a token is exiled this way, it will cease to exist and won't return to the battlefield.")
        ruling("2018-03-16", "When an effect returns the exiled card \"under your control,\" you control it indefinitely after that. In a multiplayer game, if a player leaves the game, all cards that player owns leave as well. If you leave the game, any creatures you control from Cloudshift's effect are exiled.")
    }
}
