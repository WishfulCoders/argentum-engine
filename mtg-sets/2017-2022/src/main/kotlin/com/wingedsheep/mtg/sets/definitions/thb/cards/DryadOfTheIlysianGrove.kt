package com.wingedsheep.mtg.sets.definitions.thb.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantAdditionalLandDrop
import com.wingedsheep.sdk.scripting.GrantSubtype
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Dryad of the Ilysian Grove — Theros Beyond Death #169 (canonical printing)
 * {2}{G} · Enchantment Creature — Nymph Dryad · 2/4
 *
 * You may play an additional land on each of your turns.
 * Lands you control are every basic land type in addition to their other types.
 *
 * The extra land drop is [GrantAdditionalLandDrop] (cumulative across sources, per the ruling).
 * The land types are Leyline of the Guildpact's five layer-4 [GrantSubtype]s over lands you
 * control; the engine derives each basic land type's intrinsic mana ability from the projected
 * subtypes, and names, supertypes and other subtypes are untouched.
 */
val DryadOfTheIlysianGrove = card("Dryad of the Ilysian Grove") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Enchantment Creature — Nymph Dryad"
    power = 2
    toughness = 4
    oracleText = "You may play an additional land on each of your turns.\n" +
        "Lands you control are every basic land type in addition to their other types."

    staticAbility {
        ability = GrantAdditionalLandDrop()
    }

    val landsYouControl = GroupFilter(GameObjectFilter.Land.youControl())
    listOf("Plains", "Island", "Swamp", "Mountain", "Forest").forEach { subtype ->
        staticAbility {
            ability = GrantSubtype(subtype, landsYouControl)
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "169"
        artist = "Scott Murphy"
        flavorText = "\"Vine-wreathed, he nurtures life among the dead.\"\n—Psemilla, Meletian poet"
        imageUri = "https://cards.scryfall.io/normal/front/6/d/6d964876-194b-49f1-8e74-cfe9269f2c62.jpg?1783931541"
        ruling("2020-01-24", "Dryad of the Ilysian Grove’s first ability is cumulative if you control more than one. It’s also cumulative with other effects that let you play additional lands, such as the one from Escape to the Wilds.")
        ruling("2020-01-24", "Each land you control will have the land types Plains, Island, Swamp, Mountain, and Forest. They’ll also have the mana ability of each basic land type (for example, Forests have “{T}: Add {G}.”). They’ll still have their other subtypes and abilities.")
        ruling("2020-01-24", "Giving a land additional basic land types doesn’t change its name or whether it’s legendary or basic.")
    }
}
