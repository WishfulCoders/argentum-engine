package com.wingedsheep.mtg.sets.definitions.lea.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Kudzu
 * {1}{G}{G}
 * Enchantment — Aura
 * Enchant land
 * When enchanted land becomes tapped, destroy it. That land's controller may attach this Aura to a
 * land of their choice.
 *
 * "It" and "that land" are the land that became tapped — the triggering entity — so the destroy
 * reads [EffectTarget.TriggeringEntity] and the re-attach runs under a `ForEachPlayer` over
 * [Player.ControllerOfTriggeringEntity], which rebinds the deciding player to that land's
 * controller (last-known controller once it is in the graveyard, CR 608.2h). The new host is a
 * resolution-time choice, not a target (2008-04-01 ruling), gathered *after* the destroy so the
 * destroyed land is no longer a candidate; `chooseUpTo(1)` is the "may". Attaching moves Kudzu
 * without changing its controller. Kudzu itself must still be on the battlefield to move: if it was
 * destroyed in response, the land is still destroyed but nothing is attached. A Kudzu left
 * unattached (declined, or no land left) is put into its owner's graveyard by the state-based
 * action once the ability finishes resolving.
 */
val Kudzu = card("Kudzu") {
    manaCost = "{1}{G}{G}"
    colorIdentity = "G"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant land\n" +
        "When enchanted land becomes tapped, destroy it. That land's controller may attach this " +
        "Aura to a land of their choice."

    auraTarget = TargetObject(filter = TargetFilter.Land)

    triggeredAbility {
        trigger = Triggers.attached.becomesTapped()
        effect = Effects.Destroy(EffectTarget.TriggeringEntity) then Effects.If(
            Conditions.SourceInZone(Zone.BATTLEFIELD),
            Effects.ForEachPlayer(
                Player.ControllerOfTriggeringEntity,
                Effects.Pipeline {
                    val lands = gather(CardSource.BattlefieldMatching(filter = GameObjectFilter.Land))
                    val newHost = chooseUpTo(
                        1,
                        from = lands,
                        prompt = "You may attach Kudzu to a land",
                        useTargetingUI = true,
                    )
                    ifNotEmpty(newHost) {
                        run(Effects.AttachEquipment(newHost.asTarget))
                    }
                }
            )
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "204"
        artist = "Mark Poole"
        imageUri = "https://cards.scryfall.io/normal/front/b/2/b2b72dcd-9ea1-4729-baae-ecd262fdff67.jpg?1783948675"
        ruling(
            "2008-04-01",
            "Because the ability isn't targeted, the controller of the destroyed land may attach it " +
                "to a land that can't be the target of abilities."
        )
        ruling(
            "2005-08-01",
            "If Kudzu is destroyed directly, or the land is destroyed by a spell or ability, then " +
                "Kudzu goes to the graveyard like any Aura would."
        )
        ruling("2004-10-04", "You can move it to any other player's land whenever you get to move it.")
    }
}
