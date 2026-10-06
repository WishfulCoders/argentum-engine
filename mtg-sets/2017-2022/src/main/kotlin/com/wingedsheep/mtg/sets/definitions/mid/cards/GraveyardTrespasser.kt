package com.wingedsheep.mtg.sets.definitions.mid.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.daybound
import com.wingedsheep.sdk.dsl.nightbound
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.WardCost
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Graveyard Trespasser // Graveyard Glutton (Innistrad: Midnight Hunt)
 * {2}{B}
 * Creature — Human Werewolf // Creature — Werewolf
 *
 * Front — Graveyard Trespasser (3/3): Ward—Discard a card. "Whenever this creature enters or
 *          attacks, exile up to one target card from a graveyard. If a creature card was exiled this
 *          way, each opponent loses 1 life and you gain 1 life." Daybound.
 * Back  — Graveyard Glutton (4/4): Ward—Discard a card. Same trigger exiling up to two target cards
 *          from graveyards, draining 1 for each creature card exiled this way. Nightbound.
 *
 * "Enters or attacks" is two triggered abilities sharing one effect (the Sun Titan shape). "Exiled this
 * way" reads the pile that actually moved ([moveTracked]) rather than the chosen targets, so a target
 * that left its graveyard before resolution drains nothing. The back face's drain scales off the
 * creature-filtered moved pile's count. The back face has no mana cost, so its color comes from a
 * color indicator (CR 204).
 */

private fun frontEffect() = Effects.Pipeline {
    val exiled = moveTracked(gather(CardSource.ChosenTargets), CardDestination.ToZone(Zone.EXILE))
    ifNotEmpty(exiled, filter = GameObjectFilter.Creature) {
        run(Effects.LoseLife(1, EffectTarget.PlayerRef(Player.EachOpponent)) then Effects.GainLife(1))
    } orElse {
        run(Effects.Nothing)
    }
}

private fun backEffect() = Effects.Pipeline {
    val exiled = moveTracked(gather(CardSource.ChosenTargets), CardDestination.ToZone(Zone.EXILE))
    val creatures = filter(exiled, GameObjectFilter.Creature)
    run(
        Effects.LoseLife(creatures.count, EffectTarget.PlayerRef(Player.EachOpponent)) then
            Effects.GainLife(creatures.count)
    )
}

private const val FRONT_TRIGGER_TEXT = "Whenever this creature enters or attacks, exile up to one target " +
    "card from a graveyard. If a creature card was exiled this way, each opponent loses 1 life and you " +
    "gain 1 life."

private const val BACK_TRIGGER_TEXT = "Whenever this creature enters or attacks, exile up to two target " +
    "cards from graveyards. For each creature card exiled this way, each opponent loses 1 life and you " +
    "gain 1 life."

private val GraveyardTrespasserFront = card("Graveyard Trespasser") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Human Werewolf"
    power = 3
    toughness = 3
    oracleText = "Ward—Discard a card.\n" +
        "$FRONT_TRIGGER_TEXT\n" +
        "Daybound (If a player casts no spells during their own turn, it becomes night next turn.)"

    keywordAbility(KeywordAbility.Ward(WardCost.Discard()))

    triggeredAbility {
        trigger = Triggers.self.enters()
        target(TargetFilter.CardInGraveyard, optional = true)
        effect = frontEffect()
        description = FRONT_TRIGGER_TEXT
    }
    triggeredAbility {
        trigger = Triggers.self.attacks()
        target(TargetFilter.CardInGraveyard, optional = true)
        effect = frontEffect()
        description = FRONT_TRIGGER_TEXT
    }
    daybound()

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "104"
        artist = "Chris Rallis"
        imageUri = "https://cards.scryfall.io/normal/front/d/a/daa2a273-488f-4285-a069-ad159ad2d393.jpg?1783925622"
    }
}

private val GraveyardGlutton = card("Graveyard Glutton") {
    manaCost = ""
    colorIdentity = "B"
    colorIndicator = "B" // Transformed back face, no mana cost (CR 204).
    typeLine = "Creature — Werewolf"
    power = 4
    toughness = 4
    oracleText = "Ward—Discard a card.\n" +
        "$BACK_TRIGGER_TEXT\n" +
        "Nightbound (If a player casts at least two spells during their own turn, it becomes day next turn.)"

    keywordAbility(KeywordAbility.Ward(WardCost.Discard()))

    triggeredAbility {
        trigger = Triggers.self.enters()
        targets(TargetFilter.CardInGraveyard, count = 2, optional = true)
        effect = backEffect()
        description = BACK_TRIGGER_TEXT
    }
    triggeredAbility {
        trigger = Triggers.self.attacks()
        targets(TargetFilter.CardInGraveyard, count = 2, optional = true)
        effect = backEffect()
        description = BACK_TRIGGER_TEXT
    }
    nightbound()

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "104"
        artist = "Chris Rallis"
        imageUri = "https://cards.scryfall.io/normal/back/d/a/daa2a273-488f-4285-a069-ad159ad2d393.jpg?1783925622"
    }
}

val GraveyardTrespasser: CardDefinition = CardDefinition.doubleFacedCreature(
    frontFace = GraveyardTrespasserFront,
    backFace = GraveyardGlutton,
)
