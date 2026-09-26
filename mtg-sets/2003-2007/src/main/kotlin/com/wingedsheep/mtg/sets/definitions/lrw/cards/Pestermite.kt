package com.wingedsheep.mtg.sets.definitions.lrw.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.Mode
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Pestermite
 * {2}{U}
 * Creature — Faerie Rogue
 * 2/1
 * Flash
 * Flying
 * When this creature enters, you may tap or untap target permanent.
 *
 * Flash plus the untap half is the card's real line: cast it in your own upkeep to untap a land, or
 * during combat to tap a blocker. The target is chosen when the ETB trigger goes on the stack; the
 * tap-or-untap choice is made on resolution, so an opponent who taps the permanent in response
 * doesn't lock you into the now-useless half — the [GraniteWitness] idiom, a [Effects.May] over a
 * two-[Mode] [ModalEffect] with `countsAsModalSpell = false` so the choice isn't read as a modal
 * *spell*.
 *
 * "Target permanent", not "target creature": Pestermite untaps lands too, which is what makes it
 * half of the Splinter Twin combo.
 */
val Pestermite = card("Pestermite") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Faerie Rogue"
    power = 2
    toughness = 1
    oracleText = "Flash\n" +
        "Flying\n" +
        "When this creature enters, you may tap or untap target permanent."

    keywords(Keyword.FLASH, Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.enters()
        val permanent = target(TargetFilter.Permanent)
        effect = Effects.May(
            Effects.Modal(
                modes = listOf(
                    Mode.noTarget(Effects.Tap(permanent), "Tap that permanent"),
                    Mode.noTarget(Effects.Untap(permanent), "Untap that permanent")
                ),
                chooseCount = 1,
                countsAsModalSpell = false
            )
        )
        description = "When this creature enters, you may tap or untap target permanent."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "78"
        artist = "Christopher Moeller"
        flavorText = "The fae know when they're not wanted. That's precisely why they show up."
        imageUri = "https://cards.scryfall.io/normal/front/f/2/f252ae53-443c-4a27-b8f0-639a9a2b8598.jpg?1783942899"
    }
}
