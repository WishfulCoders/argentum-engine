package com.wingedsheep.mtg.sets.definitions.dsk.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.mode
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.effects.Mode
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Ghostly Dancers
 * {3}{W}{W}
 * Creature — Spirit
 * 2/5
 * Flying
 * When this creature enters, return an enchantment card from your graveyard to your hand or unlock
 * a locked door of a Room you control.
 * Eerie — Whenever an enchantment you control enters and whenever you fully unlock a Room, create a
 * 3/1 white Spirit creature token with flying.
 *
 * The ETB is a "choose one" modal (CR 700.2): mode 1 returns an enchantment card from your graveyard
 * to your hand (a resolution-time Gather → Select(1) → Move-to-hand, not a target); mode 2 unlocks a
 * locked door of a Room you control via the resolution-time [Effects.UnlockDoor] effect (CR 709.5f),
 * modeled as "up to one target Room you control with a locked door" so a player with no eligible
 * Room can still pick this mode. The Eerie ability is the two standard Eerie triggers (an enchantment
 * you control entering, and fully unlocking a Room) each creating the flying Spirit token.
 */
val GhostlyDancers = card("Ghostly Dancers") {
    manaCost = "{3}{W}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Spirit"
    power = 2
    toughness = 5
    oracleText = "Flying\n" +
        "When this creature enters, return an enchantment card from your graveyard to your hand or " +
        "unlock a locked door of a Room you control.\n" +
        "Eerie — Whenever an enchantment you control enters and whenever you fully unlock a Room, " +
        "create a 3/1 white Spirit creature token with flying."

    keywords(Keyword.FLYING, Keyword.EERIE)

    // ETB — choose one: return an enchantment from your graveyard, or unlock a Room door.
    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = ModalEffect.chooseOne(
            Mode.noTarget(
                Effects.Pipeline {
                    val ghostlyDancersReturnable = gather(
                        CardSource.FromZone(
                            Zone.GRAVEYARD,
                            Player.You,
                            GameObjectFilter.Enchantment,
                        )
                    )
                    val ghostlyDancersToReturn = chooseExactly(
                        1,
                        from = ghostlyDancersReturnable,
                        showAllCards = true,
                        prompt = "Return an enchantment card from your graveyard to your hand",
                        selectedLabel = "Return to hand"
                    )
                    toHand(ghostlyDancersToReturn)
                },
                "Return an enchantment card from your graveyard to your hand",
            ),
            mode("Unlock a locked door of a Room you control") {
                val targetedObject = target(
                    TargetFilter(GameObjectFilter.Any.withSubtype(Subtype.ROOM).youControl()).hasLockedDoor(),
                    optional = true,
                )
                effect = Effects.UnlockDoor(targetedObject)
            },
        )
        description = "When this creature enters, return an enchantment card from your graveyard to " +
            "your hand or unlock a locked door of a Room you control."
    }

    // Eerie — part 1: whenever an enchantment you control enters.
    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Enchantment.youControl()).enters()
        effect = Effects.CreateToken(
            power = 3,
            toughness = 1,
            colors = setOf(Color.WHITE),
            creatureTypes = setOf("Spirit"),
            keywords = setOf(Keyword.FLYING),
            imageUri = "https://cards.scryfall.io/normal/front/4/b/4b7697e3-b3fb-481c-a90f-1ae08d8a9880.jpg?1726236503",
        )
        description = "Eerie — Whenever an enchantment you control enters, create a 3/1 white Spirit creature token with flying."
    }

    // Eerie — part 2: whenever you fully unlock a Room.
    triggeredAbility {
        trigger = Triggers.you.fullyUnlocksARoom()
        effect = Effects.CreateToken(
            power = 3,
            toughness = 1,
            colors = setOf(Color.WHITE),
            creatureTypes = setOf("Spirit"),
            keywords = setOf(Keyword.FLYING),
            imageUri = "https://cards.scryfall.io/normal/front/4/b/4b7697e3-b3fb-481c-a90f-1ae08d8a9880.jpg?1726236503",
        )
        description = "Eerie — Whenever you fully unlock a Room, create a 3/1 white Spirit creature token with flying."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "13"
        artist = "Josh Newton"
        imageUri = "https://cards.scryfall.io/normal/front/a/b/ab38adb5-8f16-4a4a-8dbc-f6ec14ca6c9f.jpg?1726285905"
    }
}
