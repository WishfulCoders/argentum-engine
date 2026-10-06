package com.wingedsheep.mtg.sets.definitions.bro.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Gix's Command
 * {3}{B}{B}
 * Sorcery
 * Choose two —
 * • Put two +1/+1 counters on up to one creature. It gains lifelink until end of turn.
 * • Destroy each creature with power 2 or less.
 * • Return up to two creature cards from your graveyard to your hand.
 * • Each opponent sacrifices a creature with the greatest power among creatures they control.
 *
 * None of the modes target (2022-10-14 ruling), so the creature for the counters and the cards to
 * return are chosen as the spell resolves — a resolution-time `chooseUpTo` pipeline rather than a
 * cast-time `target(...)`. Modes resolve in printed order, so choosing the first two modes means
 * the countered creature is measured *after* its +2/+2 when "power 2 or less" is checked.
 */
val GixsCommand = card("Gix's Command") {
    manaCost = "{3}{B}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Choose two —\n" +
        "• Put two +1/+1 counters on up to one creature. It gains lifelink until end of turn.\n" +
        "• Destroy each creature with power 2 or less.\n" +
        "• Return up to two creature cards from your graveyard to your hand.\n" +
        "• Each opponent sacrifices a creature with the greatest power among creatures they control."

    spell {
        modal(chooseCount = 2) {
            mode("Put two +1/+1 counters on up to one creature; it gains lifelink until end of turn") {
                effect = Effects.Pipeline {
                    val creatures = gather(GameObjectFilter.Creature)
                    val chosen = chooseUpTo(
                        1,
                        from = creatures,
                        prompt = "Choose up to one creature to get two +1/+1 counters and lifelink",
                        useTargetingUI = true
                    )
                    run(Effects.AddCountersToCollection(chosen, CounterType.PLUS_ONE_PLUS_ONE, 2))
                    run(Effects.GrantKeyword(Keyword.LIFELINK, chosen.asTarget))
                }
            }
            mode("Destroy each creature with power 2 or less") {
                effect = Effects.DestroyAll(GameObjectFilter.Creature.powerAtMost(2))
            }
            mode("Return up to two creature cards from your graveyard to your hand") {
                effect = Effects.Pipeline {
                    val graveyard = gather(
                        CardSource.FromZone(
                            zone = Zone.GRAVEYARD,
                            player = Player.You,
                            filter = GameObjectFilter.Creature
                        )
                    )
                    val chosen = chooseUpTo(
                        2,
                        from = graveyard,
                        prompt = "Choose up to two creature cards to return to your hand",
                        selectedLabel = "Return to hand"
                    )
                    toHand(chosen)
                }
            }
            mode("Each opponent sacrifices a creature with the greatest power among creatures they control") {
                effect = Effects.Sacrifice(
                    GameObjectFilter.Creature.hasGreatestPower(),
                    1,
                    EffectTarget.PlayerRef(Player.EachOpponent)
                )
            }
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "97"
        artist = "Dominik Mayer"
        imageUri = "https://cards.scryfall.io/normal/front/9/6/9606de75-c25f-411b-a271-258ac5a60987.jpg?1783920087"
        ruling("2022-10-14", "Since none of the modes have targets, you don't have to choose which creature is getting the counters or which cards you're returning until Gix's Command resolves.")
    }
}
