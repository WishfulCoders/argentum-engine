package com.wingedsheep.mtg.sets.definitions.roe.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.ProtectionScope
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Emrakul, the Aeons Torn — Rise of the Eldrazi #4
 * {15} · Legendary Creature — Eldrazi · 15/15 · Mythic
 *
 * This spell can't be countered.
 * When you cast this spell, take an extra turn after this one.
 * Flying, protection from spells that are one or more colors, annihilator 6
 * When Emrakul is put into a graveyard from anywhere, its owner shuffles their graveyard into
 * their library.
 *
 * Modeling notes:
 *  - The extra turn is a **cast trigger** (`Triggers.self.isCast()`, CR 601.2i): it goes
 *    on the stack above Emrakul and resolves first, even if Emrakul leaves the stack (ruling).
 *  - [ProtectionScope.ColoredSpells] is the CR 702.16a quality "spells that are one or more
 *    colors": colored spells can't target Emrakul (702.16b) and their damage to it is prevented
 *    (702.16e), while
 *    abilities (an enters trigger of a colored permanent, say) and colorless spells still reach it,
 *    and a colored creature on the battlefield isn't a spell so it may block Emrakul.
 *  - Annihilator is a display-only [KeywordAbility.Numeric] in the SDK (as on Artisan of Kozilek
 *    and Pathrazer of Ulamog), so annihilator 6 (CR 702.86a) is lowered by hand as the attack
 *    trigger it abbreviates: defending player sacrifices six permanents of their choice.
 *  - The shuffle triggers from the graveyard (`triggerZone = GRAVEYARD`): the card has the ability
 *    in the zone it arrives in, so it fires whether Emrakul is destroyed, discarded, milled or
 *    countered. A triggered ability is controlled by its source's controller (CR 603.3a), and a
 *    card in a graveyard has none, so that is its owner (CR 108.4a) — [EffectTarget.Controller]
 *    is "its owner". The whole graveyard is shuffled in even if Emrakul has left it by then.
 */
val EmrakulTheAeonsTorn = card("Emrakul, the Aeons Torn") {
    manaCost = "{15}"
    colorIdentity = ""
    typeLine = "Legendary Creature — Eldrazi"
    power = 15
    toughness = 15
    oracleText = "This spell can't be countered.\n" +
        "When you cast this spell, take an extra turn after this one.\n" +
        "Flying, protection from spells that are one or more colors, annihilator 6\n" +
        "When Emrakul is put into a graveyard from anywhere, its owner shuffles their graveyard " +
        "into their library."

    cantBeCountered = true

    triggeredAbility {
        trigger = Triggers.self.isCast()
        effect = Effects.TakeExtraTurn()
        description = "When you cast this spell, take an extra turn after this one."
    }

    keywords(Keyword.FLYING)
    keywordAbility(KeywordAbility.Protection(ProtectionScope.ColoredSpells))
    keywordAbility(KeywordAbility.annihilator(6))

    // Annihilator 6 — the lowering of the display-only keyword ability above.
    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = Effects.Sacrifice(
            GameObjectFilter.Permanent,
            6,
            EffectTarget.PlayerRef(Player.DefendingPlayer)
        )
        description = "Annihilator 6"
    }

    triggeredAbility {
        triggerZone = Zone.GRAVEYARD
        trigger = Triggers.self.changesZone(to = Zone.GRAVEYARD)
        effect = Patterns.Library.shuffleGraveyardIntoLibrary(EffectTarget.Controller)
        description = "When Emrakul is put into a graveyard from anywhere, its owner shuffles " +
            "their graveyard into their library."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "4"
        artist = "Mark Tedin"
        imageUri = "https://cards.scryfall.io/normal/front/6/7/67600383-bbb8-411c-b8e6-2296650bc747.jpg?1783942013"
        ruling("2018-12-07", "Emrakul's second ability triggers as you cast it, and that ability resolves before the spell itself. It resolves even if that spell is somehow removed from the stack.")
        ruling("2018-12-07", "Emrakul can be targeted by spells that try to counter it (such as Ionize). Those spells will resolve, but the part of their effect that would counter Emrakul won't do anything. Any other effects those spells have will work as normal.")
        ruling("2018-12-07", "\"Colored spells\" is not synonymous with \"colored cards.\" For example, even though creatures are spells when they're cast, they're not spells when they're on the battlefield and can block Emrakul; and triggered abilities of permanents entering the battlefield (such as that of Banishing Light) can target it.")
        ruling("2018-12-07", "Emrakul can't be the target of colored Aura spells, but colored Auras can be put onto the battlefield enchanting it.")
        ruling("2010-06-15", "Annihilator abilities trigger and resolve during the declare attackers step. The defending player chooses and sacrifices the required number of permanents before they declare blockers. Any creatures sacrificed this way won't be able to block.")
    }
}
