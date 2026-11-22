package com.wingedsheep.mtg.sets.definitions.snc.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Obscura Interceptor (SNC #209)
 * {1}{W}{U}{B}  Creature — Octopus Wizard  3/1
 *
 * Flash
 * Lifelink
 * When this creature enters, it connives. When it connives this way, return up to one target
 * spell to its owner's hand.
 *
 * Modeling: "When it connives this way" is a reflexive trigger (CR 603.12) whose action is the
 * connive itself, so the "up to one target spell" is chosen after the draw and discard, when the
 * reflexive trigger goes on the stack. Conniving always happens — even if the Interceptor has left
 * the battlefield by the time the ETB trigger resolves — so the reflexive trigger is mandatory
 * (`optional = false`). The bounce is not a counter, so it works on a spell that can't be countered.
 */
val ObscuraInterceptor = card("Obscura Interceptor") {
    manaCost = "{1}{W}{U}{B}"
    colorIdentity = "WUB"
    typeLine = "Creature — Octopus Wizard"
    power = 3
    toughness = 1
    oracleText = "Flash\n" +
        "Lifelink\n" +
        "When this creature enters, it connives. When it connives this way, return up to one target " +
        "spell to its owner's hand. (To have a creature connive, draw a card, then discard a card. " +
        "If you discarded a nonland card, put a +1/+1 counter on that creature.)"

    keywords(Keyword.FLASH, Keyword.LIFELINK)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.ReflexiveTrigger(action = Effects.Connive(), optional = false) {
            val spell = target(TargetFilter.SpellOnStack, optional = true)
            effect = Effects.ReturnSpellOrPermanentToOwnersHand(spell)
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "209"
        artist = "Jason A. Engle"
        imageUri = "https://cards.scryfall.io/normal/front/0/8/08886346-2405-47ee-997f-c0fb7776c783.jpg?1783923074"
        ruling(
            "2022-04-29",
            "If a resolving spell or ability instructs a specific creature to connive but that " +
                "creature has left the battlefield, the creature still connives. Abilities that " +
                "trigger \"when [that creature] connives,\" such as that of Obscura Interceptor, will trigger."
        )
    }
}
