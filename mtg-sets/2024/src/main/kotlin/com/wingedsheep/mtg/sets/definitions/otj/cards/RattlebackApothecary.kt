package com.wingedsheep.mtg.sets.definitions.otj.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.effects.Mode
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Rattleback Apothecary
 * {2}{B}
 * Creature — Gorgon Warlock
 * 3/2
 *
 * Deathtouch
 * Whenever you commit a crime, target creature you control gains your choice of menace or lifelink
 * until end of turn.
 *
 * The crime trigger (`Triggers.you.commitsCrime()`) targets a creature you control and offers a
 * [ModalEffect.chooseOne] between two [GrantKeywordEffect]s — the same "your choice of keyword X or Y"
 * shape as Manifold Mouse. Each mode grants its keyword to the chosen creature (ContextTarget(0)) for
 * `Duration.EndOfTurn`. The crime-this-turn tracker is read at the engine's `CrimeDetector` emit site;
 * this card only consumes `Triggers.you.commitsCrime()`.
 */
val RattlebackApothecary = card("Rattleback Apothecary") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Gorgon Warlock"
    power = 3
    toughness = 2
    oracleText = "Deathtouch\n" +
        "Whenever you commit a crime, target creature you control gains your choice of menace or " +
        "lifelink until end of turn. (Targeting opponents, anything they control, and/or cards in " +
        "their graveyards is a crime.)"

    keywords(Keyword.DEATHTOUCH)

    triggeredAbility {
        trigger = Triggers.you.commitsCrime()
        val t = target(TargetFilter.Creature.youControl())
        effect = ModalEffect.chooseOne(
            Mode.noTarget(Effects.GrantKeyword(Keyword.MENACE, t, Duration.EndOfTurn), "Menace"),
            Mode.noTarget(Effects.GrantKeyword(Keyword.LIFELINK, t, Duration.EndOfTurn), "Lifelink")
        )
        description = "Whenever you commit a crime, target creature you control gains your choice of " +
            "menace or lifelink until end of turn."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "100"
        artist = "Loïc Canavaggia"
        flavorText = "\"Looking for a little liquid courage? I also stock liquid murder, if that's what you're after.\""
        imageUri = "https://cards.scryfall.io/normal/front/9/a/9a88e233-f09c-49e7-b1e3-386fba851fdf.jpg?1712355645"
    }
}
