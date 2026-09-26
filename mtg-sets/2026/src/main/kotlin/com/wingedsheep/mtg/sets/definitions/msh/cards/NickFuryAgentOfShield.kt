package com.wingedsheep.mtg.sets.definitions.msh.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardOrder
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Nick Fury, Agent of S.H.I.E.L.D. — Marvel Super Heroes #25 (rare)
 * {W} · Legendary Creature — Human Spy Hero · 2/1
 *
 * Power-up — {W}{U}{B}{R}{G}: Put two +1/+1 counters on Nick Fury, then look at the top seven
 * cards of your library. You may put a Hero, Equipment, or Vehicle card from among them onto the
 * battlefield. If it's a double-faced card, you may transform it. Put the rest on the bottom of
 * your library in a random order. (Activate each power-up ability only once. Reduce the cost by
 * his mana cost if he entered this turn.)
 *
 * A one-mana 2/1 whose power-up is the set's widest dig. `{W}{U}{B}{R}{G}` − `{W}` =
 * `{U}{B}{R}{G}`, so the white pip he was cast with is the one his own ability stops asking for —
 * which is the point, since the rest of the cost is every other color.
 *
 * The dig is the standard Gather → Select → Move pipeline (Gishath, Sun's Avatar is the closest
 * existing shape), with two differences that follow the printed text:
 *  - **Look, don't reveal** — `revealed = false` on the gather, so the seven cards stay hidden
 *    from opponents; `showAllCards` still shows the player everything they're choosing among.
 *  - **"You may put *a*"** is `ChooseUpTo(1)`, not `ChooseAnyNumber` and not a forced choice: with
 *    no Hero, Equipment, or Vehicle among the seven the ability still resolves and simply bottoms
 *    all of them.
 *
 * The transform clause is applied **after** the permanent enters, which is what the printed order
 * says — "put … onto the battlefield. If it's a double-faced card, you may transform it." So the
 * card enters front face up and its enters-the-battlefield triggers fire on that face, then the
 * optional [TransformEffect] flips it. That matters in this set: putting a front face in and
 * transforming it is not the same as putting its back face directly onto the battlefield.
 *
 * *"If it's a double-faced card"* is a real gate, not a formality: [Filters.DoubleFaced]
 * (`CardPredicate.IsDoubleFaced`, CR 712.1) is checked against what actually entered, so the "you
 * may transform it" prompt is raised only when the permanent has a second face to turn to. Put a
 * single-faced Hero onto the battlefield and the ability finishes without ever asking — the card
 * offers no choice there, so neither does the engine.
 */
val NickFuryAgentOfShield = card("Nick Fury, Agent of S.H.I.E.L.D.") {
    manaCost = "{W}"
    colorIdentity = "WUBRG"
    typeLine = "Legendary Creature — Human Spy Hero"
    oracleText = "Power-up — {W}{U}{B}{R}{G}: Put two +1/+1 counters on Nick Fury, then look at " +
        "the top seven cards of your library. You may put a Hero, Equipment, or Vehicle card from " +
        "among them onto the battlefield. If it's a double-faced card, you may transform it. Put " +
        "the rest on the bottom of your library in a random order. (Activate each power-up " +
        "ability only once. Reduce the cost by his mana cost if he entered this turn.)"
    power = 2
    toughness = 1

    activatedAbility {
        isPowerUp = true
        cost = Costs.Mana("{W}{U}{B}{R}{G}")
        effect = Effects.Pipeline {
            run(Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 2, EffectTarget.Self))
            // "Look at", not "reveal" — stated rather than left to the default, because it is a
            // printed distinction on this card.
            val furyLooked = gather(CardSource.TopOfLibrary(7), revealed = false)
            val (furyToBattlefield, furyToBottom) = chooseUpToSplit(
                1,
                from = furyLooked,
                filter = GameObjectFilter.Any.withSubtype(Subtype.HERO.value) or
                    GameObjectFilter.Any.withSubtype(Subtype.EQUIPMENT.value) or
                    GameObjectFilter.Any.withSubtype(Subtype.VEHICLE.value),
                showAllCards = true,
                prompt = "You may put a Hero, Equipment, or Vehicle card onto the battlefield",
                selectedLabel = "Put onto the battlefield",
                remainderLabel = "Put on the bottom of your library"
            )
            val furyEntered = moveTracked(furyToBattlefield, CardDestination.ToZone(Zone.BATTLEFIELD, Player.You))
            run(Effects.If(
                condition = whenMatches(furyEntered, Filters.DoubleFaced),
                then = Effects.May(
                    Effects.ForEachInCollection(furyEntered, Effects.Transform(EffectTarget.IterationEntity)),
                    descriptionOverride = "transform it"
                )
            ))
            toLibraryBottom(furyToBottom, order = CardOrder.Random)
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "25"
        artist = "Marco Turini"
        imageUri = "https://cards.scryfall.io/normal/front/3/a/3aa6fc02-ef76-426b-accd-6c0ef88b2a5e.jpg?1783902971"
    }
}
