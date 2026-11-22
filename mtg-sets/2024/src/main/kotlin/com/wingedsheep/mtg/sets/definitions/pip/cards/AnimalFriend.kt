package com.wingedsheep.mtg.sets.definitions.pip.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.CollectionSlot
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.grantedTriggeredAbility
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantTriggeredAbility
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.CollectionFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Animal Friend
 * {1}{G}
 * Enchantment — Aura
 * Enchant creature
 * Enchanted creature has "Whenever this creature attacks, create a 1/1 green Squirrel creature
 * token. Put a +1/+1 counter on that token for each Aura and Equipment attached to this creature
 * other than Animal Friend."
 *
 * The ability belongs to the enchanted creature, so inside it "this creature" is
 * [EffectTarget.Self] and "Animal Friend" is [EffectTarget.GrantingSource] — the Aura whose static
 * granted the trigger, captured when it went on the stack. The count is a gather over the Auras and
 * Equipment attached to the creature at resolution, minus Animal Friend by identity
 * ([CollectionFilter.ExcludeEntity]) rather than a flat "minus one": if Animal Friend has left the
 * battlefield in response, every remaining attachment counts.
 *
 * Per the 2024-03-08 ruling the token enters as a 1/1 and then gets its counters, which is the
 * create-then-counter order below.
 */
val AnimalFriend = card("Animal Friend") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\n" +
        "Enchanted creature has \"Whenever this creature attacks, create a 1/1 green Squirrel " +
        "creature token. Put a +1/+1 counter on that token for each Aura and Equipment attached to " +
        "this creature other than Animal Friend.\""

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    staticAbility {
        ability = GrantTriggeredAbility(
            ability = grantedTriggeredAbility {
                trigger = Triggers.self.attacks()
                effect = Effects.Pipeline {
                    val attachments = gather(
                        CardSource.AttachedTo(
                            host = EffectTarget.Self,
                            filter = GameObjectFilter.Any.withSubtype(Subtype.AURA) or
                                GameObjectFilter.Any.withSubtype(Subtype.EQUIPMENT)
                        )
                    )
                    val others = filter(
                        attachments,
                        CollectionFilter.ExcludeEntity(EffectTarget.GrantingSource)
                    )
                    run(
                        Effects.CreateToken(
                            power = 1,
                            toughness = 1,
                            colors = setOf(Color.GREEN),
                            creatureTypes = setOf("Squirrel"),
                            imageUri = "https://cards.scryfall.io/normal/front/f/9/" +
                                "f90bb066-79fd-48cd-ab5a-eaed71139b4e.jpg?1783912044"
                        )
                    )
                    run(
                        Effects.AddDynamicCounters(
                            CounterType.PLUS_ONE_PLUS_ONE,
                            others.count,
                            CollectionSlot.CreatedTokens.asTarget
                        )
                    )
                }
                description = "Whenever this creature attacks, create a 1/1 green Squirrel creature " +
                    "token. Put a +1/+1 counter on that token for each Aura and Equipment attached " +
                    "to this creature other than Animal Friend."
            },
            filter = Filters.EnchantedCreature,
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "72"
        artist = "Maxim Kostin"
        flavorText = "\"I scratch their back, they scratch you.\""
        imageUri = "https://cards.scryfall.io/normal/front/a/f/af31a045-0387-44ff-b735-54e19b04d925.jpg?1783912399"
        ruling(
            "2024-03-08",
            "The token will enter the battlefield and then have counters put on it. In most cases, " +
                "the game will see a 1/1 creature entering the battlefield for the purposes of any " +
                "abilities that may trigger, such as the first ability of Overseer of Vault 76."
        )
    }
}
