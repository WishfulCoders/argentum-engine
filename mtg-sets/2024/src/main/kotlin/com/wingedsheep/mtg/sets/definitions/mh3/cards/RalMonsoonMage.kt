package com.wingedsheep.mtg.sets.definitions.mh3.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.EntersWithDynamicCounters
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget
import com.wingedsheep.sdk.scripting.targets.AnyTarget
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Ral, Monsoon Mage // Ral, Leyline Prodigy — Modern Horizons 3 #247
 * {1}{R} · Legendary Creature — Human Wizard 1/3 // Legendary Planeswalker — Ral (loyalty 2)
 *
 * Front:
 *   Instant and sorcery spells you cast cost {1} less to cast.
 *   Whenever you cast an instant or sorcery spell during your turn, flip a coin. If you lose the
 *   flip, Ral deals 1 damage to you. If you win the flip, you may exile Ral. If you do, return him
 *   to the battlefield transformed under his owner's control.
 * Back:
 *   Ral enters with an additional loyalty counter on him for each instant and sorcery spell you've
 *   cast this turn.
 *   +1: Until your next turn, instant and sorcery spells you cast cost {1} less to cast.
 *   −2: Ral deals 2 damage divided as you choose among one or two targets. Draw a card if you
 *       control a blue permanent other than Ral.
 *   −8: Exile the top eight cards of your library. You may cast instant and sorcery spells from
 *       among them this turn without paying their mana costs.
 *
 * Modeling notes:
 *  - "During your turn" is a trigger restriction ([Conditions.IsYourTurn]), not an intervening if.
 *  - The flip's "exile Ral … return him transformed" is [Effects.ExileAndReturnTransformed]: a new
 *    object enters back face up, so the back face's enters-with replacement (the extra loyalty)
 *    applies on the way in, on top of the printed loyalty 2. The count reads the cast history, so
 *    countered spells and spells still on the stack count too (ruling).
 *  - If an earlier copy of the trigger already turned Ral over, a later winning flip finds the
 *    original object gone and does nothing (ruling) — the source reference is lost (CR 400.7).
 *  - The +1 is [Effects.ReduceSpellCosts] with `Duration.UntilYourNextTurn`, so it covers the
 *    opponent's turn and expires as your next turn begins.
 */
private val RalMonsoonMageFront = card("Ral, Monsoon Mage") {
    manaCost = "{1}{R}"
    colorIdentity = "UR"
    typeLine = "Legendary Creature — Human Wizard"
    power = 1
    toughness = 3
    oracleText = "Instant and sorcery spells you cast cost {1} less to cast.\n" +
        "Whenever you cast an instant or sorcery spell during your turn, flip a coin. If you lose " +
        "the flip, Ral deals 1 damage to you. If you win the flip, you may exile Ral. If you do, " +
        "return him to the battlefield transformed under his owner's control."

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.YouCast(GameObjectFilter.InstantOrSorcery),
            modification = CostModification.ReduceGeneric(1),
        )
    }

    triggeredAbility {
        trigger = Triggers.you.casts(GameObjectFilter.InstantOrSorcery)
        triggerRestriction = Conditions.IsYourTurn
        effect = Effects.FlipCoin(
            wonEffect = Effects.May(Effects.ExileAndReturnTransformed(EffectTarget.Self)),
            lostEffect = Effects.DealDamage(1, EffectTarget.Controller),
        )
        description = "Whenever you cast an instant or sorcery spell during your turn, flip a coin. " +
            "If you lose the flip, Ral deals 1 damage to you. If you win the flip, you may exile " +
            "Ral. If you do, return him to the battlefield transformed under his owner's control."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "247"
        artist = "Dmitry Burmak"
        imageUri = "https://cards.scryfall.io/normal/front/4/3/438d8a26-ddc9-4829-8aff-22d6af6575cf.jpg?1783911228"

        ruling("2024-06-07", "In some rare cases, a spell or ability may cause Ral, Monsoon Mage to transform while he's a creature (front face up) on the battlefield. If this happens, Ral, Leyline Prodigy won't have any loyalty counters on him and will subsequently be put into his owner's graveyard.")
        ruling("2024-06-07", "You flip a coin as Ral, Monsoon Mage's triggered ability resolves. No player may take actions between seeing the result of the flip and the effects that occur based on the result of the flip.")
        ruling("2024-06-07", "If Ral, Monsoon Mage is exiled and returns transformed while one or more instances of his triggered ability are still on the stack, those abilities will still resolve. However, if the controller of one of those abilities wins the flip, nothing will happen since the object that ability refers to is no longer on the battlefield.")
    }
}

private val RalLeylineProdigy = card("Ral, Leyline Prodigy") {
    manaCost = ""
    colorIdentity = "UR"
    colorIndicator = "UR"
    typeLine = "Legendary Planeswalker — Ral"
    startingLoyalty = 2
    oracleText = "Ral enters with an additional loyalty counter on him for each instant and sorcery " +
        "spell you've cast this turn.\n" +
        "+1: Until your next turn, instant and sorcery spells you cast cost {1} less to cast.\n" +
        "−2: Ral deals 2 damage divided as you choose among one or two targets. Draw a card if you " +
        "control a blue permanent other than Ral.\n" +
        "−8: Exile the top eight cards of your library. You may cast instant and sorcery spells " +
        "from among them this turn without paying their mana costs."

    replacementEffect(
        EntersWithDynamicCounters(
            counterType = CounterType.LOYALTY,
            count = DynamicAmounts.spellsCastThisTurn(filter = GameObjectFilter.InstantOrSorcery),
        )
    )

    loyaltyAbility(+1) {
        effect = Effects.ReduceSpellCosts(
            spellFilter = GameObjectFilter.InstantOrSorcery,
            amount = DynamicAmounts.fixed(1),
            duration = Duration.UntilYourNextTurn,
        )
        description = "Until your next turn, instant and sorcery spells you cast cost {1} less to cast."
    }

    loyaltyAbility(-2) {
        target = AnyTarget(count = 2, minCount = 1)
        effect = Effects.DividedDamage(total = 2, minTargets = 1, maxTargets = 2) then
            Effects.If(
                Conditions.YouControl(GameObjectFilter.Permanent.withColor(Color.BLUE), excludeSelf = true),
                Effects.DrawCards(1),
            )
        description = "Ral deals 2 damage divided as you choose among one or two targets. Draw a " +
            "card if you control a blue permanent other than Ral."
    }

    loyaltyAbility(-8) {
        effect = Effects.Pipeline {
            val exiled = gather(CardSource.TopOfLibrary(8))
            exile(exiled)
            val castable = filter(exiled, GameObjectFilter.InstantOrSorcery)
            run(Effects.GrantMayPlayFromExile(castable))
            run(Effects.GrantPlayWithoutPayingCost(castable))
        }
        description = "Exile the top eight cards of your library. You may cast instant and sorcery " +
            "spells from among them this turn without paying their mana costs."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "247"
        artist = "Dmitry Burmak"
        imageUri = "https://cards.scryfall.io/normal/back/4/3/438d8a26-ddc9-4829-8aff-22d6af6575cf.jpg?1783911228"

        ruling("2024-06-07", "You can activate one of Ral, Leyline Prodigy's loyalty abilities the turn he enters the battlefield. However, you may do so only during one of your main phases when the stack is empty. For example, if Ral, Leyline Prodigy enters the battlefield during combat, there will be an opportunity for your opponent to remove him before you can activate one of his abilities.")
        ruling("2024-06-07", "The first ability of Ral, Leyline Prodigy will count any instant and sorcery spells you've cast that turn, including ones that were countered, didn't resolve for other reasons, or are still on the stack.")
        ruling("2024-06-07", "You choose how many targets Ral, Leyline Prodigy's second loyalty ability has and how the damage is divided as you activate the ability. Each target must receive at least 1 damage.")
        ruling("2024-06-07", "If some of the targets are illegal as Ral, Leyline Prodigy's second loyalty ability tries to resolve, the original division of damage still applies and the damage that would have been dealt to the illegal targets is lost. It won't be dealt instead to a legal target. If all of the targets are illegal, the ability won't resolve and none of its effects will happen. You won't draw a card.")
        ruling("2024-06-07", "You follow all normal timing rules for spells cast with the permission granted by Ral, Leyline Prodigy's last ability. For example, if one of the exiled cards is a sorcery, you can cast it only during your main phase while the stack is empty.")
        ruling("2024-06-07", "If one of the exiled cards has {X} in its mana cost, you must choose 0 as the value of X when casting it without paying its mana cost.")
    }
}

val RalMonsoonMage: CardDefinition = CardDefinition.doubleFacedPermanent(
    frontFace = RalMonsoonMageFront,
    backFace = RalLeylineProdigy,
)
