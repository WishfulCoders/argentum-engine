package com.wingedsheep.mtg.sets.definitions.msh.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.times
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantDynamicStats
import com.wingedsheep.sdk.scripting.effects.FeasibilityCheck
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Winter Soldier, Icy Assassin — Marvel Super Heroes #239 (rare)
 * {W}{B} · Legendary Creature — Human Assassin Villain · 2/2
 *
 * Vigilance, menace
 * Winter Soldier gets +2/+0 for each Equipment attached to him.
 * {3}{W}{B}: Return this card from your graveyard to the battlefield with a finality counter on
 * him. Then you may attach an Equipment you control to him.
 *
 * Two pieces, both assembled from existing primitives:
 *
 *  - **The buff** is the With Great Power shape — [GrantDynamicStats] over
 *    [GroupFilter.source] with `DynamicAmount.Multiply(equipmentAttachedToSelf(), 2)`. The
 *    attachment count is read off *projected* subtypes, so a permanent that becomes (or stops
 *    being) an Equipment is counted correctly, and the bonus recomputes continuously as Equipment
 *    is attached or falls off.
 *  - **The graveyard reanimation** is the Uchbenbak shape — a graveyard-zone activated ability
 *    (`activateFromZone = Zone.GRAVEYARD`) that moves this card GRAVEYARD → BATTLEFIELD and then
 *    drops a [CounterType.FINALITY] counter on it; the "exile it instead of dying" replacement is the
 *    engine-wide behavior of that counter. No sorcery-speed restriction is printed, so none is
 *    modelled.
 *
 * The "Then you may attach an Equipment you control to him" tail is a *resolution-time* choice, not
 * a cast-time target: [Effects.May] asks the yes/no (skipped outright via [FeasibilityCheck] when you
 * control no Equipment, so a player who can't attach anything is never asked), then
 * [SelectTargetEffect] picks the Equipment and [Effects.AttachTargetEquipmentToCreature] moves it
 * onto Winter Soldier ([EffectTarget.Self], which is the permanent that just entered). Attaching an
 * Equipment this way is not the equip ability, so it doesn't use the stack and ignores equip costs
 * and sorcery timing.
 */
val WinterSoldierIcyAssassin = card("Winter Soldier, Icy Assassin") {
    manaCost = "{W}{B}"
    colorIdentity = "WB"
    typeLine = "Legendary Creature — Human Assassin Villain"
    power = 2
    toughness = 2
    oracleText = "Vigilance, menace\n" +
        "Winter Soldier gets +2/+0 for each Equipment attached to him.\n" +
        "{3}{W}{B}: Return this card from your graveyard to the battlefield with a finality " +
        "counter on him. Then you may attach an Equipment you control to him. (If a creature with " +
        "a finality counter on it would die, exile it instead.)"

    keywords(Keyword.VIGILANCE, Keyword.MENACE)

    staticAbility {
        ability = GrantDynamicStats(
            filter = GroupFilter.source(),
            powerBonus = DynamicAmounts.equipmentAttachedToSelf() * 2,
            toughnessBonus = DynamicAmounts.fixed(0)
        )
    }

    activatedAbility {
        cost = Costs.Mana("{3}{W}{B}")
        activateFromZone = Zone.GRAVEYARD
        effect = Effects.Move(EffectTarget.Self, Zone.BATTLEFIELD, fromZone = Zone.GRAVEYARD) then
            Effects.AddCounters(counterType = CounterType.FINALITY, count = 1, target = EffectTarget.Self) then
            Effects.May(
                effect = Effects.Pipeline {
                    val winterSoldierEquipment = selectTarget(
                        TargetObject(
                            filter = TargetFilter(
                                GameObjectFilter.Artifact.withSubtype(Subtype.EQUIPMENT).youControl()
                            )
                        )
                    )
                    run(Effects.AttachTargetEquipmentToCreature(
                        equipmentTarget = winterSoldierEquipment.asTarget,
                        creatureTarget = EffectTarget.Self
                    ))
                },
                descriptionOverride = "You may attach an Equipment you control to Winter Soldier.",
                feasibility = FeasibilityCheck.ControlsPermanentMatching(
                    GameObjectFilter.Artifact.withSubtype(Subtype.EQUIPMENT)
                )
            )
        description = "{3}{W}{B}: Return this card from your graveyard to the battlefield with a " +
            "finality counter on him. Then you may attach an Equipment you control to him."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "239"
        artist = "Riccardo Federici"
        imageUri = "https://cards.scryfall.io/normal/front/e/b/ebf71ffc-6e3e-4ca0-a84a-3c1ebd2b64b1.jpg?1783902893"
    }
}
