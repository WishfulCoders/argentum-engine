package com.wingedsheep.mtg.sets.definitions.khm.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ExtraOnceOnlyActivations
import com.wingedsheep.sdk.scripting.OnceOnlyAbilityKind
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.ManaExpiry

/**
 * Birgi, God of Storytelling // Harnfel, Horn of Bounty — Kaldheim #123 (canonical printing)
 * Legendary Creature — God 3/3 // Legendary Artifact · Modal double-faced card
 *
 * Front: "Whenever you cast a spell, add {R}. Until end of turn, you don't lose this mana as steps
 * and phases end. Creatures you control can boast twice during each of your turns rather than
 * once." The mana trigger uses the stack (it isn't a mana ability, per the ruling); the boast
 * clause raises boast's once-each-turn limit (CR 702.142a) by one, during your turns only —
 * [ExtraOnceOnlyActivations] over [OnceOnlyAbilityKind.BOAST], gated on [Conditions.IsYourTurn].
 * A creature must still have attacked this turn to boast at all.
 *
 * Back: "Discard a card: Exile the top two cards of your library. You may play those cards this
 * turn." The discarded card isn't one of "those cards".
 */
private val BirgiGodOfStorytellingFront = card("Birgi, God of Storytelling") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Legendary Creature — God"
    power = 3
    toughness = 3
    oracleText = "Whenever you cast a spell, add {R}. Until end of turn, you don't lose this mana as " +
        "steps and phases end.\nCreatures you control can boast twice during each of your turns " +
        "rather than once."

    triggeredAbility {
        trigger = Triggers.you.casts()
        effect = Effects.AddMana(Color.RED, 1, expiry = ManaExpiry.KEPT_UNTIL_END_OF_TURN)
        description = "Whenever you cast a spell, add {R}. Until end of turn, you don't lose this " +
            "mana as steps and phases end."
    }

    staticAbility {
        ability = ExtraOnceOnlyActivations(
            kind = OnceOnlyAbilityKind.BOAST,
            extraActivations = 1,
            condition = Conditions.IsYourTurn
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "123"
        artist = "Eric Deschamps"
        flavorText = "\"This is a tale to make all Kaldheim tremble...\""
        imageUri = "https://cards.scryfall.io/normal/front/4/4/44657ab1-0a6a-4a5f-9688-86f239083821.jpg?1783928241"
        ruling("2021-02-05", "Birgi's first ability uses the stack and can be responded to. It isn't a mana ability.")
        ruling("2021-02-05", "You must still attack with a creature you control to activate its boast ability.")
    }
}

private val HarnfelHornOfBountyBack = card("Harnfel, Horn of Bounty") {
    manaCost = "{4}{R}"
    colorIdentity = "R"
    typeLine = "Legendary Artifact"
    oracleText = "Discard a card: Exile the top two cards of your library. You may play those cards this turn."

    activatedAbility {
        cost = Costs.DiscardCard
        effect = Effects.Pipeline {
            val exiled = gather(CardSource.TopOfLibrary(2))
            exile(exiled)
            run(Effects.GrantMayPlayFromExile(from = exiled))
        }
        description = "Exile the top two cards of your library. You may play those cards this turn."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "123"
        artist = "Eric Deschamps"
        flavorText = "Like her well of stories, Birgi's horn never runs dry."
        imageUri = "https://cards.scryfall.io/normal/back/4/4/44657ab1-0a6a-4a5f-9688-86f239083821.jpg?1783928241"
        ruling("2021-02-05", "The cards exiled by Harnfel's activated ability are exiled face up.")
        ruling("2021-02-05", "You may play those cards that turn even if Harnfel leaves the battlefield or another player gains control of it.")
        ruling("2021-02-05", "Unless an effect allows you to play additional lands that turn, you can play land cards exiled with Harnfel only if you haven't played a land yet that turn. In most cases, if you exile two land cards with Harnfel, you won't be able to play both.")
        ruling("2021-02-05", "Any cards exiled with Harnfel that you don't play remain exiled.")
        ruling("2021-02-05", "The card you discard to activate the ability isn't part of \"those cards\" and can't be played, even if it somehow ends up in exile.")
    }
}

val BirgiGodOfStorytelling: CardDefinition = CardDefinition.modalDoubleFacedPermanent(
    frontFace = BirgiGodOfStorytellingFront,
    backFace = HarnfelHornOfBountyBack,
)
