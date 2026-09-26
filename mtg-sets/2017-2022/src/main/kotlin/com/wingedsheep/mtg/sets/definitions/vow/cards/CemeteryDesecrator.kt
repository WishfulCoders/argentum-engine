package com.wingedsheep.mtg.sets.definitions.vow.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.mode
import com.wingedsheep.sdk.dsl.unaryMinus
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Cemetery Desecrator
 * {4}{B}{B}
 * Creature — Zombie
 * 4/4
 *
 * Menace
 * When this creature enters or dies, exile another card from a graveyard. When you do, choose one —
 * • Remove X counters from target permanent, where X is the mana value of the exiled card.
 * • Target creature an opponent controls gets -X/-X until end of turn, where X is the mana value
 *   of the exiled card.
 *
 * Shape notes:
 *
 *  - **"enters or dies" is two triggered abilities**, not one — the corpus convention (Reputable
 *    Merchant, Thawbringer). They share [desecrate] because the payoff clause is identical.
 *  - **"When you do" is CR 603.12**, a genuine second stack object, so the exile is the
 *    [ReflexiveTriggerEffect]'s `action` and the modal is its `reflexiveEffect`. The exile is
 *    *mandatory* ("exile another card from a graveyard", no "may"), hence `optional = false`;
 *    `ReflexiveTriggerEffectExecutor.isActionFeasible` still suppresses the whole thing when every
 *    graveyard is empty, which is what stops the modal firing off an exile that never happened.
 *  - **The modal is chosen as the reflexive ability goes on the stack**, not on resolution — a
 *    top-level `ModalEffect` on a triggered ability is caught by `TriggerProcessor` (CR 603.3c),
 *    and the reflexive trigger is an ordinary triggered ability by the time it gets there. That is
 *    the correct timing here: the opponent sees which mode was picked while it is still
 *    respondable, and the mode's target only *becomes* a target at that moment (ward, "becomes the
 *    target of" triggers).
 *  - **"another"** excludes the Desecrator's own card, which matters only on the dies trigger —
 *    by then it is sitting in a graveyard itself. Entity ids are stable across zone changes, so
 *    `GameObjectFilter.Any.notSourceItself()` is exactly the printed word.
 *  - **X** is `DynamicAmount.StoredCardManaValue(EXILED_CARD)` in both modes, read off the pipeline collection
 *    the action half stored; the reflexive trigger carries that pipeline forward
 *    (`ReflexiveAbilityTriggeredEvent.carriedPipeline`). Mode 1 removing "X counters" is
 *    [Effects.RemoveCounterOfAnyKind] with a dynamic count — the player picks which *kinds* come
 *    off, never whether, and the executor clamps the floor to what the permanent actually carries
 *    so a permanent with fewer than X counters simply loses all of them.
 */
// The exiled card is chosen by the action half and read back by the reflexive half, which is a
// sibling of the action pipeline rather than inside it — so the key is named.
private const val EXILED_CARD = "exiledCard"

private val desecrate: Effect =
    Effects.ReflexiveTrigger(
        optional = false,
        action = Effects.Pipeline {
            val graveyardCards = gather(
                CardSource.FromZone(
                    zone = Zone.GRAVEYARD,
                    player = Player.Each,
                    filter = GameObjectFilter.Any.notSourceItself()
                )
            )
            val exiledCard = chooseExactly(
                1,
                from = graveyardCards,
                chooser = Chooser.Controller,
                prompt = "Exile another card from a graveyard",
                showAllCards = true,
                name = EXILED_CARD
            )
            exile(exiledCard)
        },
        reflexiveEffect = ModalEffect.chooseOne(
            mode("Remove X counters from target permanent, " +
                "where X is the mana value of the exiled card") {
                val permanent = target(TargetFilter.Permanent)
                effect = Effects.RemoveCounterOfAnyKind(
                    target = permanent,
                    count = DynamicAmounts.manaValueOf(EXILED_CARD)
                )
            },
            mode("Target creature an opponent controls gets -X/-X until end of turn, " +
                "where X is the mana value of the exiled card") {
                val creature = target(TargetFilter.Creature.opponentControls())
                effect = Effects.ModifyStats(
                    -DynamicAmounts.manaValueOf(EXILED_CARD),
                    -DynamicAmounts.manaValueOf(EXILED_CARD),
                    creature
                )
            }
        ),
        descriptionOverride = "Exile another card from a graveyard. When you do, choose one — " +
            "remove X counters from target permanent, or target creature an opponent controls gets " +
            "-X/-X until end of turn, where X is the mana value of the exiled card"
    )

val CemeteryDesecrator = card("Cemetery Desecrator") {
    manaCost = "{4}{B}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Zombie"
    power = 4
    toughness = 4
    oracleText = "Menace\n" +
        "When this creature enters or dies, exile another card from a graveyard. " +
        "When you do, choose one —\n" +
        "• Remove X counters from target permanent, where X is the mana value of the exiled card.\n" +
        "• Target creature an opponent controls gets -X/-X until end of turn, where X is the " +
        "mana value of the exiled card."

    keywords(Keyword.MENACE)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = desecrate
    }

    triggeredAbility {
        trigger = Triggers.self.dies()
        effect = desecrate
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "100"
        artist = "Dan Murayama Scott"
        imageUri = "https://cards.scryfall.io/normal/front/4/8/48da33b1-d59c-43f1-8e55-480096b674e5.jpg?1783924869"
    }
}
