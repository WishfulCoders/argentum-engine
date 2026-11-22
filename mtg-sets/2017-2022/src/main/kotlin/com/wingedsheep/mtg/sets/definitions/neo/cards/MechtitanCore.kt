package com.wingedsheep.mtg.sets.definitions.neo.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.scripting.effects.CREATED_TOKENS
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Mechtitan Core — Kamigawa: Neon Dynasty #249 (canonical printing)
 * {2} · Artifact — Vehicle · 2/4
 *
 * {5}, Exile this Vehicle and four other artifact creatures and/or Vehicles you control: Create
 * Mechtitan, a legendary 10/10 Construct artifact creature token with flying, vigilance, trample,
 * lifelink, and haste that's all colors. When that token leaves the battlefield, return all cards
 * exiled with this Vehicle except this card to the battlefield tapped under their owners' control.
 * Crew 2
 *
 * The four other permanents the cost exiled are filed in the Mechtitan token's own linked-exile
 * pile, and the token's leaves trigger returns that pile — exactly "all cards exiled with this
 * Vehicle except this card", since the Core is the only other card the ability exiles. A token
 * exiled as part of the cost ceases to exist and has nothing to return.
 */
val MechtitanCore = card("Mechtitan Core") {
    manaCost = "{2}"
    colorIdentity = ""
    typeLine = "Artifact — Vehicle"
    power = 2
    toughness = 4
    oracleText = "{5}, Exile this Vehicle and four other artifact creatures and/or Vehicles you " +
        "control: Create Mechtitan, a legendary 10/10 Construct artifact creature token with " +
        "flying, vigilance, trample, lifelink, and haste that's all colors. When that token leaves " +
        "the battlefield, return all cards exiled with this Vehicle except this card to the " +
        "battlefield tapped under their owners' control.\nCrew 2"

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{5}"),
            Costs.ExileSelf,
            Costs.ExilePermanentsFixed(
                count = 4,
                filter = (GameObjectFilter.ArtifactCreature or
                    GameObjectFilter.Permanent.withSubtype(Subtype.VEHICLE)).youControl(),
                excludeSelf = true,
            ),
        )
        effect = Effects.Pipeline {
            val exiled = gather(CardSource.ExiledAsCost)
            run(
                Effects.CreateToken(
                    power = 10,
                    toughness = 10,
                    colors = Color.entries.toSet(),
                    creatureTypes = setOf("Construct"),
                    keywords = setOf(
                        Keyword.FLYING, Keyword.VIGILANCE, Keyword.TRAMPLE, Keyword.LIFELINK, Keyword.HASTE
                    ),
                    name = "Mechtitan",
                    legendary = true,
                    artifactToken = true,
                    imageUri = "https://cards.scryfall.io/normal/front/0/2/0281825d-9d38-4b56-b522-a8d3cf8c77c9.jpg?1783923710",
                    triggeredAbilities = listOf(
                        TriggeredAbility.create(
                            trigger = Triggers.self.leaves(),
                            effect = Effects.Pipeline {
                                val pile = gather(CardSource.FromLinkedExile())
                                move(
                                    pile,
                                    CardDestination.ToZone(Zone.BATTLEFIELD, placement = ZonePlacement.Tapped),
                                    underOwnersControl = true,
                                )
                            },
                            descriptionOverride = "When this token leaves the battlefield, return all cards " +
                                "exiled with Mechtitan Core except Mechtitan Core to the battlefield " +
                                "tapped under their owners' control."
                        )
                    ),
                )
            )
            run(Effects.LinkCollectionToPermanent(exiled, EffectTarget.PipelineTarget(CREATED_TOKENS)))
        }
        description = "{5}, Exile this Vehicle and four other artifact creatures and/or Vehicles you " +
            "control: Create Mechtitan, a legendary 10/10 Construct artifact creature token with " +
            "flying, vigilance, trample, lifelink, and haste that's all colors."
    }

    keywordAbility(KeywordAbility.crew(2))

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "249"
        artist = "Victor Adame Minguez"
        imageUri = "https://cards.scryfall.io/normal/front/c/a/ca286ba9-01a7-4dce-b388-177f5dcb95b1.jpg?1783923826"
    }
}
