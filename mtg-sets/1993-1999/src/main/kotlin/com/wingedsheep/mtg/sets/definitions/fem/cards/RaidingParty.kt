package com.wingedsheep.mtg.sets.definitions.fem.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.times
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Raiding Party
 * {2}{R}
 * Enchantment
 * This enchantment can't be the target of white spells or abilities from white sources.
 * Sacrifice an Orc: Each player may tap any number of untapped white creatures they control. For
 * each creature tapped this way, that player chooses up to two Plains. Then destroy all Plains that
 * weren't chosen this way by any player.
 *
 * The set's most elaborate card, and it is entirely a bookkeeping problem: every player gets their
 * own tap-and-spare decision, the spared Plains have to *accumulate* across players, and what is
 * destroyed is the complement of that accumulated set — including Plains belonging to players who
 * spared none.
 *
 * Three pipeline pieces carry it: [ForEachPlayerCollectingEffect] runs a fresh sub-pipeline per
 * player in APNAP order and appends each player's picks into one shared collection; the
 * spare-count is the selection's own `_count` doubled; and a `FilterCollectionEffect` over
 * `CollectionFilter.ExcludeOtherCollection` takes the battlefield's Plains *minus* everything spared.
 *
 * Note "up to two Plains" is not "up to two of your Plains" — a player may spare an opponent's, and
 * a player who taps nothing simply spares nothing.
 */
val RaidingParty = card("Raiding Party") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Enchantment"
    oracleText = "This enchantment can't be the target of white spells or abilities from white sources.\n" +
        "Sacrifice an Orc: Each player may tap any number of untapped white creatures they " +
        "control. For each creature tapped this way, that player chooses up to two Plains. Then " +
        "destroy all Plains that weren't chosen this way by any player."

    keywordAbility(KeywordAbility.hexproofFrom(Color.WHITE))

    activatedAbility {
        cost = Costs.Sacrifice(GameObjectFilter.Permanent.withSubtype(Subtype.ORC))
        effect = Effects.Pipeline {
            val (allSpared) = forEachPlayerCollecting(Player.ActivePlayerFirst) {
                val tappable = gather(
                    CardSource.ControlledPermanents(
                        player = Player.You,
                        filter = GameObjectFilter.Creature.withColor(Color.WHITE).untapped()
                    )
                )
                val tapped = chooseAnyNumber(
                    from = tappable,
                    chooser = Chooser.Controller,
                    useTargetingUI = true,
                    prompt = "Tap any number of untapped white creatures you control (each spares two Plains)"
                )
                run(Effects.ForEachInCollection(tapped, Effects.Tap(EffectTarget.IterationEntity)))
                val plains = gather(
                    CardSource.BattlefieldMatching(filter = GameObjectFilter.Land.withSubtype(Subtype.PLAINS))
                )
                val spared = chooseUpTo(
                    tapped.count * 2,
                    from = plains,
                    chooser = Chooser.Controller,
                    useTargetingUI = true,
                    prompt = "Choose up to two Plains for each creature you tapped"
                )
                listOf(spared)
            }
            // Everything left over — the complement of what every player spared between them.
            val allPlains = gather(
                CardSource.BattlefieldMatching(filter = GameObjectFilter.Land.withSubtype(Subtype.PLAINS))
            )
            destroy(exclude(allPlains, allSpared))
        }
        description = "Sacrifice an Orc: Each player may tap any number of untapped white creatures they control. For each creature tapped this way, that player chooses up to two Plains. Then destroy all Plains that weren't chosen this way by any player."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "64"
        artist = "Quinton Hoover"
        imageUri = "https://cards.scryfall.io/normal/front/9/0/907a3396-706b-4ca2-9973-bca758986032.jpg?1783947890"
    }
}
