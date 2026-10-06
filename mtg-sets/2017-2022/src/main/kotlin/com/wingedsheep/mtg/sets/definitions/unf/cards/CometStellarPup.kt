package com.wingedsheep.mtg.sets.definitions.unf.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.CollectionSlot
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Comet, Stellar Pup — Unfinity #166 (canonical printing)
 * {2}{R}{W} · Legendary Planeswalker — Comet · Starting loyalty 5
 *
 * 0: Roll a six-sided die.
 * 1 or 2 — [+2], then create two 1/1 green Squirrel creature tokens. They gain haste until end of turn.
 * 3 — [−1], then return a card with mana value 2 or less from your graveyard to your hand.
 * 4 or 5 — Comet deals damage equal to the number of loyalty counters on him to a creature or
 * player, then [−2].
 * 6 — [+1], and you may activate Comet's loyalty ability two more times this turn.
 *
 * The d6 is a `Patterns.Mechanic.rollDie` results table (CR 706.3a). The loyalty symbols inside the
 * rows are part of the *effect*, not a cost (2022-10-07 ruling), so they are
 * `AddCounters`/`RemoveCounters(LOYALTY)` on Comet, in the printed order: the 4-or-5 row reads his
 * loyalty for the damage before the [−2], and a 3 or 4-5 that takes him to 0 kills him by SBA after
 * the ability resolves.
 *
 * Nothing targets (ruling): the graveyard card and the creature or player are chosen on
 * resolution — a pipeline choice and a non-targeting `selectTarget`, so hexproof and shroud don't
 * stop Comet's damage. The Squirrels gain haste through the created-tokens collection, so the
 * grant ends at end of turn instead of being printed on the token. The 6 row's "two more times" is
 * the additive [Effects.AllowAdditionalLoyaltyActivationsThisTurn]: a second 6 in the same turn
 * adds two more again.
 */
val CometStellarPup = card("Comet, Stellar Pup") {
    manaCost = "{2}{R}{W}"
    colorIdentity = "RW"
    typeLine = "Legendary Planeswalker — Comet"
    startingLoyalty = 5
    oracleText = "0: Roll a six-sided die.\n" +
        "1 or 2 — [+2], then create two 1/1 green Squirrel creature tokens. They gain haste until end of turn.\n" +
        "3 — [−1], then return a card with mana value 2 or less from your graveyard to your hand.\n" +
        "4 or 5 — Comet deals damage equal to the number of loyalty counters on him to a creature or player, then [−2].\n" +
        "6 — [+1], and you may activate Comet's loyalty ability two more times this turn."

    loyaltyAbility(0) {
        effect = Patterns.Mechanic.rollDie(
            6,
            1..2 to (
                Effects.AddCounters(CounterType.LOYALTY, 2, EffectTarget.Self) then
                    Effects.CreateToken(
                        count = 2,
                        power = 1,
                        toughness = 1,
                        colors = setOf(Color.GREEN),
                        creatureTypes = setOf("Squirrel"),
                        imageUri = "https://cards.scryfall.io/normal/front/0/b/0b4170e0-c899-481a-8076-cae9f3effc37.jpg?1783920622"
                    ) then
                    Effects.ForEachInCollection(
                        CollectionSlot.CreatedTokens,
                        Effects.GrantKeyword(Keyword.HASTE, EffectTarget.IterationEntity)
                    )
                ),
            3..3 to (
                Effects.RemoveCounters(CounterType.LOYALTY, 1, EffectTarget.Self) then
                    Effects.Pipeline {
                        val cheap = gather(
                            CardSource.FromZone(Zone.GRAVEYARD, Player.You, GameObjectFilter.Any.manaValueAtMost(2))
                        )
                        val chosen = chooseExactly(
                            1,
                            from = cheap,
                            prompt = "Return a card with mana value 2 or less from your graveyard to your hand"
                        )
                        toHand(chosen)
                    }
                ),
            4..5 to (
                Effects.Pipeline {
                    val victim = selectTarget(
                        Targets.CreatureOrPlayer,
                        nonTargeting = true,
                        prompt = "Choose a creature or player for Comet to deal damage to"
                    )
                    run(Effects.DealDamage(DynamicAmounts.countersOnSelf(CounterType.LOYALTY), victim.asTarget))
                } then
                    Effects.RemoveCounters(CounterType.LOYALTY, 2, EffectTarget.Self)
                ),
            6..6 to (
                Effects.AddCounters(CounterType.LOYALTY, 1, EffectTarget.Self) then
                    Effects.AllowAdditionalLoyaltyActivationsThisTurn()
                ),
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "166"
        artist = "Jeff Miracola"
        imageUri = "https://cards.scryfall.io/normal/front/a/7/a76fa8d4-923d-4afc-ba47-ba10fc0fe46e.jpg?1789040645"
        ruling("2022-10-07", "Each + loyalty symbol in Comet's abilities means to put the indicated number of loyalty counters on him. Each – loyalty symbol in Comet's abilities means to remove the indicated number of loyalty counters from him. The only difference is Comet uses these symbols in the effects of his abilities rather than the costs.")
        ruling("2022-10-07", "None of Comet's abilities require a target. All choices for cards, creatures, or players are made as the abilities resolve.")
        ruling("2022-10-07", "If you roll a 3 but there aren't any cards with mana value 2 or less in your graveyard, you'll just remove one loyalty counter from Comet.")
        ruling("2022-10-07", "If you roll a 4 or 5 while Comet has one loyalty counter on him, Comet will deal 1 damage to a creature or player and then that loyalty counter will be removed.")
        ruling("2022-10-07", "In some unusual cases, Comet may gain loyalty abilities other than the one he normally has. In those cases, the effect of rolling a 6 applies only to his normal ability.")
    }
}
