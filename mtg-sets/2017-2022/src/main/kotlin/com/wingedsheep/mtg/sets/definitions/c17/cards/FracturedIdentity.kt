package com.wingedsheep.mtg.sets.definitions.c17.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Fractured Identity
 * {3}{W}{U}
 * Sorcery
 * Exile target nonland permanent. Each player other than its controller creates a token that's a
 * copy of it.
 *
 * The exile happens first, then the copies (CR 608.2c — instructions in order), so the copies enter
 * a battlefield the original has already left. Two last-known-information reads (CR 608.2h) make
 * that order work:
 *
 *  - "its controller" is [Player.ControllerOf] the exiled target, read off its last-known
 *    controller, and [Player.EachOtherThan] iterates everyone else in APNAP order — you included
 *    when you exile an opponent's permanent, only your opponents when you exile your own.
 *  - "a copy of it" copies the permanent as it last existed on the battlefield (rulings: a copy of
 *    whatever it was copying; a token's original characteristics), which
 *    [Effects.CreateTokenCopyOfTarget] reads for a permanent target that has left.
 *
 * Inside the per-player loop the controller is rebound to the iterated player, so each of them
 * creates — and controls — their own token.
 */
val FracturedIdentity = card("Fractured Identity") {
    manaCost = "{3}{W}{U}"
    colorIdentity = "WU"
    typeLine = "Sorcery"
    oracleText = "Exile target nonland permanent. Each player other than its controller creates a " +
        "token that's a copy of it."

    spell {
        val permanent = target(TargetFilter.NonlandPermanent)
        effect = Effects.Exile(permanent) then
            Effects.ForEachPlayer(
                Player.EachOtherThan(Player.ControllerOf("target")),
                Effects.CreateTokenCopyOfTarget(permanent),
            )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "37"
        artist = "Yongjae Choi"
        flavorText = "Self-reflection is not without its perils."
        imageUri = "https://cards.scryfall.io/normal/front/b/2/b2f73f5d-1aad-48c2-9e74-5f7bdd87900f.jpg?1783935938"
        ruling("2017-08-25", "The tokens copy exactly what was printed on the permanent and nothing else (unless that permanent was copying something else or was a token; see below). They don't copy whether that permanent was tapped or untapped, whether it had any counters on it or Auras and/or Equipment attached to it, or any non-copy effects that changed its power, toughness, types, color, and so on.")
        ruling("2017-08-25", "If the copied permanent had {X} in its mana cost, X is 0.")
        ruling("2017-08-25", "If the copied permanent was copying something else, the tokens enter the battlefield as whatever that permanent was copying.")
        ruling("2017-08-25", "If the copied permanent is a token, the tokens created by Fractured Identity copy the original characteristics of that token as stated by the effect that put it onto the battlefield.")
        ruling("2017-08-25", "Any enters-the-battlefield abilities of the copied permanent will trigger when the tokens enter the battlefield. Any \"As [this permanent] enters the battlefield\" or \"[This permanent] enters the battlefield with\" abilities of the copied permanent will also work.")
    }
}
