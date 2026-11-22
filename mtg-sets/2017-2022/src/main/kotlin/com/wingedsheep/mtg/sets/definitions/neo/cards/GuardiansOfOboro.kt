package com.wingedsheep.mtg.sets.definitions.neo.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CanAttackDespiteDefender
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.predicates.StatePredicate

/**
 * Guardians of Oboro — Kamigawa: Neon Dynasty #56 (canonical printing)
 * {2}{U} · Creature — Moonfolk Samurai · 3/4
 *
 * Defender. Modified creatures you control can attack as though they didn't have defender.
 *
 * The lord form of [CanAttackDespiteDefender] (Ghalta the Immovable's shape) narrowed to
 * [StatePredicate.IsModified] (CR 700.9), so the Guardians themselves can attack once modified.
 */
val GuardiansOfOboro = card("Guardians of Oboro") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Moonfolk Samurai"
    oracleText = "Defender\n" +
        "Modified creatures you control can attack as though they didn't have defender. " +
        "(Equipment, Auras you control, and counters are modifications.)"
    power = 3
    toughness = 4

    keywords(Keyword.DEFENDER)

    staticAbility {
        ability = CanAttackDespiteDefender(
            filter = GroupFilter(
                GameObjectFilter.Creature.youControl().withStatePredicate(StatePredicate.IsModified)
            ),
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "56"
        artist = "Anna Steinbauer"
        flavorText = "\"The palace is closed to outsiders.\""
        imageUri = "https://cards.scryfall.io/normal/front/e/9/e9c44d4e-742f-4e4f-9bf4-921075bc427c.jpg?1783923904"
        ruling("2022-02-18", "An Aura controlled by an opponent does not cause a creature you control to be modified.")
        ruling("2022-02-18", "A creature with a counter on it is considered modified no matter what kind of counter it is or which player put it on that creature.")
        ruling("2022-02-18", "A creature that is equipped is considered modified no matter who controls the Equipment that's attached to it.")
    }
}
