package com.wingedsheep.mtg.sets.definitions.m12.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.grantedTriggeredAbility
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersAsCopy
import com.wingedsheep.sdk.scripting.effects.CopyExceptions
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Phantasmal Image
 * {1}{U}
 * Creature — Illusion
 * 0/0
 * You may have this creature enter as a copy of any creature on the battlefield, except it's an
 * Illusion in addition to its other types and it has "When this creature becomes the target of a
 * spell or ability, sacrifice it."
 *
 * Clone's [EntersAsCopy] with two copy exceptions (CR 707.9b): the Illusion subtype is added on top
 * of the copied type line (CR 205.1b) and Skulking Ghost's self-sacrifice trigger is added as
 * copiable rules text. Both are copiable values (2017-03-14 ruling), so a Clone of the Image is an
 * Illusion with the trigger as well. Declining the copy leaves a 0/0 Illusion with no trigger.
 */
val PhantasmalImage = card("Phantasmal Image") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Illusion"
    power = 0
    toughness = 0
    oracleText = "You may have this creature enter as a copy of any creature on the battlefield, " +
        "except it's an Illusion in addition to its other types and it has \"When this creature " +
        "becomes the target of a spell or ability, sacrifice it.\""

    replacementEffect(
        EntersAsCopy(
            optional = true,
            exceptions = CopyExceptions(
                addedSubtypes = setOf(Subtype("Illusion")),
                addedTriggeredAbilities = listOf(
                    grantedTriggeredAbility {
                        trigger = Triggers.self.becomesTarget()
                        effect = Effects.SacrificeTarget(EffectTarget.Self)
                        description = "When this creature becomes the target of a spell or ability, sacrifice it."
                    }
                ),
            ),
        )
    )

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "72"
        artist = "Nils Hamm"
        imageUri = "https://cards.scryfall.io/normal/front/9/8/98e7bf8f-dba7-4005-8cee-634c9153931d.jpg?1783941088"
        ruling("2017-03-14", "The Illusion creature type and the triggered ability that Phantasmal Image gains as part of its copy effect are both copiable values that other effects may copy.")
        ruling("2017-03-14", "If the chosen creature is copying something else (for example, if the chosen creature is another Phantasmal Image), then Phantasmal Image enters the battlefield as whatever the chosen creature copied.")
        ruling("2017-03-14", "If the chosen creature is a token, Phantasmal Image copies the original characteristics of that token as stated by the effect that created the token. Phantasmal Image is not a token in this case.")
        ruling("2017-03-14", "If Phantasmal Image somehow enters the battlefield at the same time as another creature, Phantasmal Image can't become a copy of that creature. You may choose only a creature that's already on the battlefield.")
    }
}
