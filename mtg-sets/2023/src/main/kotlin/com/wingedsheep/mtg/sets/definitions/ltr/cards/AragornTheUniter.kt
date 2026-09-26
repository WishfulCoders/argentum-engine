package com.wingedsheep.mtg.sets.definitions.ltr.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Aragorn, the Uniter
 * {R}{G}{W}{U}
 * Legendary Creature — Human Noble
 * 5/5
 *
 * Whenever you cast a white spell, create a 1/1 white Human Soldier creature token.
 * Whenever you cast a blue spell, scry 2.
 * Whenever you cast a red spell, Aragorn deals 3 damage to target opponent.
 * Whenever you cast a green spell, target creature gets +4/+4 until end of turn.
 */
val AragornTheUniter = card("Aragorn, the Uniter") {
    manaCost = "{R}{G}{W}{U}"
    colorIdentity = "GRUW"
    typeLine = "Legendary Creature — Human Noble"
    power = 5
    toughness = 5
    oracleText = "Whenever you cast a white spell, create a 1/1 white Human Soldier creature token.\n" +
        "Whenever you cast a blue spell, scry 2.\n" +
        "Whenever you cast a red spell, Aragorn deals 3 damage to target opponent.\n" +
        "Whenever you cast a green spell, target creature gets +4/+4 until end of turn."

    triggeredAbility {
        trigger = Triggers.you.casts(GameObjectFilter.Any.withColor(Color.WHITE))
        effect = Effects.CreateToken(
            count = 1,
            power = 1,
            toughness = 1,
            colors = setOf(Color.WHITE),
            creatureTypes = setOf("Human", "Soldier"),
            imageUri = "https://cards.scryfall.io/normal/front/a/6/a6181330-7521-4ec6-be6c-b35487c2d2d4.jpg?1699974464"
        )
    }

    triggeredAbility {
        trigger = Triggers.you.casts(GameObjectFilter.Any.withColor(Color.BLUE))
        effect = Patterns.Library.scry(2)
    }

    triggeredAbility {
        trigger = Triggers.you.casts(GameObjectFilter.Any.withColor(Color.RED))
        val opponent = target(Targets.Opponent)
        effect = Effects.DealDamage(
            amount = 3,
            target = opponent,
            damageSource = EffectTarget.Self
        )
    }

    triggeredAbility {
        trigger = Triggers.you.casts(GameObjectFilter.Any.withColor(Color.GREEN))
        val creature = target(TargetFilter.Creature)
        effect = Effects.ModifyStats(4, 4, creature)
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "192"
        artist = "Javier Charro"
        imageUri = "https://cards.scryfall.io/normal/front/e/9/e98d5321-ec09-456c-a9ea-c8ca2cfc6205.jpg?1686969644"
    }
}
