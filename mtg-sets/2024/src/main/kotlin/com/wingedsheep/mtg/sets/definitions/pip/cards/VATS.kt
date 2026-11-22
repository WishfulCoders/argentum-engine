package com.wingedsheep.mtg.sets.definitions.pip.cards

import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.*
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.values.CardNumericProperty

val VATS = card("V.A.T.S.") {
    manaCost = "{2}{B}{B}"
    typeLine = "Instant"
    oracleText = "Split second (As long as this spell is on the stack, players can't cast spells or activate abilities that aren't mana abilities.)\n" +
        "Choose any number of target creatures with equal toughness. Destroy the chosen creatures."
    keywords(Keyword.SPLIT_SECOND)
    spell {
        targets(TargetFilter.Creature, unlimited = true, sameNumericProperty = CardNumericProperty.TOUGHNESS)
        effect = Effects.Pipeline {
            val chosen = gather(CardSource.ChosenTargets)
            destroy(chosen)
        }
    }
    metadata {
        rarity = Rarity.RARE
        collectorNumber = "50"
        artist = "Diego Gisbert"
        flavorText = "The Vault-Tec Assisted Targeting System redefines combat. Get your V.A.T.S. today!"
        imageUri = "https://cards.scryfall.io/normal/front/5/1/512df222-0db0-4b24-9372-d0c01b181b91.jpg?1783912406"
        ruling("2024-03-08", "If you choose just one target for V.A.T.S., that creature will be destroyed when V.A.T.S. resolves as long as it’s still a legal target, regardless of whether or not its toughness has changed since V.A.T.S. was cast.")
        ruling("2024-03-08", "In the rare case where the legal targets no longer all have equal toughness (probably because of a triggered ability or special action) when V.A.T.S. tries to resolve, it won’t resolve. None of the chosen creatures will be destroyed.")
        ruling("2024-03-08", "Players may turn face-down creatures face up while a spell with split second is on the stack.")
        ruling("2024-03-08", "Split second doesn’t stop triggered abilities from triggering. If one does, its controller puts it on the stack and chooses targets for it, if any. Those abilities will resolve as normal.")
    }
}
