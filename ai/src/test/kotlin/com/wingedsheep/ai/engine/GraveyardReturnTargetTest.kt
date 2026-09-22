package com.wingedsheep.ai.engine

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.TargetsResponse
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.MtgSetCatalog
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Regression from the 2026-09-22 play session (replay `d069e1d5`, action 278): Dundoolin Weaver's
 * "return target permanent card from your graveyard to your hand" came back with a Forest, over
 * Morcant's Eyes, Lluwen, Reaping Willow and seven other spells, from a mana-flooded player.
 *
 * Two things lined up. The pre-ranking that trims a large target pool to `targetCandidates` knew
 * only creatures on the battlefield and players, so every graveyard card scored 0 and the trim kept
 * the first eight in graveyard order — Morcant's Eyes was ninth and never simulated. Then a card
 * returned to hand scores the same whichever card it is, and the tie went to the first candidate:
 * the Forest at the bottom of the graveyard.
 *
 * The opposite position is the check that this is not "spells beat lands": a player stuck on two
 * lands with a four-drop in hand wants the land back.
 */
class GraveyardReturnTargetTest : FunSpec({

    fun createRegistry(): CardRegistry {
        val registry = CardRegistry()
        registry.register(TestCards.all)
        for (set in MtgSetCatalog.all) {
            registry.register(set.cards)
            registry.register(set.basicLands)
        }
        return registry
    }

    val profiles = listOf(
        AiProfile.CURRENT,
        profileFromTokens("raceclock+timing+determinize+fixing+grants+locked"),
    )

    fun weaverTarget(profile: AiProfile, landsInPlay: Int, extraHand: List<String>): String? {
        val registry = createRegistry()
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        for (set in MtgSetCatalog.all) driver.registerCards(set.cards + set.basicLands)
        // A hand of two-drops and no lands, so the lands in play and in hand are exactly what the case says.
        driver.initMirrorMatch(deck = Deck.of("Grizzly Bears" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val player = driver.player1

        // Graveyard in the order the game built it: the lands first, Morcant's Eyes ninth.
        for (card in listOf(
            "Forest", "Dawnhand Eulogist", "Reaping Willow", "Forest", "Moonglove Extractor",
            "Lluwen, Imperfect Naturalist", "Bloom Tender", "Lys Alana Informant", "Morcant's Eyes",
            "Creakwood Safewright",
        )) driver.putCardInGraveyard(player, card)

        repeat(landsInPlay) { driver.putPermanentOnBattlefield(player, "Forest") }
        for (card in extraHand) driver.putCardInHand(player, card)
        // Three creatures once the Weaver lands, so its trigger's intervening "if" holds.
        driver.putPermanentOnBattlefield(player, "Lys Alana Dignitary")
        driver.putPermanentOnBattlefield(player, "Scarblade Scout")
        val weaver = driver.putCardInHand(player, "Dundoolin Weaver")
        driver.giveMana(player, Color.GREEN, 2)
        driver.castSpell(player, weaver)
        driver.bothPass()

        val decision = driver.pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>()
        decision.playerId shouldBe player
        (decision.legalTargets.values.flatten().size > 8) shouldBe true // the pool is trimmed

        val ai = AIPlayer.create(registry, player, profile)
        val response = ai.respondToDecision(driver.state, decision).shouldBeInstanceOf<TargetsResponse>()
        val chosen = response.selectedTargets.values.flatten().singleOrNull().shouldNotBeNull()
        return driver.state.getEntity(chosen)?.get<CardComponent>()?.name
    }

    for (profile in profiles) {
        test("flooded: Dundoolin Weaver returns a spell, not a land (${profile.id})") {
            weaverTarget(profile, landsInPlay = 7, extraHand = listOf("Forest", "Swamp")) shouldNotBe "Forest"
        }

        test("stuck on two lands with a four-drop in hand: it returns the Forest (${profile.id})") {
            weaverTarget(profile, landsInPlay = 2, extraHand = listOf("Reaping Willow", "Vinebred Brawler")) shouldBe "Forest"
        }
    }
})
