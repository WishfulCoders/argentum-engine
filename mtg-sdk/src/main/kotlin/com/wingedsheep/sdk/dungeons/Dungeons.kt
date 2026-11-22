package com.wingedsheep.sdk.dungeons

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.dungeon
import com.wingedsheep.sdk.model.DungeonDefinition
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.EffectChoice
import com.wingedsheep.sdk.scripting.effects.FeasibilityCheck
import com.wingedsheep.sdk.scripting.effects.MayPlayExpiry
import com.wingedsheep.sdk.scripting.effects.SearchDestination
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * The dungeon cards (CR 309) a player can venture into: the three of *Adventures in the Forgotten
 * Realms* and *Commander Legends: Battle for Baldur's Gate*'s Undercity, which only the initiative
 * enters (CR 701.49d).
 *
 * Dungeon cards begin outside the game and every player has access to all of them, so they are a
 * fixed catalog rather than part of anyone's deck. The engine reads this list when a player ventures
 * with no dungeon in their command zone; each room's [com.wingedsheep.sdk.model.DungeonRoom.effect] is
 * composed from the same facades cards use.
 */
object Dungeons {

    /** Lost Mine of Phandelver — AFR dungeon (tafr #21). Four rooms deep. */
    val LostMineOfPhandelver: DungeonDefinition = dungeon("Lost Mine of Phandelver") {
        imageUri = "https://cards.scryfall.io/normal/front/5/9/59b11ff8-f118-4978-87dd-509dc0c8c932.jpg?1783926340"
        room("Cave Entrance", "Scry 1.", leadsTo = listOf("Goblin Lair", "Mine Tunnels")) {
            effect = Effects.Scry(1)
        }
        room("Goblin Lair", "Create a 1/1 red Goblin creature token.", leadsTo = listOf("Storeroom", "Dark Pool")) {
            effect = Effects.CreateToken(
                power = 1, toughness = 1, colors = setOf(Color.RED), creatureTypes = setOf("Goblin"),
                imageUri = "https://cards.scryfall.io/normal/front/1/4/1425e965-7eea-419c-a7ec-c8169fa9edbf.jpg?1783926342"
            )
        }
        room("Mine Tunnels", "Create a Treasure token.", leadsTo = listOf("Dark Pool", "Fungi Cavern")) {
            effect = Effects.CreateTreasure()
        }
        room("Storeroom", "Put a +1/+1 counter on target creature.", leadsTo = listOf("Temple of Dumathoin")) {
            val creature = target(TargetFilter.Creature)
            effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, creature)
        }
        room("Dark Pool", "Each opponent loses 1 life and you gain 1 life.", leadsTo = listOf("Temple of Dumathoin")) {
            effect = Effects.LoseLife(1, EffectTarget.PlayerRef(Player.EachOpponent)) then Effects.GainLife(1)
        }
        room("Fungi Cavern", "Target creature gets -4/-0 until your next turn.", leadsTo = listOf("Temple of Dumathoin")) {
            val creature = target(TargetFilter.Creature)
            effect = Effects.ModifyStats(-4, 0, creature, Duration.UntilYourNextTurn)
        }
        room("Temple of Dumathoin", "Draw a card.") {
            effect = Effects.DrawCards(1)
        }
    }

    /** Dungeon of the Mad Mage — AFR dungeon (tafr #20). The longest: seven rooms deep. */
    val DungeonOfTheMadMage: DungeonDefinition = dungeon("Dungeon of the Mad Mage") {
        imageUri = "https://cards.scryfall.io/normal/front/6/f/6f509dbe-6ec7-4438-ab36-e20be46c9922.jpg?1789711206"
        room("Yawning Portal", "You gain 1 life.", leadsTo = listOf("Dungeon Level")) {
            effect = Effects.GainLife(1)
        }
        room("Dungeon Level", "Scry 1.", leadsTo = listOf("Goblin Bazaar", "Twisted Caverns")) {
            effect = Effects.Scry(1)
        }
        room("Goblin Bazaar", "Create a Treasure token.", leadsTo = listOf("Lost Level")) {
            effect = Effects.CreateTreasure()
        }
        room("Twisted Caverns", "Target creature can't attack until your next turn.", leadsTo = listOf("Lost Level")) {
            val creature = target(TargetFilter.Creature)
            effect = Effects.CantAttack(creature, Duration.UntilYourNextTurn)
        }
        room("Lost Level", "Scry 2.", leadsTo = listOf("Runestone Caverns", "Muiral's Graveyard")) {
            effect = Effects.Scry(2)
        }
        // No duration is printed, so the permission lasts as long as the cards stay exiled
        // (CR 611.2a — an effect with no stated duration lasts until the end of the game).
        room("Runestone Caverns", "Exile the top two cards of your library. You may play them.", leadsTo = listOf("Deep Mines")) {
            effect = Patterns.Exile.impulse(2, MayPlayExpiry.Permanent, storeAs = "runestoneCaverns")
        }
        room("Muiral's Graveyard", "Create two 1/1 black Skeleton creature tokens.", leadsTo = listOf("Deep Mines")) {
            effect = Effects.CreateToken(
                power = 1, toughness = 1, colors = setOf(Color.BLACK), creatureTypes = setOf("Skeleton"), count = 2,
                imageUri = "https://cards.scryfall.io/normal/front/f/a/fa6fdb57-82f3-4695-b1fa-1f301ea4ef83.jpg?1783926346"
            )
        }
        room("Deep Mines", "Scry 3.", leadsTo = listOf("Mad Wizard's Lair")) {
            effect = Effects.Scry(3)
        }
        room("Mad Wizard's Lair", "Draw three cards and reveal them. You may cast one of them without paying its mana cost.") {
            // Peek at the three cards about to be drawn so the free cast can be offered among
            // exactly those, then keep only the ones that really reached the hand (a replaced or
            // short draw leaves fewer).
            effect = Effects.Pipeline {
                val toDraw = gather(CardSource.TopOfLibrary(3))
                run(Effects.DrawCards(3))
                val drawn = filter(toDraw, GameObjectFilter.Any.currentlyIn(Zone.HAND))
                reveal(drawn)
                run(Effects.CastUpToNFromCollectionWithoutPayingCost(drawn, 1))
            }
        }
    }

    /** Tomb of Annihilation — AFR dungeon (tafr #22). The shortest and harshest: three rooms deep. */
    val TombOfAnnihilation: DungeonDefinition = dungeon("Tomb of Annihilation") {
        imageUri = "https://cards.scryfall.io/normal/front/7/0/70b284bd-7a8f-4b60-8238-f746bdc5b236.jpg?1783926339"
        room("Trapped Entry", "Each player loses 1 life.", leadsTo = listOf("Veils of Fear", "Oubliette")) {
            effect = Effects.LoseLife(1, EffectTarget.PlayerRef(Player.Each))
        }
        room("Veils of Fear", "Each player loses 2 life unless they discard a card.", leadsTo = listOf("Sandfall Cell")) {
            effect = Effects.ForEachPlayer(
                Player.Each,
                Effects.ChooseAction(
                    player = EffectTarget.Controller,
                    choices = listOf(
                        EffectChoice(
                            label = "Discard a card",
                            effect = Patterns.Hand.discardCards(1, EffectTarget.Controller),
                            feasibilityCheck = FeasibilityCheck.HasCardsInZone(Zone.HAND),
                        ),
                        EffectChoice(label = "Lose 2 life", effect = Effects.LoseLife(2, EffectTarget.Controller)),
                    ),
                )
            )
        }
        room(
            "Sandfall Cell",
            "Each player loses 2 life unless they sacrifice a creature, artifact, or land of their choice.",
            leadsTo = listOf("Cradle of the Death God")
        ) {
            val sacrificeable = GameObjectFilter.Creature or GameObjectFilter.Artifact or GameObjectFilter.Land
            effect = Effects.ForEachPlayer(
                Player.Each,
                Effects.ChooseAction(
                    player = EffectTarget.Controller,
                    choices = listOf(
                        EffectChoice(
                            label = "Sacrifice a creature, artifact, or land",
                            effect = Effects.Sacrifice(sacrificeable, 1, EffectTarget.Controller),
                            feasibilityCheck = FeasibilityCheck.ControlsPermanentMatching(sacrificeable),
                        ),
                        EffectChoice(label = "Lose 2 life", effect = Effects.LoseLife(2, EffectTarget.Controller)),
                    ),
                )
            )
        }
        room("Oubliette", "Discard a card and sacrifice a creature, an artifact, and a land.", leadsTo = listOf("Cradle of the Death God")) {
            effect = Patterns.Hand.discardCards(1, EffectTarget.Controller) then
                Effects.SacrificeOwn(GameObjectFilter.Creature) then
                Effects.SacrificeOwn(GameObjectFilter.Artifact) then
                Effects.SacrificeOwn(GameObjectFilter.Land)
        }
        room(
            "Cradle of the Death God",
            "Create The Atropal, a legendary 4/4 black God Horror creature token with deathtouch."
        ) {
            effect = Effects.CreateToken(
                power = 4, toughness = 4, colors = setOf(Color.BLACK), creatureTypes = setOf("God", "Horror"),
                keywords = setOf(Keyword.DEATHTOUCH), name = "The Atropal", legendary = true,
                imageUri = "https://cards.scryfall.io/normal/front/6/5/65f8e40f-fb5e-4ab8-add3-a8b87e7bcdd9.jpg?1783926347"
            )
        }
    }

    /**
     * Undercity — CLB dungeon (tclb #20), the front face of The Initiative card. Only "venture into
     * Undercity" enters it, and only the initiative ventures into Undercity.
     */
    val Undercity: DungeonDefinition = dungeon("Undercity") {
        imageUri = "https://cards.scryfall.io/normal/front/2/c/2c65185b-6cf0-451d-985e-56aa45d9a57d.jpg?1788840929"
        enteredOnlyByName = true
        room(
            "Secret Entrance",
            "Search your library for a basic land card, reveal it, put it into your hand, then shuffle.",
            leadsTo = listOf("Forge", "Lost Well")
        ) {
            effect = Patterns.Library.searchLibrary(
                filter = GameObjectFilter.BasicLand, destination = SearchDestination.HAND, reveal = true
            )
        }
        room("Forge", "Put two +1/+1 counters on target creature.", leadsTo = listOf("Trap!", "Arena")) {
            val creature = target(TargetFilter.Creature)
            effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 2, creature)
        }
        room("Lost Well", "Scry 2.", leadsTo = listOf("Arena", "Stash")) {
            effect = Effects.Scry(2)
        }
        room("Trap!", "Target player loses 5 life.", leadsTo = listOf("Archives")) {
            val player = target(Targets.Player)
            effect = Effects.LoseLife(5, player)
        }
        room("Arena", "Goad target creature.", leadsTo = listOf("Archives", "Catacombs")) {
            val creature = target(TargetFilter.Creature)
            effect = Effects.Goad(creature)
        }
        room("Stash", "Create a Treasure token.", leadsTo = listOf("Catacombs")) {
            effect = Effects.CreateTreasure()
        }
        room("Archives", "Draw a card.", leadsTo = listOf("Throne of the Dead Three")) {
            effect = Effects.DrawCards(1)
        }
        room("Catacombs", "Create a 4/1 black Skeleton creature token with menace.", leadsTo = listOf("Throne of the Dead Three")) {
            effect = Effects.CreateToken(
                power = 4, toughness = 1, colors = setOf(Color.BLACK), creatureTypes = setOf("Skeleton"),
                keywords = setOf(Keyword.MENACE),
                imageUri = "https://cards.scryfall.io/normal/front/c/f/cf4c245f-af2f-46a7-81f3-670a04940901.jpg?1783922321"
            )
        }
        room(
            "Throne of the Dead Three",
            "Reveal the top ten cards of your library. Put a creature card from among them onto the " +
                "battlefield with three +1/+1 counters on it. It gains hexproof until your next turn. Then shuffle."
        ) {
            // The counters are placed as the creature lands rather than as a replacement on its
            // entry, so an "enters with counters" observer doesn't see them; nothing in the corpus
            // reads that today.
            effect = Effects.Pipeline {
                val revealed = gather(CardSource.TopOfLibrary(10), revealed = true)
                val chosen = chooseExactly(
                    1, from = revealed, filter = GameObjectFilter.Creature,
                    prompt = "Put a creature card onto the battlefield with three +1/+1 counters on it",
                    showAllCards = true
                )
                val entered = moveTracked(chosen, CardDestination.ToZone(Zone.BATTLEFIELD))
                run(Effects.AddCountersToCollection(entered, CounterType.PLUS_ONE_PLUS_ONE, 3))
                run(
                    Effects.ForEachInCollection(
                        entered,
                        Effects.GrantKeyword(Keyword.HEXPROOF, EffectTarget.IterationEntity, Duration.UntilYourNextTurn)
                    )
                )
                run(Effects.ShuffleLibrary())
            }
        }
    }

    /** Every dungeon, in the order a player is offered them. */
    val ALL: List<DungeonDefinition> = listOf(LostMineOfPhandelver, DungeonOfTheMadMage, TombOfAnnihilation, Undercity)

    private val byName: Map<String, DungeonDefinition> = ALL.associateBy { it.name }

    /** The dungeon named [name], or null. */
    fun byName(name: String): DungeonDefinition? = byName[name]

    /**
     * The dungeons a player may choose when venturing into a new one (CR 701.49a/d): the one named
     * by a "venture into [quality]", or every dungeon that isn't entered only by name.
     */
    fun choosable(into: String?): List<DungeonDefinition> =
        if (into != null) listOfNotNull(byName[into]) else ALL.filterNot { it.enteredOnlyByName }
}
