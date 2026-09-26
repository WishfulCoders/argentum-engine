package com.wingedsheep.mtg.sets.definitions.sos.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Ark of Hunger
 * {2}{R}{W}
 * Artifact
 *
 * Whenever one or more cards leave your graveyard, this artifact deals 1 damage to each
 * opponent and you gain 1 life.
 * {T}: Mill a card. You may play that card this turn.
 *
 * The leave-graveyard trigger batches (fires at most once per event batch regardless of how
 * many cards left, and regardless of where they went). "Mill a card" puts the top card into
 * the graveyard; the may-play grant keyed to the milled collection then lets the controller
 * play it from the graveyard until end of turn — the cast-from-zone enumerator honors a
 * [com.wingedsheep.sdk.scripting.effects.MayPlayExpiry] permission whose card sits in the
 * graveyard, so no exile detour is needed (same shape as Tablet of Discovery).
 */
val ArkOfHunger = card("Ark of Hunger") {
    manaCost = "{2}{R}{W}"
    colorIdentity = "RW"
    typeLine = "Artifact"
    oracleText = "Whenever one or more cards leave your graveyard, this artifact deals 1 damage to " +
        "each opponent and you gain 1 life.\n" +
        "{T}: Mill a card. You may play that card this turn."

    triggeredAbility {
        trigger = Triggers.oneOrMore(GameObjectFilter.Any).leaveYourGraveyard()
        effect = Effects.DealDamage(1, EffectTarget.PlayerRef(Player.EachOpponent), damageSource = EffectTarget.Self) then
            Effects.GainLife(1)
    }

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.Pipeline {
            val milledThisWay = gather(CardSource.TopOfLibrary(1))
            toGraveyard(milledThisWay)
            run(Effects.GrantMayPlayFromExile(milledThisWay))
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "173"
        artist = "Ksenia Kim"
        flavorText = "Few relics survived the turmoil of the Blood Age; those that did are powerful, dangerous, or both."
        imageUri = "https://cards.scryfall.io/normal/front/7/9/79d01c19-162b-4a12-9e27-18366d95eaa0.jpg?1775938187"
    }
}
