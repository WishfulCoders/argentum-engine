package com.wingedsheep.mtg.sets.definitions.sos.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Aziza, Mage Tower Captain — Secrets of Strixhaven #174
 * {R}{W} · Legendary Creature — Djinn Sorcerer · 2/2
 *
 * Whenever you cast an instant or sorcery spell, you may tap three untapped creatures you
 * control. If you do, copy that spell. You may choose new targets for the copy.
 *
 * "you may tap three untapped creatures you control. If you do, [copy]" is an
 * [Effects.MayPay] whose payable cost is the Gather → Select-exactly-3 → Tap pipeline (same
 * idiom as Rent Is Due): the player may decline, and the cost only "pays" if three untapped
 * creatures they control are tapped — otherwise nothing happens. When paid, the triggering spell
 * ([EffectTarget.TriggeringEntity]) is copied via [Effects.CopyTargetSpell], which by default lets
 * the controller choose new targets for the copy (CR 707.10). The copy is created on the stack, so
 * it is not "cast" and does not re-trigger Aziza.
 */
val AzizaMageTowerCaptain = card("Aziza, Mage Tower Captain") {
    manaCost = "{R}{W}"
    colorIdentity = "RW"
    typeLine = "Legendary Creature — Djinn Sorcerer"
    power = 2
    toughness = 2
    oracleText = "Whenever you cast an instant or sorcery spell, you may tap three untapped " +
        "creatures you control. If you do, copy that spell. You may choose new targets for the copy."

    triggeredAbility {
        trigger = Triggers.you.casts(GameObjectFilter.InstantOrSorcery)
        val tapCost = Effects.Pipeline {
            val azizaTapPool = gather(
                CardSource.ControlledPermanents(
                    player = Player.You,
                    filter = GameObjectFilter.Creature.untapped(),
                )
            )
            val azizaToTap = chooseExactly(
                3,
                from = azizaTapPool,
                prompt = "Tap three untapped creatures you control",
                useTargetingUI = true
            )
            run(Effects.TapCollection(azizaToTap, tap = true))
        }
        effect = Effects.MayPay(
            cost = tapCost,
            then = Effects.CopyTargetSpell(target = EffectTarget.TriggeringEntity),
            descriptionOverride = "You may tap three untapped creatures you control. If you do, " +
                "copy that spell. You may choose new targets for the copy.",
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "174"
        artist = "Aurore Folny"
        flavorText = "\"Good play, team! Now let's run it again until it's perfect!\""
        imageUri = "https://cards.scryfall.io/normal/front/6/2/6261e89a-dbf1-481a-823e-6bb00be57195.jpg?1775938194"
    }
}
