package com.wingedsheep.mtg.sets.definitions.shm.cards

import com.wingedsheep.sdk.dsl.*
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

val Scuttlemutt = card("Scuttlemutt") {
    manaCost = "{3}"
    typeLine = "Artifact Creature — Scarecrow"
    oracleText = "{T}: Add one mana of any color.\n{T}: Target creature becomes the color or colors of your choice until end of turn."
    colorIdentity = ""
    power = 2
    toughness = 2

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddManaOfChoice()
        manaAbility = true
        timing = TimingRule.ManaAbility
        description = "{T}: Add one mana of any color."
    }
    activatedAbility {
        cost = Costs.Tap
        val creature = target(TargetFilter.Creature)
        effect = Effects.ChooseColorsThen(Effects.ChangeColorToChosen(creature))
        description = "{T}: Target creature becomes the color or colors of your choice until end of turn."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "263"
        artist = "Jeremy Jarvis"
        flavorText = "Built to shuttle goods from the river, it took off one day carrying a cauldron of dyes."
        imageUri = "https://cards.scryfall.io/normal/front/0/7/07cb3ab2-6d34-4cbf-ad46-070516a7cc54.jpg?1783942709"
        ruling("2019-07-12", "You must choose one or more of the five colors of Magic while resolving Scuttlemutt’s last ability. You can’t choose “artifact,” “colorless,” or “chartreuse.”")
        ruling("2019-07-12", "The target creature has only the colors Scuttlemutt gives it—Scuttlemutt’s ability overwrites any previous colors the creature had.")
        ruling("2019-07-12", "In the Commander variant, the color identities of cards are determined as the game begins. Changing the colors of a player’s commander doesn’t affect its color identity or the cards that can be legally included in the deck.")
    }
}
