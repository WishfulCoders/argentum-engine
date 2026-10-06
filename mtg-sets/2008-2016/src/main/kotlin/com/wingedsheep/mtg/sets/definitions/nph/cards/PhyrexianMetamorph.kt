package com.wingedsheep.mtg.sets.definitions.nph.cards

import com.wingedsheep.sdk.core.CardType
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersAsCopy
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CopyExceptions

/**
 * Phyrexian Metamorph
 * {3}{U/P}
 * Artifact Creature — Phyrexian Shapeshifter
 * 0/0
 * ({U/P} can be paid with either {U} or 2 life.)
 * You may have this creature enter as a copy of any artifact or creature on the battlefield,
 * except it's an artifact in addition to its other types.
 *
 * Clone's [EntersAsCopy] widened to "artifact or creature", with the "artifact in addition to its
 * other types" exception carried in the shared [CopyExceptions] vocabulary (CR 707.9b / 205.1b):
 * it is a copiable value, so a later copy of the Metamorph is an artifact too. Copying a
 * noncreature artifact leaves it a noncreature artifact (ruling); declining the copy leaves a 0/0
 * artifact creature that state-based actions put into the graveyard.
 */
val PhyrexianMetamorph = card("Phyrexian Metamorph") {
    manaCost = "{3}{U/P}"
    colorIdentity = "U"
    typeLine = "Artifact Creature — Phyrexian Shapeshifter"
    power = 0
    toughness = 0
    oracleText = "({U/P} can be paid with either {U} or 2 life.)\n" +
        "You may have this creature enter as a copy of any artifact or creature on the battlefield, " +
        "except it's an artifact in addition to its other types."

    replacementEffect(
        EntersAsCopy(
            optional = true,
            copyFilter = GameObjectFilter.Artifact or GameObjectFilter.Creature,
            exceptions = CopyExceptions(addedCardTypes = setOf(CardType.ARTIFACT)),
        )
    )

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "42"
        artist = "Jana Schirmer & Johannes Voss"
        imageUri = "https://cards.scryfall.io/normal/front/d/2/d2e27911-87cb-49a0-a34f-6afe4bddd592.jpg?1783941318"
        ruling("2011-06-01", "If the chosen permanent is a token, Phyrexian Metamorph copies the original characteristics of that token as stated by the effect that put the token onto the battlefield, except it's also an artifact. Phyrexian Metamorph is not a token.")
        ruling("2011-06-01", "If Phyrexian Metamorph copies a noncreature artifact, it is no longer a creature.")
        ruling("2011-06-01", "If the chosen creature is copying something else (for example, if the chosen creature is a Clone), then your Phyrexian Metamorph enters as whatever the chosen creature copied, except it's also an artifact.")
        ruling("2011-06-01", "You can choose not to copy anything. In that case, Phyrexian Metamorph simply enters as a 0/0 artifact creature and is put into its owner's graveyard as a state-based action (unless something else is raising its toughness).")
    }
}
