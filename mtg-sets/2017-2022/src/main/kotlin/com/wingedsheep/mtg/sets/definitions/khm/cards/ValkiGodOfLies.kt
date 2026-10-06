package com.wingedsheep.mtg.sets.definitions.khm.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantMayCastFromLinkedExile
import com.wingedsheep.sdk.scripting.OnEnterRun
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Valki, God of Lies // Tibalt, Cosmic Impostor — Kaldheim #114 (mythic), modal double-faced card.
 *
 * Front — Valki, God of Lies · {1}{B} · Legendary Creature — God · 2/1
 *   When Valki enters, each opponent reveals their hand. For each opponent, exile a creature card
 *   they revealed this way until Valki leaves the battlefield.
 *   {X}: Choose a creature card exiled with Valki with mana value X. Valki becomes a copy of that card.
 *
 * Back — Tibalt, Cosmic Impostor · {5}{B}{R} · Legendary Planeswalker — Tibalt · loyalty 5
 *   As Tibalt enters, you get an emblem with "You may play cards exiled with Tibalt, Cosmic
 *   Impostor, and you may spend mana as though it were mana of any color to cast those spells."
 *   +2: Exile the top card of each player's library.
 *   −3: Exile target artifact or creature.
 *   −8: Exile all graveyards. Add {R}{R}{R}.
 *
 * A [CardDefinition.modalDoubleFacedPermanent]: the caster picks a face (CR 712.11b), and the back
 * resolves onto the battlefield back face up with its own mana value and loyalty (CR 712.8f).
 *
 * **Valki's exile** is the modern "until" one-shot pair (CR 610.3), not a leaves trigger:
 * [Effects.MoveUntilSourceLeaves] returns each card to its owner's hand the moment Valki leaves, and
 * does nothing if Valki has already left when the ability resolves (ruling: the hands are still
 * revealed, no cards are exiled). Valki's controller makes the choice for each opponent
 * ([Chooser.SourceController] — the per-opponent loop rebinds "you" to the opponent).
 *
 * **Valki's copy** chooses on resolution among creature cards exiled with Valki whose mana value is
 * the X paid (ruling: chosen as the ability resolves; nothing happens if none match), then Valki
 * becomes a copy of that card's printed values with no duration ([Effects.EachPermanentBecomesCopyOfTarget]
 * with `sourceFromAnyZone`, the Lazav shape). The copy overwrites Valki's own abilities (ruling), and
 * the exiled cards stay linked to the object, so they still come back when it leaves.
 *
 * **Tibalt's emblem** is an as-enters replacement ([OnEnterRun]) — no trigger, nothing to respond
 * to — whose owned [GrantMayCastFromLinkedExile] is bound by the engine to this Tibalt object's
 * linked-exile pile, so its owner keeps playing those cards after Tibalt leaves (ruling) and a later
 * Tibalt's exiles need that Tibalt's own emblem. "Play" admits lands (filter Any) and the spend-mana
 * clause is `withAnyManaType`. Every loyalty ability exiles with `linkToSource`, face up.
 */
private val ValkiGodOfLiesFront = card("Valki, God of Lies") {
    manaCost = "{1}{B}"
    colorIdentity = "BR"
    typeLine = "Legendary Creature — God"
    power = 2
    toughness = 1
    oracleText = "When Valki enters, each opponent reveals their hand. For each opponent, exile a " +
        "creature card they revealed this way until Valki leaves the battlefield.\n" +
        "{X}: Choose a creature card exiled with Valki with mana value X. Valki becomes a copy of that card."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.ForEachPlayer(Player.EachOpponent, Effects.RevealHand(EffectTarget.Controller)) then
            Effects.ForEachPlayer(
                Player.EachOpponent,
                Effects.Pipeline {
                    val revealed = gather(CardSource.FromZone(Zone.HAND, Player.You, GameObjectFilter.Creature))
                    val chosen = chooseExactly(
                        1,
                        from = revealed,
                        chooser = Chooser.SourceController,
                        prompt = "Exile a creature card until Valki leaves the battlefield",
                        selectedLabel = "Exile",
                    )
                    run(Effects.MoveUntilSourceLeaves(chosen.asTarget, Zone.EXILE))
                }
            )
        description = "When Valki enters, each opponent reveals their hand. For each opponent, exile a " +
            "creature card they revealed this way until Valki leaves the battlefield."
    }

    activatedAbility {
        cost = Costs.Mana("{X}")
        effect = Effects.Pipeline {
            val pile = gather(CardSource.FromLinkedExile())
            val chosen = chooseExactly(
                1,
                from = pile,
                filter = GameObjectFilter.Creature.manaValueEqualsX(),
                prompt = "Choose a creature card exiled with Valki for Valki to copy",
                selectedLabel = "Copy",
            )
            run(
                Effects.EachPermanentBecomesCopyOfTarget(
                    target = chosen.asTarget,
                    affected = EffectTarget.Self,
                    sourceFromAnyZone = true,
                )
            )
        }
        description = "{X}: Choose a creature card exiled with Valki with mana value X. Valki becomes a copy of that card."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "114"
        artist = "Yongjae Choi"
        imageUri = "https://cards.scryfall.io/normal/front/e/a/ea7e4c65-b4c4-4795-9475-3cba71c50ea5.jpg?1783928244"
        ruling("2021-02-05", "If Valki leaves the battlefield before its enters-the-battlefield ability resolves, each opponent will reveal their hand, but no cards will be exiled.")
        ruling("2021-02-05", "You don't choose which creature card exiled with Valki that Valki will become a copy of until that ability is resolving. (In many cases, the value you chose for X will give away your intentions.)")
        ruling("2021-02-05", "If there are no creature cards exiled with Valki with mana value equal to the value of X as Valki's activated ability resolves, nothing happens. God of Lies, indeed.")
        ruling("2021-02-05", "Valki copies the printed values of the exiled creature card. Notably, once Valki becomes a copy of another creature card, he won't have his own printed activated ability.")
        ruling("2021-02-05", "If another object becomes a copy of Valki, it will become whatever Valki is copying. That object remains a copy even if Valki leaves the battlefield.")
    }
}

private val TibaltCosmicImpostorBack = card("Tibalt, Cosmic Impostor") {
    manaCost = "{5}{B}{R}"
    colorIdentity = "BR"
    typeLine = "Legendary Planeswalker — Tibalt"
    startingLoyalty = 5
    oracleText = "As Tibalt enters, you get an emblem with \"You may play cards exiled with Tibalt, " +
        "Cosmic Impostor, and you may spend mana as though it were mana of any color to cast those spells.\"\n" +
        "+2: Exile the top card of each player's library.\n" +
        "−3: Exile target artifact or creature.\n" +
        "−8: Exile all graveyards. Add {R}{R}{R}."

    replacementEffect(
        OnEnterRun(
            Effects.CreatePermanentEmblem(
                ownedStaticAbilities = listOf(
                    GrantMayCastFromLinkedExile(filter = GameObjectFilter.Any, withAnyManaType = true)
                ),
                emblemDescription = "You may play cards exiled with Tibalt, Cosmic Impostor, and you may " +
                    "spend mana as though it were mana of any color to cast those spells.",
            )
        )
    )

    loyaltyAbility(2) {
        effect = Effects.Pipeline {
            val top = gather(CardSource.TopOfLibrary(count = 1, player = Player.Each))
            exile(top, linkToSource = true)
        }
        description = "Exile the top card of each player's library."
    }

    loyaltyAbility(-3) {
        val permanent = target(TargetFilter(GameObjectFilter.Artifact or GameObjectFilter.Creature))
        effect = Effects.ExileLinkedToSource(permanent)
        description = "Exile target artifact or creature."
    }

    loyaltyAbility(-8) {
        effect = Effects.Pipeline {
            val graveyards = gather(CardSource.FromZone(Zone.GRAVEYARD, Player.Each))
            exile(graveyards, linkToSource = true)
            run(Effects.AddMana(Color.RED, 3))
        }
        description = "Exile all graveyards. Add {R}{R}{R}."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "114"
        artist = "Yongjae Choi"
        imageUri = "https://cards.scryfall.io/normal/back/e/a/ea7e4c65-b4c4-4795-9475-3cba71c50ea5.jpg?1783928244"
        ruling("2021-02-05", "The emblem given to you by Tibalt allows you to play cards exiled with that specific Tibalt, Cosmic Impostor, even after that Tibalt leaves the battlefield. If a different Tibalt, Cosmic Impostor comes under your control, it's a new object (even if it's represented by the same card). Of course, the new Tibalt will also give you an emblem so you can play the cards he exiles.")
        ruling("2021-02-05", "The cards exiled by Tibalt's loyalty abilities are all exiled face up.")
        ruling("2021-02-05", "Playing the cards exiled with Tibalt follows the normal rules for playing those cards. You must pay their costs, if any, and you must follow all applicable timing rules. For example, if one of the cards is a sorcery card, you can cast that card by paying its mana cost only during your main phase while the stack is empty.")
        ruling("2021-02-05", "Unless an effect allows you to play additional lands that turn, you can play land cards exiled with Tibalt only if you haven't played a land yet that turn.")
        ruling("2021-02-05", "While resolving Tibalt's last ability, you'll add {R}{R}{R} even if you don't exile any cards.")
    }
}

val ValkiGodOfLies: CardDefinition = CardDefinition.modalDoubleFacedPermanent(
    frontFace = ValkiGodOfLiesFront,
    backFace = TibaltCosmicImpostorBack,
)
