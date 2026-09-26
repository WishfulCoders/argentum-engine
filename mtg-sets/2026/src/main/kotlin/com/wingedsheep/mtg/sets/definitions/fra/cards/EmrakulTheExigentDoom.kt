package com.wingedsheep.mtg.sets.definitions.fra.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.scripting.AbilityId
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivatedAbility
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.MayPlayExpiry
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.effects.WardCost

/**
 * Emrakul, the Exigent Doom (Reality Fracture #1) — {10} Legendary Creature — Eldrazi 12/12.
 *
 * - The cast trigger untaps every land you control, so the lands that paid for Emrakul are
 *   available again as soon as it is on the stack.
 * - Ward—Sacrifice three permanents is the generic sacrifice ward over any permanent.
 * - The hand ability pays `{3}` and exiles the card itself as its cost ([Costs.ExileSelf] from
 *   [Zone.HAND]). On resolution the target land gains "{T}: Add {C}{C}" for
 *   [Duration.UntilSourceCastFromExile]: the grant remembers the exact exile object Emrakul became,
 *   and casting that object from exile removes it. Any other way out of exile makes Emrakul a new
 *   object (CR 400.7), so the land then keeps the ability indefinitely.
 * - "You may cast this card for as long as it remains exiled" gathers the card from its owner's
 *   exile (nothing, if it already left) and gives it a permanent cast-from-exile permission. The
 *   permission waives nothing — Emrakul still costs {10} — but the granted land and the cast
 *   trigger's untap are what make that affordable.
 */
val EmrakulTheExigentDoom = card("Emrakul, the Exigent Doom") {
    manaCost = "{10}"
    typeLine = "Legendary Creature — Eldrazi"
    power = 12
    toughness = 12
    oracleText = "When you cast this spell, untap all lands you control.\n" +
        "Flying, trample\n" +
        "Ward—Sacrifice three permanents.\n" +
        "{3}, Exile this card from your hand: Target land gains \"{T}: Add {C}{C}\" until this card " +
        "is cast from exile. You may cast this card for as long as it remains exiled."

    triggeredAbility {
        trigger = Triggers.self.isCast()
        effect = Patterns.Group.untapGroup(GroupFilter(GameObjectFilter.Land.youControl()))
    }

    keywords(Keyword.FLYING, Keyword.TRAMPLE)

    keywordAbility(KeywordAbility.Ward(WardCost.Sacrifice(GameObjectFilter.Permanent, count = 3)))

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{3}"), Costs.ExileSelf)
        activateFromZone = Zone.HAND
        val land = target(TargetFilter.Land)
        effect = Effects.GrantActivatedAbility(
            ability = ActivatedAbility(
                id = AbilityId.next(),
                cost = AbilityCost.Tap,
                effect = Effects.AddColorlessMana(2),
                isManaAbility = true,
                timing = TimingRule.ManaAbility
            ),
            target = land,
            duration = Duration.UntilSourceCastFromExile
        ) then
            Effects.Pipeline {
                val exiledEmrakul = gather(
                    CardSource.FromZone(
                        zone = Zone.EXILE,
                        player = Player.You,
                        filter = GameObjectFilter.Any.sourceItself()
                    )
                )
                run(Effects.GrantMayPlayFromExile(from = exiledEmrakul, expiry = MayPlayExpiry.Permanent))
            }
        description = "{3}, Exile this card from your hand: Target land gains \"{T}: Add {C}{C}\" until " +
            "this card is cast from exile. You may cast this card for as long as it remains exiled."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "1"
        artist = "Cristi Balanescu"
        imageUri = "https://cards.scryfall.io/normal/front/c/3/c3ff8dd3-88a8-49dc-a59b-e2748680623c.jpg?1789060137"
        inBooster = false
    }
}
