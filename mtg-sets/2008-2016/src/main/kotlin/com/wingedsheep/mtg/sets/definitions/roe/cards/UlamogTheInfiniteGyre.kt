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
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Ulamog, the Infinite Gyre
 * {11}
 * Legendary Creature — Eldrazi
 * 10/10
 * When you cast this spell, destroy target permanent.
 * Indestructible
 * Annihilator 4 (Whenever this creature attacks, defending player sacrifices four permanents of
 * their choice.)
 * When Ulamog is put into a graveyard from anywhere, its owner shuffles their graveyard into their
 * library.
 *
 *  - The cast trigger resolves before Ulamog itself, and even if Ulamog is countered (2018 ruling).
 *  - Annihilator is display-only in this SDK, so it is lowered by hand as the attack trigger it
 *    abbreviates (Pathrazer of Ulamog, Artisan of Kozilek).
 *  - The graveyard shuffle functions from the graveyard (the Lorwyn Incarnation shape); its
 *    controller there is Ulamog's owner, so "its owner shuffles their graveyard" is the
 *    controller's graveyard — Ulamog included, if it is still there.
 */
val UlamogTheInfiniteGyre = card("Ulamog, the Infinite Gyre") {
    manaCost = "{11}"
    colorIdentity = ""
    typeLine = "Legendary Creature — Eldrazi"
    power = 10
    toughness = 10
    oracleText = "When you cast this spell, destroy target permanent.\n" +
        "Indestructible\n" +
        "Annihilator 4 (Whenever this creature attacks, defending player sacrifices four permanents " +
        "of their choice.)\n" +
        "When Ulamog is put into a graveyard from anywhere, its owner shuffles their graveyard into " +
        "their library."

    triggeredAbility {
        trigger = Triggers.self.isCast()
        val permanent = target(TargetFilter.Permanent)
        effect = Effects.Destroy(permanent)
        description = "When you cast this spell, destroy target permanent."
    }

    keywords(Keyword.INDESTRUCTIBLE)

    keywordAbility(KeywordAbility.annihilator(4))

    // Annihilator 4 — the lowering of the display-only keyword ability above.
    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = Effects.Sacrifice(
            GameObjectFilter.Permanent,
            4,
            EffectTarget.PlayerRef(Player.DefendingPlayer)
        )
        description = "Annihilator 4"
    }

    triggeredAbility {
        triggerZone = Zone.GRAVEYARD
        trigger = Triggers.self.changesZone(to = Zone.GRAVEYARD)
        effect = Patterns.Library.shuffleGraveyardIntoLibrary(EffectTarget.Controller)
        description = "When Ulamog is put into a graveyard from anywhere, its owner shuffles their " +
            "graveyard into their library."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "12"
        artist = "Aleksi Briclot"
        imageUri = "https://cards.scryfall.io/normal/front/2/2/225e3cc6-34d0-4f81-9f49-162d97e2ea59.jpg?1783942011"
        ruling(
            "2018-12-07",
            "Ulamog's ability triggers as you cast it, and that ability resolves before the spell itself. " +
                "It resolves even if that spell is countered."
        )
        ruling(
            "2010-06-15",
            "Annihilator abilities trigger and resolve during the declare attackers step. The defending " +
                "player chooses and sacrifices the required number of permanents before they declare " +
                "blockers. Any creatures sacrificed this way won't be able to block."
        )
        ruling(
            "2010-06-15",
            "If a creature with annihilator is attacking a planeswalker, and the defending player chooses " +
                "to sacrifice that planeswalker, the attacking creature continues to attack. It may be " +
                "blocked. If it isn't blocked, it simply won't deal combat damage to anything."
        )
    }
}
