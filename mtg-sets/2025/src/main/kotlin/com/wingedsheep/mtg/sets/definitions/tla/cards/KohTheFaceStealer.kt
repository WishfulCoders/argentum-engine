package com.wingedsheep.mtg.sets.definitions.tla.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.HasAbilitiesOfChosenLinkedExiledCard
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Koh, the Face Stealer
 * {4}{B}{B}
 * Legendary Creature — Shapeshifter Spirit
 * 6/6
 *
 * When Koh enters, exile up to one other target creature.
 * Whenever another nontoken creature dies, you may exile it.
 * Pay 1 life: Choose a creature card exiled with Koh.
 * Koh has all activated and triggered abilities of the last chosen card.
 *
 * Implementation — Koh builds a personal pile of "cards exiled with Koh" and, on demand, dons the
 * face of one of them:
 *  - The ETB exiles up to one other target creature into Koh's [Effects.ExileLinkedToSource] pile
 *    (a permanent exile with no return — unlike Aang's Iceberg's exile-until-leaves).
 *  - A dies trigger ([TriggerBinding.OTHER], nontoken creatures) offers to exile the dying creature
 *    from its graveyard into the same pile ([Effects.May] + [EffectTarget.TriggeringEntity]).
 *  - "Pay 1 life: Choose a creature card exiled with Koh" gathers the linked-exile pile, lets the
 *    controller pick one creature card, and stamps it as the last chosen card via
 *    [Effects.RecordChosenLinkedExile].
 *  - [HasAbilitiesOfChosenLinkedExiledCard] then grants Koh all activated *and* triggered abilities
 *    of that chosen card (with Koh as their source), re-reading the choice live so re-choosing
 *    swaps faces. It grants only activated/triggered abilities — never static or keyword ones.
 *
 * Rulings-faithful notes: the "Pay 1 life" ability has no other cost and can be activated any time
 * you have priority, even with no cards exiled (it just does nothing to choose). The granted
 * abilities treat "this creature" as Koh, so a chosen creature's `{T}` or self-sacrifice binds to Koh.
 */
val KohTheFaceStealer = card("Koh, the Face Stealer") {
    manaCost = "{4}{B}{B}"
    colorIdentity = "B"
    typeLine = "Legendary Creature — Shapeshifter Spirit"
    power = 6
    toughness = 6
    oracleText = "When Koh enters, exile up to one other target creature.\n" +
        "Whenever another nontoken creature dies, you may exile it.\n" +
        "Pay 1 life: Choose a creature card exiled with Koh.\n" +
        "Koh has all activated and triggered abilities of the last chosen card."

    // "When Koh enters, exile up to one other target creature." — permanent exile into Koh's pile.
    triggeredAbility {
        trigger = Triggers.self.enters()
        val creature = target(TargetFilter.Creature.other(), optional = true)
        effect = Effects.ExileLinkedToSource(creature)
    }

    // "Whenever another nontoken creature dies, you may exile it." — exile the dying card from its
    // graveyard into Koh's pile.
    triggeredAbility {
        trigger = Triggers.another(GameObjectFilter.Creature.nontoken()).dies()
        effect = Effects.May(
            Effects.ExileLinkedToSource(EffectTarget.TriggeringEntity),
            inlineOnTrigger = true
        )
    }

    // "Pay 1 life: Choose a creature card exiled with Koh." — record the last chosen card.
    activatedAbility {
        cost = Costs.PayLife(1)
        effect = Effects.Pipeline {
            val kohExile = gather(CardSource.FromLinkedExile())
            val kohChosen = chooseExactly(
                1,
                from = kohExile,
                filter = GameObjectFilter.Creature,
                prompt = "Choose a creature card exiled with Koh"
            )
            run(Effects.RecordChosenLinkedExile(kohChosen))
        }
    }

    // "Koh has all activated and triggered abilities of the last chosen card."
    staticAbility {
        ability = HasAbilitiesOfChosenLinkedExiledCard()
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "107"
        artist = "Eduardo Francisco"
        imageUri = "https://cards.scryfall.io/normal/front/2/8/28f6fa32-5058-4c29-9ba8-9d8f2057eb6d.jpg?1764120741"
    }
}
