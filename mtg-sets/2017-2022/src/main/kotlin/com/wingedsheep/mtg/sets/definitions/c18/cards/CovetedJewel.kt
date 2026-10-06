package com.wingedsheep.mtg.sets.definitions.c18.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Coveted Jewel
 * {6}
 * Artifact
 * When this artifact enters, draw three cards.
 * {T}: Add three mana of any one color.
 * Whenever one or more creatures an opponent controls attack you and aren't blocked, that player
 * draws three cards and gains control of this artifact. Untap it.
 *
 * The last ability is `Triggers.you.isAttackedUnblocked()`: it triggers after blockers are
 * declared, once per attacking player with an unblocked creature attacking you (not a
 * planeswalker you control), and that attacking player is `Player.TriggeringPlayer`. The control
 * change has no duration — it lasts until something else changes it, including the next time
 * the Jewel's new controller is attacked; a later grant from this same ability replaces the
 * earlier one rather than stacking under it.
 */
val CovetedJewel = card("Coveted Jewel") {
    manaCost = "{6}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "When this artifact enters, draw three cards.\n" +
        "{T}: Add three mana of any one color.\n" +
        "Whenever one or more creatures an opponent controls attack you and aren't blocked, that " +
        "player draws three cards and gains control of this artifact. Untap it."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.DrawCards(3)
    }

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddAnyColorMana(3)
        manaAbility = true
        timing = TimingRule.ManaAbility
        description = "{T}: Add three mana of any one color."
    }

    triggeredAbility {
        trigger = Triggers.you.isAttackedUnblocked()
        effect = Effects.DrawCards(3, EffectTarget.PlayerRef(Player.TriggeringPlayer)) then
            Effects.GiveControl(EffectTarget.Self, EffectTarget.PlayerRef(Player.TriggeringPlayer)) then
            Effects.Untap(EffectTarget.Self)
        description = "Whenever one or more creatures an opponent controls attack you and aren't blocked, " +
            "that player draws three cards and gains control of this artifact. Untap it."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "54"
        artist = "Jason A. Engle"
        imageUri = "https://cards.scryfall.io/normal/front/f/8/f83ed433-fae3-4fa5-acad-bb8a5b535ce3.jpg?1783934324"
        ruling("2018-07-13", "Coveted Jewel's last ability resolves after blockers are chosen but before combat damage is dealt.")
        ruling(
            "2018-07-13",
            "Coveted Jewel's last ability triggers after you declare blockers (or declare no blockers at all) if " +
                "any attacking creatures are unblocked. It doesn't matter if some attacking creatures were blocked."
        )
        ruling("2018-07-13", "A creature attacking a planeswalker you control won't cause Coveted Jewel's last ability to trigger.")
        ruling(
            "2018-07-13",
            "If more than one opponent attacks you at the same time, Coveted Jewel's last ability triggers for each " +
                "of them. You choose which player ends up with Coveted Jewel, but each of them draws three cards and " +
                "has a chance to activate its mana ability."
        )
    }
}
