package com.wingedsheep.mtg.sets.definitions.rna.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.CollectionFilter
import com.wingedsheep.sdk.scripting.effects.FilterCollectionEffect
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.SelectFromCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectionMode
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Electrodominance
 * {X}{R}{R}
 * Instant
 * Electrodominance deals X damage to any target. You may cast a spell with mana value X or less
 * from your hand without paying its mana cost.
 *
 * X damage, then Kellan, the Kid's free-cast pipeline with X as the cap: the hand's nonland cards
 * with mana value X or less, at most one chosen, cast during the resolution (so timing restrictions
 * are ignored, rulings) through [Effects.CastFromCollectionWithoutPayingCost]. A lethally damaged
 * target is still on the battlefield while that spell is cast; it dies once Electrodominance has
 * finished resolving. An illegal target fizzles the whole spell, free cast included.
 */
val Electrodominance = card("Electrodominance") {
    manaCost = "{X}{R}{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "Electrodominance deals X damage to any target. You may cast a spell with mana value X or less from your hand without paying its mana cost."

    spell {
        val t = target("any target", Targets.Any)
        effect = Effects.Composite(
            Effects.DealDamage(DynamicAmount.XValue, t),
            GatherCardsEffect(CardSource.FromZone(Zone.HAND, Player.You, GameObjectFilter.Nonland), "hand"),
            FilterCollectionEffect(
                from = "hand",
                filter = CollectionFilter.ManaValueAtMost(DynamicAmount.XValue),
                storeMatching = "castable"
            ),
            SelectFromCollectionEffect(
                from = "castable",
                selection = SelectionMode.ChooseSpell,
                storeSelected = "spellToCast",
                showAllCards = true,
                prompt = "You may cast a spell with mana value X or less without paying its mana cost",
                selectedLabel = "Cast for free"
            ),
            Effects.CastFromCollectionWithoutPayingCost("spellToCast")
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "99"
        artist = "Dmitry Burmak"
        flavorText = "\"Basically, we turn a bunch of little lightnings into one big lightning.\""
        imageUri = "https://cards.scryfall.io/normal/front/5/c/5c63877b-cdab-4ce4-a1c0-c088eb62a57a.jpg?1783933682"
        ruling("2024-04-12", "If the target is illegal as Electrodominance tries to resolve, it won't resolve and none of its effects will happen. You won't get to cast a spell from your hand.")
        ruling("2024-04-12", "Effects that allow you to cast a spell don't allow you to play a land.")
        ruling("2024-04-12", "If the target is dealt lethal damage this way, it will still be on the battlefield while you cast a spell from your hand. It won't die until after Electrodominance is finished resolving. If any of its abilities trigger while you cast that spell, those abilities will trigger but won't resolve until after the target has died.")
        ruling("2024-04-12", "You cast the spell during the resolution of Electrodominance. Ignore timing restrictions based on the spell's type.")
        ruling("2024-04-12", "If you cast a spell \"without paying its mana cost,\" you can't choose to cast it for any alternative costs. You can, however, pay additional costs, such as kicker costs. If the spell has any mandatory additional costs, such as that of Bankrupt in Blood, those must be paid to cast the card.")
        ruling("2024-04-12", "If a spell has {X} in its mana cost, you must choose 0 as the value of X when casting it without paying its mana cost.")
    }
}
