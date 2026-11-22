package com.wingedsheep.mtg.sets.definitions.dmu.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.grantedTriggeredAbility
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Jaya, Fiery Negotiator
 * {2}{R}{R}
 * Legendary Planeswalker — Jaya
 * Loyalty 4
 *
 * +1: Create a 1/1 red Monk creature token with prowess.
 * −1: Exile the top two cards of your library. Choose one of them. You may play that card this turn.
 * −2: Choose target creature an opponent controls. Whenever you attack this turn, Jaya deals damage
 *     equal to the number of attacking creatures to that creature.
 * −8: You get an emblem with "Whenever you cast a red instant or sorcery spell, copy it twice. You
 *     may choose new targets for the copies."
 *
 * - +1 is Monastery Mentor's prowess Monk token, in red.
 * - −1 is Mishra's Research Desk's exile-two-choose-one shape with a this-turn play permission
 *   (play, so a chosen land may be played — under the normal land-play rules, per the ruling).
 * - −2 targets on activation, then hands the target to a delayed "whenever you attack this turn"
 *   trigger through a carried pipeline collection (Golden Argosy's shape). The carried binding keeps
 *   the object's identity, so a creature that has since left the battlefield is no longer "that
 *   creature". The delayed trigger fires even if Jaya is gone (ruling); she is its damage source.
 * - −8 is a triggered emblem ([Effects.CreateGlobalTriggeredAbility], Chandra, Dressed to Kill's
 *   shape) copying the triggering spell twice; it fires for any red instant or sorcery, with or
 *   without targets, and the copies resolve first.
 */
val JayaFieryNegotiator = card("Jaya, Fiery Negotiator") {
    manaCost = "{2}{R}{R}"
    colorIdentity = "R"
    typeLine = "Legendary Planeswalker — Jaya"
    startingLoyalty = 4
    oracleText = "+1: Create a 1/1 red Monk creature token with prowess.\n" +
        "−1: Exile the top two cards of your library. Choose one of them. You may play that card this turn.\n" +
        "−2: Choose target creature an opponent controls. Whenever you attack this turn, Jaya deals " +
        "damage equal to the number of attacking creatures to that creature.\n" +
        "−8: You get an emblem with \"Whenever you cast a red instant or sorcery spell, copy it " +
        "twice. You may choose new targets for the copies.\""

    loyaltyAbility(+1) {
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = setOf(Color.RED),
            creatureTypes = setOf("Monk"),
            keywords = setOf(Keyword.PROWESS),
            imageUri = "https://cards.scryfall.io/normal/front/3/8/388180bd-cc96-4b18-9ddd-18a3f7e06282.jpg?1783921127"
        )
        description = "Create a 1/1 red Monk creature token with prowess."
    }

    loyaltyAbility(-1) {
        effect = Effects.Pipeline {
            val exiled = gather(CardSource.TopOfLibrary(2))
            exile(exiled)
            val chosen = chooseExactly(1, from = exiled, prompt = "Choose a card you may play this turn")
            run(Effects.GrantMayPlayFromExile(chosen))
        }
        description = "Exile the top two cards of your library. Choose one of them. You may play " +
            "that card this turn."
    }

    loyaltyAbility(-2) {
        target(TargetFilter.CreatureOpponentControls)
        effect = Effects.Pipeline {
            val thatCreature = gather(CardSource.ChosenTargets)
            run(Effects.CreateDelayedTrigger(
                trigger = Triggers.you.attacks(),
                effect = Effects.DealDamage(
                    DynamicAmounts.battlefield(Player.Each, GameObjectFilter.Creature.attacking()).count(),
                    thatCreature.asTarget,
                ),
                carryCollections = listOf(thatCreature.key),
            ))
        }
        description = "Choose target creature an opponent controls. Whenever you attack this turn, " +
            "Jaya deals damage equal to the number of attacking creatures to that creature."
    }

    loyaltyAbility(-8) {
        effect = Effects.CreateGlobalTriggeredAbility(
            ability = grantedTriggeredAbility {
                trigger = Triggers.you.casts(GameObjectFilter.InstantOrSorcery.withColor(Color.RED))
                effect = Effects.CopyTargetSpell(
                    target = EffectTarget.TriggeringEntity,
                    copies = DynamicAmounts.fixed(2),
                )
                description = "Whenever you cast a red instant or sorcery spell, copy it twice. " +
                    "You may choose new targets for the copies."
            },
            descriptionOverride = "Whenever you cast a red instant or sorcery spell, copy it twice. " +
                "You may choose new targets for the copies.",
        )
        description = "You get an emblem with \"Whenever you cast a red instant or sorcery spell, " +
            "copy it twice. You may choose new targets for the copies.\""
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "133"
        artist = "Marta Nael"
        imageUri = "https://cards.scryfall.io/normal/front/3/b/3b9f54f0-4368-4d28-aa21-3f1e7be1b23a.jpg?1783921315"

        ruling("2022-09-09", "You must pay all costs and follow all normal timing rules to play a card chosen for Jaya's second loyalty ability. For example, if you choose a land card, you may only play it during one of your main phases while the stack is empty, and only if you haven't yet played a land in the same turn.")
        ruling("2022-09-09", "Activating Jaya's third loyalty ability creates a delayed triggered ability that will trigger each time you declare attackers this turn (usually, this will only happen once). This ability will trigger even if Jaya is no longer on the battlefield. If anything needs information about the source of that damage, use Jaya's characteristics as she last existed on the battlefield.")
        ruling("2022-09-09", "The triggered ability of Jaya's emblem will copy any red instant or sorcery spell you cast, not just one with targets.")
        ruling("2022-09-09", "Copies are created even if the spell that caused the ability to trigger has been countered by the time that ability resolves. The copies resolve before the original spell.")
        ruling("2022-09-09", "If the spell that's copied is modal (that is, it says \"Choose one —\" or the like), the copies will have the same mode or modes. You can't choose different ones.")
        ruling("2022-09-09", "You can't choose to pay any additional costs for the copies. However, effects based on any additional costs that were paid for the original spell are copied as though those same costs were paid for the copies too. Notably, if the spell you copy was kicked, the copies will also be kicked.")
        ruling("2022-09-09", "The copies are created on the stack, so they're not \"cast.\" Abilities that trigger when a player casts a spell won't trigger.")
    }
}
