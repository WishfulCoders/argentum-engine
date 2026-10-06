package com.wingedsheep.mtg.sets.definitions.mh2.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CreatureOutsideBattlefield
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.RepeatCondition
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

private const val GRIST_MILLED = "gristMilled"

/**
 * Grist, the Hunger Tide — Modern Horizons 2 #202
 * {1}{B}{G} · Legendary Planeswalker — Grist · Loyalty 3
 *
 * As long as Grist isn't on the battlefield, it's a 1/1 Insect creature in addition to its other types.
 * +1: Create a 1/1 black and green Insect creature token, then mill a card. If an Insect card was
 *     milled this way, put a loyalty counter on Grist and repeat this process.
 * −2: You may sacrifice a creature. When you do, destroy target creature or planeswalker.
 * −5: Each opponent loses life equal to the number of creature cards in your graveyard.
 *
 * The first line is [CreatureOutsideBattlefield] (CR 113.6c): anywhere but the battlefield Grist is a
 * Legendary Planeswalker Creature — Grist Insect, so it is a creature card to searches and graveyard
 * counts and a creature spell on the stack.
 *
 * +1 is a do-while loop ([Effects.RepeatWhile]) over "token, mill one": it repeats exactly while the
 * card just milled is an Insect card (Grist itself counts, being an Insect off the battlefield). The
 * loyalty counter is only placed while Grist is still on the battlefield; if it has left, the token
 * and the mill still happen and the process still repeats (ruling 2021-06-18).
 *
 * −2 is Shrapnel Slinger's optional reflexive sacrifice: the creature is chosen as the ability
 * resolves (not targeted), and only "when you do" is the creature or planeswalker targeted. It works
 * even if Grist has left the battlefield (ruling 2021-06-18).
 */
val GristTheHungerTide = card("Grist, the Hunger Tide") {
    manaCost = "{1}{B}{G}"
    colorIdentity = "BG"
    typeLine = "Legendary Planeswalker — Grist"
    startingLoyalty = 3
    oracleText = "As long as Grist isn't on the battlefield, it's a 1/1 Insect creature in addition to its other types.\n" +
        "+1: Create a 1/1 black and green Insect creature token, then mill a card. If an Insect card was milled this way, put a loyalty counter on Grist and repeat this process.\n" +
        "−2: You may sacrifice a creature. When you do, destroy target creature or planeswalker.\n" +
        "−5: Each opponent loses life equal to the number of creature cards in your graveyard."

    staticAbility {
        ability = CreatureOutsideBattlefield(power = 1, toughness = 1, subtypes = setOf("Insect"))
    }

    loyaltyAbility(+1) {
        val insectMilled = Conditions.CollectionContainsMatch(GRIST_MILLED, GameObjectFilter.Any.withSubtype("Insect"))
        effect = Effects.RepeatWhile(
            body = Effects.CreateToken(
                power = 1,
                toughness = 1,
                colors = setOf(Color.BLACK, Color.GREEN),
                creatureTypes = setOf("Insect"),
                imageUri = "https://cards.scryfall.io/normal/front/0/3/03ce1033-ce07-40ef-9315-beb759af9465.jpg?1783926587",
            ) then Effects.Pipeline {
                val milled = gather(CardSource.TopOfLibrary(DynamicAmounts.fixed(1), Player.You, isMill = true), name = GRIST_MILLED)
                move(milled, CardDestination.ToZone(Zone.GRAVEYARD, Player.You))
            } then Effects.If(
                condition = Conditions.All(
                    insectMilled,
                    Conditions.SourceMatches(GameObjectFilter.Permanent.sourceItself().currentlyIn(Zone.BATTLEFIELD))
                ),
                then = Effects.AddCounters(CounterType.LOYALTY, 1, EffectTarget.Self)
            ),
            repeatCondition = RepeatCondition.WhileCondition(insectMilled)
        )
    }

    loyaltyAbility(-2) {
        effect = Effects.ReflexiveTrigger(
            action = Effects.SacrificeOwn(GameObjectFilter.Creature),
            optional = true,
            descriptionOverride = "You may sacrifice a creature. When you do, destroy target creature or planeswalker."
        ) {
            val victim = target(Targets.CreatureOrPlaneswalker)
            effect = Effects.Destroy(victim)
        }
    }

    loyaltyAbility(-5) {
        effect = Effects.LoseLife(
            DynamicAmounts.zone(Player.You, Zone.GRAVEYARD, GameObjectFilter.Creature).count(),
            EffectTarget.PlayerRef(Player.EachOpponent)
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "202"
        artist = "Yongjae Choi"
        imageUri = "https://cards.scryfall.io/normal/front/6/9/69af2825-18c2-4463-b6ba-42eaa070ccc1.jpg?1783926814"
        ruling("2021-06-18", "Anywhere but on the battlefield, Grist is a Legendary Planeswalker Creature — Grist Insect. Once it enters the battlefield, it is no longer a creature and is just a planeswalker. Anything that could search for or affect a creature or planeswalker card in zones other than the battlefield could affect Grist. For example, you could put it onto the battlefield with Chord of Calling, it could be countered by Essence Scatter (but not by Negate), and opponents couldn't make you discard it with Duress.")
        ruling("2021-06-18", "If Grist is no longer on the battlefield as its first loyalty ability resolves, you will still create a 1/1 black and green Insect creature token and mill a card. If an Insect card is milled this way, you won't be able to put a loyalty counter on Grist, but you will still repeat the process.")
        ruling("2021-06-18", "Grist's second loyalty ability doesn't require a target. If you choose to sacrifice a creature as it resolves, the reflexive triggered ability triggers and you'll choose a target creature or planeswalker to destroy. This will happen even if Grist isn't on the battlefield when the loyalty ability resolves.")
        ruling("2021-06-18", "Count the number of creature cards in your graveyard as the third loyalty ability resolves to determine how much life each opponent loses. If Grist is in your graveyard at this time, it'll be a creature card and will contribute to the count.")
    }
}
