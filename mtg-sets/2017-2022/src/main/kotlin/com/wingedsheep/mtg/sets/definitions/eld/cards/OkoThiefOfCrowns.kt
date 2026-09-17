package com.wingedsheep.mtg.sets.definitions.eld.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.RemoveAllAbilitiesEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetPermanent

/**
 * Oko, Thief of Crowns
 * {1}{G}{U}
 * Legendary Planeswalker — Oko
 * Loyalty 4
 * +2: Create a Food token.
 * +1: Target artifact or creature loses all abilities and becomes a green Elk creature with base
 * power and toughness 3/3.
 * −5: Exchange control of target artifact or creature you control and target creature an opponent
 * controls with power 3 or less.
 *
 * The +1 is Lizard, Connors's Curse's shape — lose all abilities, then become a creature with a
 * set base P/T, colour and creature type, both for good (it outlasts Oko and the turn). Per the
 * rulings it is "just a green Elk": it also loses every other card type (an artifact stops being
 * one) while keeping its supertypes, and abilities or P/T changes applied later still apply. The
 * −5 is Trade the Helm's exchange; if either target is illegal on resolution, nothing is exchanged.
 */
val OkoThiefOfCrowns = card("Oko, Thief of Crowns") {
    manaCost = "{1}{G}{U}"
    colorIdentity = "GU"
    typeLine = "Legendary Planeswalker — Oko"
    startingLoyalty = 4
    oracleText = "+2: Create a Food token. (It's an artifact with \"{2}, {T}, Sacrifice this token: You gain 3 life.\")\n" +
        "+1: Target artifact or creature loses all abilities and becomes a green Elk creature with base power and toughness 3/3.\n" +
        "−5: Exchange control of target artifact or creature you control and target creature an opponent controls with power 3 or less."

    loyaltyAbility(2) {
        effect = Effects.CreateFood()
    }

    loyaltyAbility(1) {
        val t = target("target artifact or creature", TargetPermanent(filter = TargetFilter(GameObjectFilter.CreatureOrArtifact)))
        effect = Effects.Composite(
            RemoveAllAbilitiesEffect(t, Duration.Permanent),
            Effects.BecomeCreature(
                target = t,
                power = 3,
                toughness = 3,
                creatureTypes = setOf("Elk"),
                removeTypes = setOf("ARTIFACT", "ENCHANTMENT", "LAND", "PLANESWALKER", "BATTLE", "KINDRED"),
                colors = setOf("GREEN"),
                duration = Duration.Permanent
            )
        )
    }

    loyaltyAbility(-5) {
        val yours = target(
            "target artifact or creature you control",
            TargetPermanent(filter = TargetFilter(GameObjectFilter.CreatureOrArtifact.youControl()))
        )
        val theirs = target(
            "target creature an opponent controls with power 3 or less",
            TargetPermanent(filter = TargetFilter(GameObjectFilter.Creature.opponentControls().powerAtMost(3)))
        )
        effect = Effects.ExchangeControl(yours, theirs)
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "197"
        artist = "Yongjae Choi"
        imageUri = "https://cards.scryfall.io/normal/front/3/4/3462a3d0-5552-49fa-9eb7-100960c55891.jpg?1783932594"
        ruling("2019-10-04", "Oko's second ability overwrites all previous effects that set the creature's base power and toughness to specific values. Any power- or toughness-setting effects that start to apply after Oko's second ability resolves will overwrite this effect.")
        ruling("2019-10-04", "Effects that modify a creature's power and/or toughness, such as the effect of Festive Funeral, will apply to the creature no matter when they started to take effect. The same is true for any counters that change its power and/or toughness.")
        ruling("2019-10-04", "Oko's second ability overwrites all colors and creature types the affected creature has. It's just a green Elk. The creature keeps any supertypes (such as legendary) it has, but loses any other card types it has (such as artifact).")
        ruling("2019-10-04", "The effects of Oko's second ability lasts indefinitely. It doesn't expire during the cleanup step or if you or Oko leave the game.")
        ruling("2019-10-04", "If either of the target permanents is an illegal target when Oko's last ability resolves, the exchange won't happen.")
        ruling("2019-10-04", "Gaining control of a permanent doesn't cause you to gain control of any Auras or Equipment attached to it. Gaining control of an Equipment doesn't cause it to become unattached, although its new controller may activate its equip abilities during their main phase.")
        ruling("2019-10-04", "Oko's second ability may target a permanent that is only temporarily an artifact or a creature, such as Oko, the Trickster. If this happens, the effect causes that permanent to remain a green Elk creature even after the temporary effect expires.")
        ruling("2019-10-04", "The effects of Oko's third ability lasts indefinitely. It doesn't expire during the cleanup step or if the creature you take has its power raised above 3 later.")
        ruling("2019-10-04", "If the affected creature gains an ability after Oko's second ability resolves, it will keep that ability.")
    }
}
