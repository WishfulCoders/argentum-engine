package com.wingedsheep.mtg.sets.definitions.iko.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Lutri, the Spellchaser
 * {1}{U/R}{U/R}
 * Legendary Creature — Elemental Otter
 * 3/2
 * Companion — Each nonland card in your starting deck has a different name.
 * Flash
 * When Lutri enters, if you cast it, copy target instant or sorcery spell you control. You may
 * choose new targets for the copy.
 *
 * The companion clause is a deckbuilding / outside-the-game ability (CR 702.139): it has no effect
 * while the card is in the starting deck or on the battlefield, so it is carried in the oracle text
 * only. The engine has no companion zone, so Lutri plays as a main-deck card; its in-game abilities
 * are complete.
 *
 * "If you cast it" is an intervening if ([Conditions.WasCast]): Lutri put onto the battlefield
 * without being cast doesn't trigger. The copy prompt for new targets is built into
 * [Effects.CopyTargetSpell]; a spell without targets is copied all the same.
 */
val LutriTheSpellchaser = card("Lutri, the Spellchaser") {
    manaCost = "{1}{U/R}{U/R}"
    colorIdentity = "UR"
    typeLine = "Legendary Creature — Elemental Otter"
    power = 3
    toughness = 2
    oracleText = "Companion — Each nonland card in your starting deck has a different name. " +
        "(If this card is your chosen companion, you may put it into your hand from outside the game for {3} as a sorcery.)\n" +
        "Flash\n" +
        "When Lutri enters, if you cast it, copy target instant or sorcery spell you control. " +
        "You may choose new targets for the copy."

    keywords(Keyword.FLASH)

    triggeredAbility {
        trigger = Triggers.self.enters()
        interveningIf = Conditions.WasCast
        val spell = target(TargetFilter.InstantOrSorcerySpellOnStack.youControl())
        effect = Effects.CopyTargetSpell(target = spell)
        description = "When Lutri enters, if you cast it, copy target instant or sorcery spell you control. " +
            "You may choose new targets for the copy."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "227"
        artist = "Lie Setiawan"
        imageUri = "https://cards.scryfall.io/normal/front/f/b/fb1189c9-7842-466e-8238-1e02677d8494.jpg?1783931010"

        ruling("2020-04-17", "Lutri's ability can copy any instant or sorcery spell you control, not just one with targets.")
        ruling("2020-04-17", "The copy is created on the stack, so it's not \"cast.\" Abilities that trigger when a player casts a spell won't trigger. It will resolve before the original spell does.")
        ruling("2020-04-17", "The copy will have the same targets as the spell it's copying unless you choose new ones. You may change any number of the targets, including all of them or none of them. If, for one of the targets, you can't choose a new legal target, then it remains unchanged (even if the current target is illegal).")
        ruling("2020-04-17", "If the spell that's copied is modal (that is, it says \"Choose one —\" or the like), the copy will have the same mode. A different mode can't be chosen.")
        ruling("2020-04-17", "If the spell that's copied has an X whose value was determined as it was cast, the copy will have the same value of X.")
        ruling("2020-04-17", "The companion ability has no effect if the card is in your starting deck and creates no restriction on putting a card with a companion ability into your starting deck.")
    }
}
