package com.wingedsheep.mtg.sets.definitions.ecl.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Vinebred Brawler
 * {2}{G}
 * Creature — Elf Berserker
 * 4/2
 *
 * This creature must be blocked if able.
 * Whenever this creature attacks, another target Elf you control gets +2/+1 until end of turn.
 */
val VinebredBrawler = card("Vinebred Brawler") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Elf Berserker"
    power = 4
    toughness = 2
    oracleText = "This creature must be blocked if able.\nWhenever this creature attacks, another target Elf you control gets +2/+1 until end of turn."

    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = Effects.MustBeBlocked(EffectTarget.Self, allCreatures = false)
    }

    triggeredAbility {
        trigger = Triggers.self.attacks()
        val elf = target(TargetFilter.OtherCreatureYouControl.withSubtype(Subtype.ELF))
        effect = Effects.ModifyStats(2, 1, elf)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "201"
        artist = "Evyn Fong"
        flavorText = "Once looked down upon, vinebreeders proved invaluable during the Phyrexian Invasion and soon became exquisite in the eyes of elves."
        imageUri = "https://cards.scryfall.io/normal/front/6/3/63c573fe-e74c-48ca-ad05-92f37dc466f1.jpg?1767658495"
    }
}
