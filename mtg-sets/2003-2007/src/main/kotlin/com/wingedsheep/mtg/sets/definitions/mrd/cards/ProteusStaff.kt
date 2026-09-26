package com.wingedsheep.mtg.sets.definitions.mrd.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Proteus Staff
 * {3}
 * Artifact
 *
 * {2}{U}, {T}: Put target creature on the bottom of its owner's library. That creature's
 * controller reveals cards from the top of their library until they reveal a creature card. The
 * player puts that card onto the battlefield and the rest on the bottom of their library in any
 * order. Activate only as a sorcery.
 *
 * Every clause after the bottoming acts on *that creature's controller*, not on the Staff's, so
 * the whole reveal runs inside a one-player [Effects.ForEachPlayer] over [Player.ControllerOf].
 * That rebinds the resolution context's controller, which is what makes `Player.You` mean the
 * target's controller for the library being revealed *and* routes the "in any order" prompt to
 * them rather than to the Staff's controller. The reference itself stays correct even though the
 * creature has already left the battlefield: the engine falls back to the entity's
 * last-known-permanent snapshot.
 *
 * Inside that scope the reveal is the `revealUntilMatchToHand` shape with a different landing
 * zone: gather until the first creature card, reveal the whole pile, then split it — the single
 * match onto the battlefield, everything revealed before it to the bottom of the library, under
 * the target itself since it was bottomed first. That ordering is also why a library holding no
 * *other* creature card finds the bottomed target and puts it straight back.
 */
val ProteusStaff = card("Proteus Staff") {
    manaCost = "{3}"
    colorIdentity = "U"
    typeLine = "Artifact"
    oracleText = "{2}{U}, {T}: Put target creature on the bottom of its owner's library. That " +
        "creature's controller reveals cards from the top of their library until they reveal a " +
        "creature card. The player puts that card onto the battlefield and the rest on the bottom " +
        "of their library in any order. Activate only as a sorcery."

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}{U}"), Costs.Tap)
        val creature = target(TargetFilter.Creature)
        effect = Effects.PutOnBottomOfLibrary(creature) then
            Effects.ForEachPlayer(
                Player.ControllerOf("that creature"),
                Effects.Pipeline {
                    val (_, proteusRevealed) = gatherUntilMatch(GameObjectFilter.Creature, player = Player.You)
                    reveal(proteusRevealed)
                    val (proteusCreature, proteusRest) = filterSplit(proteusRevealed, GameObjectFilter.Creature)
                    move(proteusCreature, CardDestination.ToZone(Zone.BATTLEFIELD, Player.You))
                    toLibraryBottom(proteusRest)
                }
            )
        timing = TimingRule.SorcerySpeed
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "230"
        artist = "Trevor Hairsine"
        imageUri = "https://cards.scryfall.io/normal/front/5/5/55e5c69c-43d0-4f5b-8720-40074eb122bb.jpg?1783944506"
    }
}
