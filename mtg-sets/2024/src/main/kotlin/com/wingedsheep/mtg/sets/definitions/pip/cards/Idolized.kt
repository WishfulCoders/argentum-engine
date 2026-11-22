package com.wingedsheep.mtg.sets.definitions.pip.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantTriggeredAbility
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.scripting.events.AttackPredicate
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Idolized — Fallout #17
 * {1}{W} · Enchantment — Aura
 *
 * Enchant creature
 * Enchanted creature has "Whenever this creature attacks alone, it gets +X/+X until end of turn,
 * where X is the number of nonland permanents you control."
 *
 * The ability is *granted* (a quoted ability), so it is a [GrantTriggeredAbility] over
 * [Filters.EnchantedCreature] rather than an attached trigger on the Aura: the trigger belongs to the
 * creature and its controller. "You" inside the quote is therefore the creature's controller, which
 * is what [Player.You] reads for a granted ability — on an opponent's creature X counts *their*
 * nonland permanents. X is a resolution-time count (no "as this ability triggers" wording), so the
 * [DynamicAmounts.battlefield] count is evaluated when the pump resolves.
 *
 * "Attacks alone" is [AttackPredicate.Alone]: the only creature declared as an attacker; creatures
 * removed from combat or put onto the battlefield attacking later don't change that (see ruling).
 */
val Idolized = card("Idolized") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\n" +
        "Enchanted creature has \"Whenever this creature attacks alone, it gets +X/+X until end of turn, " +
        "where X is the number of nonland permanents you control.\""

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    staticAbility {
        val x = DynamicAmounts.battlefield(Player.You, GameObjectFilter.NonlandPermanent).count()
        ability = GrantTriggeredAbility(
            ability = TriggeredAbility.create(
                trigger = Triggers.self.attacks(setOf(AttackPredicate.Alone)),
                effect = Effects.ModifyStats(x, x, EffectTarget.Self),
                descriptionOverride = "Whenever this creature attacks alone, it gets +X/+X until end of turn, " +
                    "where X is the number of nonland permanents you control.",
            ),
            filter = Filters.EnchantedCreature,
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "17"
        artist = "Kim Sokol"
        flavorText = "Renowned for your extensive support and goodwill, you are idolized by the community."
        imageUri = "https://cards.scryfall.io/normal/front/6/8/687133cb-3754-475c-9e21-b6a33aa58bb0.jpg?1783912417"
        ruling(
            "2024-03-08",
            "A creature attacks alone if it's the only creature declared as an attacker during the declare attackers " +
                "step (including creatures controlled by your teammates, if applicable). For example, the ability granted " +
                "by Idolized won't trigger if you attack with multiple creatures and all but one of them are removed from " +
                "combat. Similarly, creatures that enter the battlefield attacking later in combat won't be considered when " +
                "determining whether or not a creature attacked alone.",
        )
    }
}
