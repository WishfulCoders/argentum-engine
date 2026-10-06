package com.wingedsheep.mtg.sets.definitions.dmu.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.readAhead
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Founding the Third Path
 * {1}{U}
 * Enchantment — Saga
 *
 * Read ahead (Choose a chapter and start with that many lore counters. Add one after your draw
 * step. Skipped chapters don't trigger. Sacrifice after III.)
 * I — You may cast an instant or sorcery spell with mana value 1 or 2 from your hand without
 *     paying its mana cost.
 * II — Target player mills four cards.
 * III — Exile target instant or sorcery card from your graveyard. Copy it. You may cast the copy.
 *
 * Chapter I is the Kellan-style "choose up to one from hand, cast it free" pipeline; choosing
 * nothing is the "may". Chapter III is Kaervek's exile → copy → may-cast chain, paying the copy's
 * costs (the card doesn't say "without paying its mana cost").
 */
val FoundingTheThirdPath = card("Founding the Third Path") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment — Saga"
    oracleText = "Read ahead (Choose a chapter and start with that many lore counters. Add one " +
        "after your draw step. Skipped chapters don't trigger. Sacrifice after III.)\n" +
        "I — You may cast an instant or sorcery spell with mana value 1 or 2 from your hand " +
        "without paying its mana cost.\n" +
        "II — Target player mills four cards.\n" +
        "III — Exile target instant or sorcery card from your graveyard. Copy it. You may cast " +
        "the copy."

    readAhead()

    sagaChapter(1) {
        effect = Effects.Pipeline {
            val candidates = gather(
                CardSource.FromZone(
                    Zone.HAND,
                    filter = GameObjectFilter.InstantOrSorcery.manaValueAtLeast(1).manaValueAtMost(2),
                )
            )
            val chosen = chooseUpTo(
                1,
                from = candidates,
                selectedLabel = "Cast without paying its mana cost",
            )
            run(Effects.CastFromCollectionWithoutPayingCost(chosen))
        }
    }

    sagaChapter(2) {
        val player = target(Targets.Player)
        effect = Patterns.Library.mill(4, player)
    }

    sagaChapter(3) {
        val exiledCard = target(
            TargetFilter(GameObjectFilter.InstantOrSorcery.ownedByYou(), zone = Zone.GRAVEYARD)
        )
        effect = Effects.Pipeline {
            run(Effects.Move(exiledCard, Zone.EXILE))
            val copy = copyCard(exiledCard)
            run(Effects.May(
                Effects.CastFromCollection(copy),
                descriptionOverride = "You may cast the copy.",
            ))
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "50"
        artist = "Chris Seaman"
        imageUri = "https://cards.scryfall.io/normal/front/2/d/2d841faa-9e0b-45ff-9dfd-f8a169d9af76.jpg?1783921351"

        ruling("2022-09-09", "If you cast a card \"without paying its mana cost,\" you can't choose to cast it for any alternative costs. You can, however, pay additional costs. If the card has any mandatory additional costs, you must pay those to cast the card.")
        ruling("2022-09-09", "If the card has {X} in its mana cost, you must choose 0 as the value of X when casting it without paying its mana cost.")
        ruling("2022-09-09", "You must pay all costs for the copy of a spell you cast as Founding the Third Path's last chapter ability resolves. If you choose to cast it, you must cast as the ability resolves. You can't wait and cast it at a later time.")
        ruling("2022-09-09", "As a Saga with read ahead enters the battlefield, its controller chooses a number from one to that Saga's greatest chapter number. The Saga enters the battlefield with the chosen number of lore counters. Neither choosing the number nor putting the counters on the Saga use the stack, and neither can be responded to.")
    }
}
