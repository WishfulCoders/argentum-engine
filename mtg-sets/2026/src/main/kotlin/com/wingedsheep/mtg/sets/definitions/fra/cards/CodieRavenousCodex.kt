package com.wingedsheep.mtg.sets.definitions.fra.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.events.SpellCastPredicate
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Codie, Ravenous Codex — Reality Fracture #168
 * {3} · Legendary Artifact Creature — Book Construct · 1/4
 *
 * Whenever you cast a prepared spell, copy it. You may choose new targets for the copy.
 * {W}{U}{B}{R}{G}, {T}: Each creature you control becomes prepared.
 *
 * "A prepared spell" is a spell cast as a prepare spell — the copy a prepared permanent keeps in
 * exile (CR 722.3c), which is the only way a prepare spell is ever cast. That cast-time fact is
 * [SpellCastPredicate.CastAsPrepareSpell], the prepare sibling of Chancellor of Tales'
 * `CastAsAdventure`. The copy is mandatory ("copy it", no "may") and the new-targets choice comes
 * from [Effects.CopyTargetSpell]; being a copy, it isn't cast, so it doesn't retrigger Codie.
 *
 * The activated ability walks every creature you control with [Effects.BecomePrepared], which
 * does nothing to a creature without a prepare spell or to one that is already prepared
 * (CR 722.3a) — exactly the reminder text.
 */
val CodieRavenousCodex = card("Codie, Ravenous Codex") {
    manaCost = "{3}"
    colorIdentity = ""
    typeLine = "Legendary Artifact Creature — Book Construct"
    power = 1
    toughness = 4
    oracleText = "Whenever you cast a prepared spell, copy it. You may choose new targets for the copy.\n" +
        "{W}{U}{B}{R}{G}, {T}: Each creature you control becomes prepared. (Only creatures with " +
        "prepare spells can become prepared.)"

    triggeredAbility {
        trigger = Triggers.you.casts(requires = setOf(SpellCastPredicate.CastAsPrepareSpell))
        effect = Effects.CopyTargetSpell(EffectTarget.TriggeringEntity)
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{W}{U}{B}{R}{G}"), Costs.Tap)
        effect = Effects.ForEachInGroup(
            filter = GroupFilter(GameObjectFilter.Creature.youControl()),
            effect = Effects.BecomePrepared(EffectTarget.IterationEntity),
        )
        description = "{W}{U}{B}{R}{G}, {T}: Each creature you control becomes prepared."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "168"
        artist = "Steve Prescott"
        flavorText = "A font of knowledge for those able to read more than a page of hexed words."
        imageUri = "https://cards.scryfall.io/normal/front/c/3/c3192390-1518-49fc-8716-f2c7a0384f39.jpg?1789646811"
        inBooster = false
    }
}
