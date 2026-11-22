package com.wingedsheep.gameserver.lobby

import com.wingedsheep.engine.limited.BoosterGenerator
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.gameserver.cube.CubeBoosterStyle
import com.wingedsheep.gameserver.cube.ResolvedCube
import com.wingedsheep.gameserver.persistence.restoreTournamentLobby
import com.wingedsheep.gameserver.persistence.toPersistent
import com.wingedsheep.gameserver.session.PlayerIdentity
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.model.Rarity
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe

class TournamentLobbyCubeTest : FunSpec({
    val forest = CardDefinition.basicLand("Forest", Subtype.FOREST)
    val generator = BoosterGenerator(
        mapOf(
            "TST" to BoosterGenerator.SetConfig(
                setCode = "TST",
                setName = "Test",
                cards = emptyList(),
                basicLands = listOf(forest),
            )
        )
    )
    val cards = (1..40).map { number ->
        CardDefinition.creature(
            name = "Cube Bear $number",
            manaCost = ManaCost.parse("{1}{G}"),
            subtypes = setOf(Subtype.BEAR),
            power = 2,
            toughness = 2,
        )
    }

    fun lobby(
        format: TournamentFormat,
        boosterCount: Int,
        packSize: Int,
        cubeCards: List<CardDefinition> = cards,
        boosterStyle: CubeBoosterStyle = CubeBoosterStyle.SHUFFLED,
    ): TournamentLobby {
        val lobby = TournamentLobby(
            setCodes = listOf("TST"),
            setNames = listOf("Test"),
            boosterGenerator = generator,
            format = format,
            boosterCount = boosterCount,
        )
        lobby.addPlayer(PlayerIdentity(playerId = EntityId("host"), playerName = "Host"))
        lobby.addPlayer(PlayerIdentity(playerId = EntityId("guest"), playerName = "Guest"))
        lobby.configureCube(
            ResolvedCube(
                name = "Friends Cube",
                cards = cubeCards,
                basicLandSetCode = "TST",
                packSize = packSize,
                boosterStyle = boosterStyle,
            )
        )
        return lobby
    }

    test("sealed deals every player complete pools without replacement and uses cube basic lands") {
        val lobby = lobby(TournamentFormat.SEALED, boosterCount = 3, packSize = 4)

        lobby.startDeckBuilding(EntityId("host")) shouldBe true

        val dealt = lobby.players.values.flatMap { it.cardPool }
        dealt.size shouldBe 24
        dealt.map { it.name }.toSet().size shouldBe 24
        lobby.basicLands.keys shouldContainExactlyInAnyOrder setOf("Forest")
    }

    test("booster draft deals one unique cube pack per player") {
        val lobby = lobby(TournamentFormat.DRAFT, boosterCount = 1, packSize = 4)

        lobby.startDraft(EntityId("host")) shouldBe true

        val dealt = lobby.players.values.flatMap { it.currentPack.orEmpty() }
        dealt.size shouldBe 8
        dealt.map { it.name }.toSet().size shouldBe 8
    }

    test("Winston and Grid use the shared cube dealer") {
        val winston = lobby(TournamentFormat.WINSTON_DRAFT, boosterCount = 4, packSize = 4)
        winston.startWinstonDraft(EntityId("host")) shouldBe true
        val winstonCards = winston.winstonMainDeck + winston.winstonPiles.flatMap { it }
        winstonCards.size shouldBe 16
        winstonCards.map { it.name }.toSet().size shouldBe 16

        val grid = lobby(TournamentFormat.GRID_DRAFT, boosterCount = 2, packSize = 10)
        grid.startGridDraft(EntityId("host")) shouldBe true
        val gridCards = grid.gridGroups.flatMap { group ->
            group.mainDeck + group.gridCards.filterNotNull()
        }
        gridCards.size shouldBe 20
        gridCards.map { it.name }.toSet().size shouldBe 20
    }

    test("capacity gate reports the full host-readable calculation") {
        val lobby = lobby(TournamentFormat.SEALED, boosterCount = 6, packSize = 4)

        lobby.cubeCapacityError() shouldBe
            "2 players × 6 packs × 4 = 48 cards needed, cube has 40"
        lobby.startDeckBuilding(EntityId("host")) shouldBe false
        lobby.state shouldBe LobbyState.WAITING_FOR_PLAYERS
    }

    test("cube broadcasts its summary without adding CUBE to available sets") {
        val lobby = lobby(TournamentFormat.DRAFT, boosterCount = 3, packSize = 5)

        val settings = lobby.buildLobbyUpdate(EntityId("host")).settings

        settings.cubeName shouldBe "Friends Cube"
        settings.cubeCardCount shouldBe 40
        settings.packSize shouldBe 5
        settings.setCodes shouldBe listOf("CUBE")
        settings.availableSets.any { it.code == "CUBE" } shouldBe false
    }

    test("persistence restores the cube and exact undealt tail") {
        val lobby = lobby(TournamentFormat.DRAFT, boosterCount = 2, packSize = 4)
        lobby.startDraft(EntityId("host")) shouldBe true
        val remainingNames = lobby.cubeDealerRemainingCards().map { it.name }
        val registry = CardRegistry().also { it.register(cards) }

        val (restored, _) = restoreTournamentLobby(lobby.toPersistent(), registry, generator)

        restored.cube?.name shouldBe "Friends Cube"
        restored.cube?.packSize shouldBe 4
        restored.cubeDealerRemainingCards().map { it.name } shouldBe remainingNames
    }

    /** A small set-shaped cube: 20 commons, 6 uncommons, 3 rares and a mythic. */
    val setShapedCube = cards.take(30).mapIndexed { index, card ->
        val rarity = when {
            index < 20 -> Rarity.COMMON
            index < 26 -> Rarity.UNCOMMON
            index < 29 -> Rarity.RARE
            else -> Rarity.MYTHIC
        }
        card.copy(metadata = card.metadata.copy(rarity = rarity))
    }

    test("a booster-style cube prints draft packs by rarity slot, never repeating a card in a pack") {
        val lobby = lobby(
            TournamentFormat.DRAFT, boosterCount = 3, packSize = 15,
            cubeCards = setShapedCube, boosterStyle = CubeBoosterStyle.DRAFT_BOOSTER,
        )

        lobby.startDraft(EntityId("host")) shouldBe true

        lobby.players.values.forEach { player ->
            val pack = player.currentPack.orEmpty()
            pack.size shouldBe 14
            pack.map { it.name }.toSet().size shouldBe 14
            val byRarity = pack.groupingBy { it.metadata.rarity }.eachCount()
            byRarity[Rarity.COMMON] shouldBe 10
            byRarity[Rarity.UNCOMMON] shouldBe 3
            (byRarity[Rarity.RARE] ?: 0) + (byRarity[Rarity.MYTHIC] ?: 0) shouldBe 1
        }
    }

    test("a booster-style cube has no capacity limit — packs are printed, not dealt") {
        val lobby = lobby(
            TournamentFormat.SEALED, boosterCount = 6, packSize = 15,
            cubeCards = setShapedCube, boosterStyle = CubeBoosterStyle.PLAY_BOOSTER,
        )

        lobby.cubeCapacityError() shouldBe null
        lobby.startDeckBuilding(EntityId("host")) shouldBe true
        lobby.players.values.forEach { it.cardPool.size shouldBe 6 * 13 }
    }

    test("a booster-style cube broadcasts its style and the layout's pack size") {
        val lobby = lobby(
            TournamentFormat.DRAFT, boosterCount = 3, packSize = 15,
            cubeCards = setShapedCube, boosterStyle = CubeBoosterStyle.PLAY_BOOSTER,
        )

        val settings = lobby.buildLobbyUpdate(EntityId("host")).settings

        settings.cubeBoosterStyle shouldBe "PLAY_BOOSTER"
        settings.packSize shouldBe 13
    }

    test("persistence restores a booster-style cube with its cube rarities") {
        val lobby = lobby(
            TournamentFormat.DRAFT, boosterCount = 3, packSize = 15,
            cubeCards = setShapedCube, boosterStyle = CubeBoosterStyle.DRAFT_BOOSTER,
        )
        // The registry knows the cards at their own (default common) rarity; the cube's rarities
        // must survive the round trip anyway.
        val registry = CardRegistry().also { it.register(cards) }

        val (restored, _) = restoreTournamentLobby(lobby.toPersistent(), registry, generator)

        restored.cube?.boosterStyle shouldBe CubeBoosterStyle.DRAFT_BOOSTER
        restored.cube?.rarityByName() shouldBe lobby.cube?.rarityByName()
        restored.cubeCapacityError() shouldBe null
    }
})
