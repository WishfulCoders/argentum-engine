package com.wingedsheep.mtg.sets.definitions.pip.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.events.AttackPredicate
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Sentinel Sarah Lyons
 * {3}{R}{W}
 * Legendary Creature — Human Knight
 * 4/4
 * Haste
 * As long as an artifact entered the battlefield under your control this turn, creatures you
 * control get +2/+2.
 * Battalion — Whenever Sentinel Sarah Lyons and at least two other creatures attack, Sentinel
 * Sarah Lyons deals damage equal to the number of artifacts you control to target player.
 *
 * The anthem is gated on [Conditions.ArtifactEnteredBattlefieldThisTurn] — the per-player entry
 * log, so it is a pure event tracker: the artifact may since have left, it may have entered before
 * Sarah did, and one or many entries give the same +2/+2 (all rulings). Battalion is the self
 * attack trigger with `AttackerCountAtLeast(3)` (Paladin Elizabeth Taggerdy); the artifact count is
 * read on resolution, and the damage source falls back to last-known information if Sarah left.
 */
val SentinelSarahLyons = card("Sentinel Sarah Lyons") {
    manaCost = "{3}{R}{W}"
    colorIdentity = "RW"
    typeLine = "Legendary Creature — Human Knight"
    power = 4
    toughness = 4
    oracleText = "Haste\n" +
        "As long as an artifact entered the battlefield under your control this turn, creatures you control get +2/+2.\n" +
        "Battalion — Whenever Sentinel Sarah Lyons and at least two other creatures attack, Sentinel Sarah Lyons " +
        "deals damage equal to the number of artifacts you control to target player."

    keywords(Keyword.HASTE)

    staticAbility {
        condition = Conditions.ArtifactEnteredBattlefieldThisTurn
        ability = ModifyStats(2, 2, GroupFilter.AllCreaturesYouControl)
    }

    triggeredAbility {
        trigger = Triggers.self.attacks(setOf(AttackPredicate.AttackerCountAtLeast(3)))
        val player = target(Targets.Player)
        effect = Effects.DealDamage(
            DynamicAmounts.battlefield(Player.You, GameObjectFilter.Artifact).count(),
            player
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "118"
        artist = "David Astruga"
        imageUri = "https://cards.scryfall.io/normal/front/2/3/23b76965-528c-48f9-9ee6-332b09cae664.jpg?1783912384"
        ruling("2024-03-08", "Sentinel Sarah Lyons's second ability only cares if one or more artifacts entered the battlefield under your control this turn. Your creatures won't get more powerful if more than one artifact entered the battlefield under your control this turn.")
        ruling("2024-03-08", "Once an artifact enters the battlefield under your control in a given turn, it doesn't matter what happens to it later. Creatures you control will still get +2/+2 that turn as long as Sentinel Sarah Lyons is on the battlefield under your control.")
        ruling("2024-03-08", "Sentinel Sarah Lyons doesn't have to have been on the battlefield at the point in the turn where an artifact entered the battlefield under your control. If an artifact enters the battlefield under your control and then Sentinel Sarah Lyons enters the battlefield under your control later in that turn, creatures you control will still get +2/+2 that turn as long as Sentinel Sarah Lyons remains on the battlefield and under your control.")
        ruling("2024-03-08", "Once Sentinel Sarah Lyons's battalion ability has triggered, it doesn't matter how many creatures are still attacking when that ability resolves. It also doesn't matter whether or not Sentinel Sarah Lyons is still attacking or on the battlefield.")
        ruling("2024-03-08", "Use the number of artifacts you control at the time Sentinel Sarah Lyons's battalion ability resolves to determine how much damage to deal.")
    }
}
