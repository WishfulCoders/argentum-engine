package com.wingedsheep.mtg.sets.definitions.ori.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.AttackRequirementWindow
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Kytheon, Hero of Akros // Gideon, Battle-Forged — Magic Origins #23
 * {W} · Legendary Creature — Human Soldier 2/1 // Legendary Planeswalker — Gideon (loyalty 3)
 *
 * Front:
 *  - "At end of combat, if Kytheon and at least two other creatures attacked this combat, …" is an
 *    intervening-if (CR 603.4) on the end-of-combat step: [Conditions.SourceAttackedThisCombat] plus
 *    [Conditions.CreaturesAttackedThisCombat] over the per-combat attacker record, which still counts
 *    an attacker that died in combat and never one put onto the battlefield attacking (ruling
 *    2015-06-22). The transform is [Effects.ExileAndReturnTransformed] — a new object enters back
 *    face up with its printed loyalty.
 *
 * Back:
 *  - +2 is [Effects.MarkMustAttackDefender] with [AttackRequirementWindow.CONTROLLERS_NEXT_TURN]: the
 *    target must attack Gideon during its controller's next turn if able (CR 508.1d), and if Gideon
 *    has left the battlefield the requirement imposes nothing.
 *  - +1 grants indestructible until your next turn, then untaps.
 *  - 0 is Gideon, Champion of Justice's shape with a fixed 4/4: he stays a planeswalker and gains the
 *    creature types Human Soldier, and damage that would be dealt to him this turn is prevented.
 */
private val KytheonHeroOfAkrosFront = card("Kytheon, Hero of Akros") {
    manaCost = "{W}"
    colorIdentity = "W"
    typeLine = "Legendary Creature — Human Soldier"
    power = 2
    toughness = 1
    oracleText = "At end of combat, if Kytheon and at least two other creatures attacked this combat, " +
        "exile Kytheon, then return him to the battlefield transformed under his owner's control.\n" +
        "{2}{W}: Kytheon gains indestructible until end of turn."

    triggeredAbility {
        trigger = Triggers.anyPlayer.beginningOf(Step.END_COMBAT)
        interveningIf = Conditions.All(
            Conditions.SourceAttackedThisCombat,
            Conditions.CreaturesAttackedThisCombat(2, GameObjectFilter.Any.notSourceItself())
        )
        effect = Effects.ExileAndReturnTransformed(EffectTarget.Self)
        description = "At end of combat, if Kytheon and at least two other creatures attacked this combat, " +
            "exile Kytheon, then return him to the battlefield transformed under his owner's control."
    }

    activatedAbility {
        cost = Costs.Mana("{2}{W}")
        effect = Effects.GrantKeyword(Keyword.INDESTRUCTIBLE, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "23"
        artist = "Willian Murai"
        imageUri = "https://cards.scryfall.io/normal/front/5/8/58c39df6-b237-40d1-bdcb-2fe5d05392a9.jpg?1783938364"
        ruling("2015-06-22", "Kytheon's first ability will count creatures that attacked but are no longer on the battlefield (perhaps because they didn't survive combat damage being dealt). It will not count any creatures that were put onto the battlefield attacking, as those creatures were never declared as attackers.")
        ruling("2015-06-22", "In some rare cases, a spell or ability may cause one of these five cards to transform while it's a creature (front face up) on the battlefield. If this happens, the resulting planeswalker won't have any loyalty counters on it and will subsequently be put into its owner's graveyard.")
    }
}

private val GideonBattleForged = card("Gideon, Battle-Forged") {
    manaCost = ""
    colorIdentity = "W"
    colorIndicator = "W"
    typeLine = "Legendary Planeswalker — Gideon"
    startingLoyalty = 3
    oracleText = "+2: Up to one target creature an opponent controls attacks Gideon during its controller's next turn if able.\n" +
        "+1: Until your next turn, target creature gains indestructible. Untap that creature.\n" +
        "0: Until end of turn, Gideon becomes a 4/4 Human Soldier creature with indestructible that's still a planeswalker. " +
        "Prevent all damage that would be dealt to him this turn."

    loyaltyAbility(+2) {
        val creature = target(TargetFilter.CreatureOpponentControls, optional = true)
        effect = Effects.MarkMustAttackDefender(
            creature,
            defender = EffectTarget.Self,
            window = AttackRequirementWindow.CONTROLLERS_NEXT_TURN
        )
    }

    loyaltyAbility(+1) {
        val creature = target(TargetFilter.Creature)
        effect = Effects.GrantKeyword(Keyword.INDESTRUCTIBLE, creature, Duration.UntilYourNextTurn) then
            Effects.Untap(creature)
    }

    loyaltyAbility(0) {
        effect = Effects.BecomeCreature(
            target = EffectTarget.Self,
            power = 4,
            toughness = 4,
            keywords = setOf(Keyword.INDESTRUCTIBLE),
            creatureTypes = setOf("Human", "Soldier"),
            duration = Duration.EndOfTurn
        ) then Effects.PreventDamage(target = EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "23"
        artist = "Willian Murai"
        imageUri = "https://cards.scryfall.io/normal/back/5/8/58c39df6-b237-40d1-bdcb-2fe5d05392a9.jpg?1783938364"
        ruling("2015-06-22", "Gideon's first ability causes a creature to attack him if able. If, during its controller's declare attackers step, that creature is tapped, is affected by a spell or ability that says it can't attack, or hasn't been under its controller's control continuously since that player's turn began, then that creature doesn't attack. If there's a cost associated with having that creature attack, its controller isn't forced to pay that cost. If they don't, the creature doesn't have to attack.")
        ruling("2015-06-22", "If the creature targeted by Gideon's first ability changes controllers before it has the chance to attack Gideon, the ability will apply to it during its new controller's next turn.")
        ruling("2015-06-22", "If Gideon can't be attacked, perhaps because he has left the battlefield before the creature's controller's next combat, the creature targeted by Gideon's first ability can attack you or another planeswalker you control, or its controller can choose to have it not attack at all.")
        ruling("2015-06-22", "Gideon's third ability causes him to become a creature with the creature types Human Soldier. He remains a planeswalker with the planeswalker type Gideon. (He also retains any other card types or subtypes he may have had.) Each subtype is correlated to the proper card type: Gideon is just a planeswalker type (not a creature type), and Human and Soldier are just creature types (not planeswalker types).")
        ruling("2015-06-22", "If you activate Gideon's third ability and then damage is dealt to him that can't be prevented, that damage has all applicable results: specifically, the damage is marked on Gideon (since he's a creature) and that damage causes that many loyalty counters to be removed from him (since he's a planeswalker). If Gideon has no loyalty counters on him, he's put into his owner's graveyard as a state-based action. (As long as he still has indestructible, the marked damage won't cause him to be destroyed.)")
    }
}

val KytheonHeroOfAkros: CardDefinition = CardDefinition.doubleFacedPermanent(
    frontFace = KytheonHeroOfAkrosFront,
    backFace = GideonBattleForged,
)
