package com.wingedsheep.mtg.sets.definitions.dmu.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.MayCastFromGraveyard
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Serra Paragon — Dominaria United #32
 * {2}{W}{W} · Creature — Angel · 3/4
 *
 * Flying
 * Once during each of your turns, you may play a land from your graveyard or cast a permanent spell
 * with mana value 3 or less from your graveyard. If you do, it gains "When this permanent is put
 * into a graveyard from the battlefield, exile it and you gain 2 life."
 *
 * One [MayCastFromGraveyard] grant with `playLands = true`: the land play and the permanent cast
 * share its single once-per-turn allowance (tracked on this Paragon, so a second Paragon brings a
 * second use, and a Paragon that leaves and returns is a new object with a fresh one). A land's mana
 * value is 0, so `Permanent.manaValueAtMost(3)` reads both halves — lands are played, never cast (CR
 * 305.9). `gainsAbility` hands the spell (CR 400.7h) or land (CR 400.7i) the death trigger; a
 * permanent spell keeps it as it becomes a permanent (CR 400.7b), and the permanent keeps it even if
 * Paragon leaves. The play/cast still follows normal timing: a land only as the land-play special
 * action with a land drop left, a spell at its normal timing and for its mana cost.
 */
val SerraParagon = card("Serra Paragon") {
    manaCost = "{2}{W}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Angel"
    oracleText = "Flying\n" +
        "Once during each of your turns, you may play a land from your graveyard or cast a permanent " +
        "spell with mana value 3 or less from your graveyard. If you do, it gains \"When this permanent " +
        "is put into a graveyard from the battlefield, exile it and you gain 2 life.\""
    power = 3
    toughness = 4

    keywords(Keyword.FLYING)

    staticAbility {
        ability = MayCastFromGraveyard(
            filter = GameObjectFilter.Permanent.manaValueAtMost(3),
            duringYourTurnOnly = true,
            oncePerTurn = true,
            playLands = true,
            gainsAbility = TriggeredAbility.create(
                trigger = Triggers.self.dies(),
                effect = Effects.Exile(EffectTarget.Self) then Effects.GainLife(2),
            ),
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "32"
        artist = "Heonhwa"
        imageUri = "https://cards.scryfall.io/normal/front/c/e/ce295f1e-fb31-4275-a5d3-8c6f29afff40.jpg?1783921359"
    }
}
