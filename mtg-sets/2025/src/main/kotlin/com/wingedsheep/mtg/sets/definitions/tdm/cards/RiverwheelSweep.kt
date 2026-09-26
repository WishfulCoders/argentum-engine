package com.wingedsheep.mtg.sets.definitions.tdm.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.MayPlayExpiry
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Riverwheel Sweep
 * {2/U}{2/R}{2/W}
 * Sorcery
 *
 * Tap target creature. Put three stun counters on it.
 * (If a permanent with a stun counter would become untapped, remove one from it instead.)
 * Exile the top two cards of your library. Choose one of them. Until the end of your next
 * turn, you may play that card.
 *
 * The impulse half composes the standard Gather → Move(EXILE) → Select(one) → grant
 * may-play pipeline (see Fireglass Mentor); the only difference is the
 * [MayPlayExpiry.UntilEndOfNextTurn] window. The other exiled card stays exiled with no
 * play permission, matching "Choose one of them."
 */
val RiverwheelSweep = card("Riverwheel Sweep") {
    manaCost = "{2/U}{2/R}{2/W}"
    colorIdentity = "URW"
    typeLine = "Sorcery"
    oracleText = "Tap target creature. Put three stun counters on it. " +
        "(If a permanent with a stun counter would become untapped, remove one from it instead.)\n" +
        "Exile the top two cards of your library. Choose one of them. Until the end of your next turn, " +
        "you may play that card."

    spell {
        val creature = target(TargetFilter.Creature)
        effect = Effects.Tap(creature) then
            Effects.AddCounters(CounterType.STUN, 3, creature) then
            Effects.Pipeline {
                val exiled = gather(CardSource.TopOfLibrary(2))
                exile(exiled)
                val chosen = chooseExactly(
                    1,
                    from = exiled,
                    prompt = "Choose a card you may play until the end of your next turn"
                )
                run(Effects.GrantMayPlayFromExile(
                    from = chosen,
                    expiry = MayPlayExpiry.UntilEndOfNextTurn
                ))
            }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "219"
        artist = "Wayne Wu"
        imageUri = "https://cards.scryfall.io/normal/front/6/8/686fe623-ee50-407d-87c9-664fb039f4d9.jpg?1743204865"
    }
}
