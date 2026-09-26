package com.wingedsheep.mtg.sets.definitions.mor.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Shared Animosity
 * {2}{R}
 * Enchantment
 * Whenever a creature you control attacks, it gets +1/+0 until end of turn for each other attacking
 * creature that shares a creature type with it.
 *
 * One trigger per attacker you control ([TriggerBinding.ANY]). The bonus counts creatures, not
 * types (ruling): every attacking creature — anyone's, so a teammate's in Two-Headed Giant — that
 * shares at least one creature type with the triggering attacker, less the attacker itself
 * ([GameObjectFilter.notTriggeringEntity]; `excludeSelf` would exclude this enchantment). The count
 * is taken as the ability resolves and locked in for the turn.
 */
val SharedAnimosity = card("Shared Animosity") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Enchantment"
    oracleText = "Whenever a creature you control attacks, it gets +1/+0 until end of turn for each other attacking creature that shares a creature type with it."

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature.youControl()).attacks()
        effect = Effects.ModifyStats(
            power = DynamicAmount.AggregateBattlefield(
                Player.Each,
                GameObjectFilter.Creature.attacking()
                    .sharingCreatureTypeWith(EffectTarget.TriggeringEntity)
                    .notTriggeringEntity()
            ),
            toughness = DynamicAmount.Fixed(0),
            target = EffectTarget.TriggeringEntity
        )
        description = "Whenever a creature you control attacks, it gets +1/+0 until end of turn for each other attacking creature that shares a creature type with it."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "104"
        artist = "Chuck Lukacs"
        flavorText = "\"It is the nature of souls that they burn more brightly together than apart.\"\n—Vessifrus, flamekin demagogue"
        imageUri = "https://cards.scryfall.io/normal/front/f/e/fe332c46-90f0-4cc0-8bf1-35a3934ff8a0.jpg?1783942783"
        ruling("2008-04-01", "This ability counts creatures, not creature types. For example, if you attack with five creatures — an Elf Shaman, an Elf Warrior, a Goblin Shaman, an Elemental, and a creature with all creature types — the ability will trigger five times. Those creatures will get +3/+0, +2/+0, +2/+0, +1/+0, and +4/+0, respectively.")
        ruling("2008-04-01", "In a Two-Headed Giant game, only creatures you control trigger the ability and get the bonus, but your teammate's attacking creatures are included in the calculation of those bonuses.")
    }
}
