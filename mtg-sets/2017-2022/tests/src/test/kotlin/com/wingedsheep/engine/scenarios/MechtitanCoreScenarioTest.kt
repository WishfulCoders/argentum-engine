package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.atq.cards.Ornithopter
import com.wingedsheep.mtg.sets.definitions.lea.cards.Shatter
import com.wingedsheep.mtg.sets.definitions.mrd.cards.SteelWall
import com.wingedsheep.mtg.sets.definitions.neo.cards.MechtitanCore
import com.wingedsheep.mtg.sets.definitions.neo.cards.MobilizerMech
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Mechtitan Core: the cost exiles the Core and four other artifact creatures and/or Vehicles, the
 * Mechtitan token is a legendary all-colours 10/10, and when the token leaves the four come back
 * tapped while the Core stays in exile ("except this card").
 */
class MechtitanCoreScenarioTest : FunSpec({

    val abilityId = MechtitanCore.activatedAbilities.first().id

    fun driver(): GameTestDriver = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(MechtitanCore, MobilizerMech, Ornithopter, SteelWall, Shatter))
        initMirrorMatch(Deck.of("Mountain" to 40), skipMulligans = true, startingPlayer = 0)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun GameTestDriver.board(): Pair<EntityId, List<EntityId>> {
        repeat(7) { putPermanentOnBattlefield(player1, "Mountain") }
        val core = putPermanentOnBattlefield(player1, "Mechtitan Core")
        val others = listOf(
            putPermanentOnBattlefield(player1, "Mobilizer Mech"), // a Vehicle that isn't a creature
            putCreatureOnBattlefield(player1, "Ornithopter"),
            putCreatureOnBattlefield(player1, "Ornithopter"),
            putCreatureOnBattlefield(player1, "Steel Wall"),
        )
        return core to others
    }

    fun GameTestDriver.activate(core: EntityId, others: List<EntityId>) = submit(
        ActivateAbility(
            playerId = player1,
            sourceId = core,
            abilityId = abilityId,
            costPayment = AdditionalCostPayment(exiledCards = others),
        )
    )

    test("the cost exiles the Core and four others, and Mechtitan is a legendary all-colours 10/10") {
        val game = driver()
        val (core, others) = game.board()

        game.activate(core, others).error shouldBe null
        withClue("the cost is paid on activation") {
            game.getExileCardNames(game.player1) shouldContainExactlyInAnyOrder listOf(
                "Mechtitan Core", "Mobilizer Mech", "Ornithopter", "Ornithopter", "Steel Wall"
            )
        }
        game.bothPass()

        val titan = game.findPermanent(game.player1, "Mechtitan").shouldNotBeNull()
        val projected = game.state.projectedState
        projected.getPower(titan) shouldBe 10
        projected.getToughness(titan) shouldBe 10
        projected.isLegendary(titan) shouldBe true
        projected.isCreature(titan) shouldBe true
        projected.getColors(titan) shouldBe Color.entries.map { it.name }.toSet()
        projected.getSubtypes(titan) shouldBe setOf("Construct")
        listOf(Keyword.FLYING, Keyword.VIGILANCE, Keyword.TRAMPLE, Keyword.LIFELINK, Keyword.HASTE).forEach {
            withClue(it) { projected.hasKeyword(titan, it) shouldBe true }
        }
    }

    test("when Mechtitan leaves, the four other cards return tapped and the Core stays exiled") {
        val game = driver()
        val (core, others) = game.board()
        game.activate(core, others).error shouldBe null
        game.bothPass()
        val titan = game.findPermanent(game.player1, "Mechtitan").shouldNotBeNull()

        val shatter = game.putCardInHand(game.player1, "Shatter")
        game.castSpell(game.player1, shatter, listOf(titan)).error shouldBe null
        game.bothPass() // Shatter resolves; Mechtitan's leaves trigger goes on the stack
        game.bothPass() // the trigger resolves

        game.findPermanent(game.player1, "Mechtitan") shouldBe null
        game.getExileCardNames(game.player1) shouldBe listOf("Mechtitan Core")
        val returned = listOf("Mobilizer Mech", "Ornithopter", "Steel Wall").flatMap { name ->
            game.state.getBattlefield().filter { game.getCardName(it) == name }
        }
        returned.size shouldBe 4
        returned.forEach { withClue(game.getCardName(it)) { game.isTapped(it) shouldBe true } }
    }

    test("the cost can't be paid with only three other permanents or with the Core itself") {
        val game = driver()
        val (core, others) = game.board()

        game.activate(core, others.take(3)).error shouldNotBe null
        game.activate(core, others.take(3) + core).error shouldNotBe null
        game.findPermanent(game.player1, "Mechtitan Core") shouldBe core
    }
})
