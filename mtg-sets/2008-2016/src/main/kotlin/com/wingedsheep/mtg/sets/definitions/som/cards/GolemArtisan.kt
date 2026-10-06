package com.wingedsheep.mtg.sets.definitions.som.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.effects.Mode
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Golem Artisan — Scars of Mirrodin #159
 * {5} · Artifact Creature — Golem · 3/3
 *
 * "Your choice of flying, trample, or haste" is chosen on resolution, so the second ability is a
 * modal *ability* (`countsAsModalSpell = false`) over one shared target chosen on activation.
 */
val GolemArtisan = card("Golem Artisan") {
    manaCost = "{5}"
    typeLine = "Artifact Creature — Golem"
    power = 3
    toughness = 3
    oracleText = "{2}: Target artifact creature gets +1/+1 until end of turn.\n" +
        "{2}: Target artifact creature gains your choice of flying, trample, or haste until end of turn."

    activatedAbility {
        val creature = target(TargetFilter(GameObjectFilter.ArtifactCreature))
        cost = Costs.Mana("{2}")
        effect = Effects.ModifyStats(1, 1, creature)
        description = "{2}: Target artifact creature gets +1/+1 until end of turn."
    }

    activatedAbility {
        val creature = target(TargetFilter(GameObjectFilter.ArtifactCreature))
        cost = Costs.Mana("{2}")
        effect = ModalEffect.chooseOne(
            Mode.noTarget(
                Effects.GrantKeyword(Keyword.FLYING, creature),
                "It gains flying until end of turn"
            ),
            Mode.noTarget(
                Effects.GrantKeyword(Keyword.TRAMPLE, creature),
                "It gains trample until end of turn"
            ),
            Mode.noTarget(
                Effects.GrantKeyword(Keyword.HASTE, creature),
                "It gains haste until end of turn"
            ),
            countsAsModalSpell = false
        )
        description = "{2}: Target artifact creature gains your choice of flying, trample, or haste until end of turn."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "159"
        artist = "Nic Klein"
        flavorText = "Better living through metallurgy."
        imageUri = "https://cards.scryfall.io/normal/front/7/c/7ccfc314-2f18-43c2-9ccd-59bb5dbe35e9.jpg?1783941708"
    }
}
