package com.wingedsheep.mtg.sets.definitions.mat.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.IncrementAbilityResolutionCountEffect

/**
 * Nissa, Resurgent Animist — March of the Machine: The Aftermath #22
 * {2}{G} · Legendary Creature — Elf Scout · 3 / 3
 *
 * Landfall — Whenever a land you control enters, add one mana of any color. Then if this is the
 * second time this ability has resolved this turn, reveal cards from the top of your library until
 * you reveal an Elf or Elemental card. Put that card into your hand and the rest on the bottom of
 * your library in a random order.
 *
 * The mana is added every resolution; the reveal rides the Harvestrite Host resolution count
 * ([IncrementAbilityResolutionCountEffect] then [Conditions.SourceAbilityResolvedNTimes] (2)), so
 * only the second resolution in a turn digs. The dig is Wirewood Herald's
 * `revealUntilMatchToHand` — rest to the bottom in a random order, and a whole library with no
 * match goes back randomized (ruling). The trigger is not a mana ability; it uses the stack.
 */
val NissaResurgentAnimist = card("Nissa, Resurgent Animist") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Legendary Creature — Elf Scout"
    power = 3
    toughness = 3
    oracleText = "Landfall — Whenever a land you control enters, add one mana of any color. Then if this is " +
        "the second time this ability has resolved this turn, reveal cards from the top of your library " +
        "until you reveal an Elf or Elemental card. Put that card into your hand and the rest on the " +
        "bottom of your library in a random order."

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Land.youControl()).enters()
        effect = Effects.AddAnyColorMana(1) then
            IncrementAbilityResolutionCountEffect then
            Effects.If(
                condition = Conditions.SourceAbilityResolvedNTimes(2),
                then = Patterns.Library.revealUntilMatchToHand(
                    filter = GameObjectFilter.Any.withAnySubtype("Elf", "Elemental")
                )
            )
        description = "Landfall — Whenever a land you control enters, add one mana of any color. Then if " +
            "this is the second time this ability has resolved this turn, reveal cards from the top of " +
            "your library until you reveal an Elf or Elemental card. Put that card into your hand and the " +
            "rest on the bottom of your library in a random order."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "22"
        artist = "Tuan Duong Chu"
        imageUri = "https://cards.scryfall.io/normal/front/2/4/248c76d3-b5cb-4582-be17-7cd1d0cb0f58.jpg?1783916520"

        ruling("2023-05-12", "Nissa's landfall ability isn't a mana ability, even though it causes you to add mana. It uses the stack and can be responded to.")
        ruling("2023-05-12", "Each time the landfall ability resolves in a turn after the second time, you'll just add mana.")
        ruling(
            "2023-05-12",
            "If you reveal your entire library and don't reveal an Elf or Elemental card, you'll put all revealed " +
                "cards back in a random order. This uses the same physical action as shuffling, although it isn't " +
                "technically a \"shuffle.\""
        )
    }
}
