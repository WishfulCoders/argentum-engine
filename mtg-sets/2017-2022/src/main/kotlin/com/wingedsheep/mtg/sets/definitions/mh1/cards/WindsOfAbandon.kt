package com.wingedsheep.mtg.sets.definitions.mh1.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Winds of Abandon
 * {1}{W}
 * Sorcery
 * Exile target creature you don't control. For each creature exiled this way, its controller
 * searches their library for a basic land card. Those players put those cards onto the
 * battlefield tapped, then shuffle.
 * Overload {4}{W}{W}
 *
 * Unlike Path to Exile the search is mandatory ("searches", not "may search"); a search may still
 * fail to find. Single-target: the exiled creature's controller (last-known) searches their own
 * library. Overloaded: every opponent exiles the creatures they control and searches for that
 * many basic lands; an opponent who lost no creatures doesn't search or shuffle. The overload
 * loop runs player by player, which is identical to the printed simultaneous exile in a two-player
 * game.
 */
val WindsOfAbandon = card("Winds of Abandon") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Sorcery"
    oracleText = "Exile target creature you don't control. For each creature exiled this way, its controller " +
        "searches their library for a basic land card. Those players put those cards onto the battlefield " +
        "tapped, then shuffle.\nOverload {4}{W}{W} (You may cast this spell for its overload cost. If you do, " +
        "change \"target\" in its text to \"each.\")"

    keywordAbility(KeywordAbility.overload("{4}{W}{W}"))

    spell {
        val creature = target(TargetFilter.CreatureOpponentControls)
        effect = Effects.Exile(creature) then Effects.Pipeline {
            val searchable = gather(
                CardSource.FromZone(
                    zone = Zone.LIBRARY,
                    player = Player.ControllerOf("target"),
                    filter = GameObjectFilter.BasicLand,
                ),
                search = true
            )
            val found = chooseUpTo(1, from = searchable, chooser = Chooser.ControllerOfTarget)
            move(
                found,
                CardDestination.ToZone(
                    zone = Zone.BATTLEFIELD,
                    player = Player.ControllerOf("target"),
                    placement = ZonePlacement.Tapped,
                )
            )
            run(Effects.ShuffleLibrary(target = EffectTarget.TargetController))
        }

        overloadEffect = Effects.ForEachPlayer(
            players = Player.EachOpponent,
            Effects.Pipeline {
                val theirs = gather(CardSource.ControlledPermanents(Player.You, GameObjectFilter.Creature))
                exile(theirs)
                ifNotEmpty(theirs) {
                    val searchable = gather(
                        CardSource.FromZone(Zone.LIBRARY, Player.You, GameObjectFilter.BasicLand),
                        search = true
                    )
                    val found = chooseUpTo(
                        DynamicAmounts.distinctEntitiesIn(theirs),
                        from = searchable,
                        chooser = Chooser.Controller,
                        prompt = "Search your library for a basic land card for each creature exiled"
                    )
                    move(found, CardDestination.ToZone(Zone.BATTLEFIELD, Player.You, ZonePlacement.Tapped))
                    run(Effects.ShuffleLibrary(EffectTarget.PlayerRef(Player.You)))
                }
            }
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "37"
        artist = "Noah Bradley"
        imageUri = "https://cards.scryfall.io/normal/front/3/b/3bb17913-fe4d-4acd-9b75-71f5a90f898b.jpg?1783933150"
        ruling("2019-06-14", "If a creature is exiled but ends up in another zone, it's still a \"creature exiled this way\" for Winds of Abandon.")
        ruling("2024-01-12", "Because a spell with overload doesn't target when its overload cost is paid, it may affect permanents with hexproof or with protection from the appropriate color.")
    }
}
