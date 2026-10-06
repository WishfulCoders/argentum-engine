package com.wingedsheep.mtg.sets.definitions.leg.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CopyRecipient

/**
 * Chain Lightning
 * {R}
 * Sorcery
 * Chain Lightning deals 3 damage to any target. Then that player or that permanent's controller may
 * pay {R}{R}. If the player does, they may copy this spell and may choose a new target for that copy.
 *
 * The Onslaught chain shape with a **mana** copy cost: the damaged player (or the damaged
 * permanent's controller) pays {R}{R} mid-resolution through the mana-payment window, and is only
 * asked when they can (CR 118.3). The copy is controlled by whoever paid for it and is itself a
 * Chain Lightning, so the chain continues for as long as someone keeps paying.
 */
val ChainLightning = card("Chain Lightning") {
    manaCost = "{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Chain Lightning deals 3 damage to any target. Then that player or that permanent's controller may pay {R}{R}. If the player does, they may copy this spell and may choose a new target for that copy."

    spell {
        val t = target(Targets.Any)
        effect = Effects.ChainCopy(
            action = Effects.DealDamage(3, t),
            target = t,
            offerTo = CopyRecipient.AFFECTED_PLAYER,
            copyTarget = Targets.Any,
            copyCost = Costs.pay.Mana("{R}{R}")
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "137"
        artist = "Sandra Everingham"
        imageUri = "https://cards.scryfall.io/normal/front/b/5/b5883762-ca0a-4932-8d2a-41a45796a5f8.jpg?1783948058"
        ruling("2022-12-08", "As Chain Lightning resolves, either the original or one of the copies, the targeted player or the controller of the targeted permanent may copy it. The copy has the same text, target, and color as the resolving spell, though the player creating the copy may choose a new target for it. Once that copy is created (or not), the Chain Lightning finishes resolving and leaves the stack.")
        ruling("2022-12-08", "The player putting the copy of the spell on the stack controls that copy.")
        ruling("2022-12-08", "The copy of Chain Lightning is created on the stack, so it's not cast. Abilities that trigger when a player casts a spell won't trigger. Players may respond to that spell before it resolves.")
        ruling("2022-12-08", "If the targeted player or permanent is an illegal target as Chain Lightning tries to resolve, the spell doesn't resolve and none of its effects happen. It can't be copied.")
    }
}
