package com.wingedsheep.mtg.sets.definitions.stx.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.effects.MoveType
import com.wingedsheep.sdk.scripting.effects.RevealHandEffect
import com.wingedsheep.sdk.scripting.effects.SelectFromCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectionMode
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetOpponent
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Humiliate
 * {W}{B}
 * Sorcery
 * Target opponent reveals their hand. You choose a nonland card from it. That player discards that
 * card. Put a +1/+1 counter on a creature you control.
 *
 * Pilfer's reveal-choose-discard, then Season of Gathering's untargeted "a creature you control",
 * chosen as the spell resolves, after the hand is revealed (ruling). With no creature the counter
 * part does nothing, and the spell can still be cast (ruling).
 */
val Humiliate = card("Humiliate") {
    manaCost = "{W}{B}"
    colorIdentity = "WB"
    typeLine = "Sorcery"
    oracleText = "Target opponent reveals their hand. You choose a nonland card from it. That player discards that card. Put a +1/+1 counter on a creature you control."

    spell {
        val opponent = target("target opponent", TargetOpponent())
        effect = Effects.Composite(
            listOf(
                RevealHandEffect(opponent),
                GatherCardsEffect(
                    source = CardSource.FromZone(Zone.HAND, Player.ContextPlayer(0)),
                    storeAs = "opponentHand"
                ),
                SelectFromCollectionEffect(
                    from = "opponentHand",
                    selection = SelectionMode.ChooseExactly(DynamicAmount.Fixed(1)),
                    chooser = Chooser.Controller,
                    filter = GameObjectFilter.Nonland,
                    storeSelected = "toDiscard",
                    prompt = "Choose a nonland card to discard",
                    alwaysPrompt = true,
                    showAllCards = true
                ),
                MoveCollectionEffect(
                    from = "toDiscard",
                    destination = CardDestination.ToZone(Zone.GRAVEYARD, Player.ContextPlayer(0)),
                    moveType = MoveType.Discard
                ),
                Effects.SelectTarget(Targets.CreatureYouControl, "counterTarget")
                    .then(Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 1, EffectTarget.PipelineTarget("counterTarget")))
            )
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "193"
        artist = "Randy Vargas"
        flavorText = "\"Aw, what's that? No comeback?\""
        imageUri = "https://cards.scryfall.io/normal/front/d/c/dc3ae8cb-fbdb-45a8-83c2-cbf4aff01f90.jpg?1783927311"
        ruling("2021-04-16", "You may cast Humiliate even if you control no creatures.")
        ruling("2021-04-16", "You choose which creature to put the +1/+1 counter on as Humiliate is resolving, after their hand has been revealed.")
    }
}
