package com.wingedsheep.engine.core

import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.mechanics.mana.CostCalculator
import com.wingedsheep.engine.mechanics.mana.ManaColorSetResolver
import com.wingedsheep.engine.mechanics.sba.player.CommanderDamageLossCheck
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.CommanderComponent
import com.wingedsheep.engine.state.components.identity.CommanderRegistryComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.player.LossReason
import com.wingedsheep.engine.state.components.player.PlayerLostComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Format
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Supertype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.values.ManaColorSet
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Two commanders in one deck — the partner abilities (CR 702.124).
 *
 * Deck construction decides *whether* two cards may share command (the server's deck validator
 * asks `CommanderEligibility.pairingError`); this pins what the engine owes a pair once it has
 * one: both begin in the command zone (702.124b), each is taxed only for its own casts (702.124d,
 * 903.8), commander damage is tallied per commander and never pooled across the pair (702.124d,
 * 903.10a), and "your commander's color identity" is the pair's combined identity (702.124c) —
 * what Command Tower and Arcane Signet read through [ManaColorSet.CommanderIdentity].
 */
class PartnerCommandersTest : FunSpec({

    val whitePartner = CardDefinition.creature(
        name = "Test White Partner",
        manaCost = ManaCost.parse("{1}{W}"),
        subtypes = setOf(Subtype("Human")),
        power = 3,
        toughness = 3,
        supertypes = setOf(Supertype.LEGENDARY),
        oracleText = "Partner",
    ).copy(keywordAbilities = listOf(KeywordAbility.partner()))

    val bluePartner = CardDefinition.creature(
        name = "Test Blue Partner",
        manaCost = ManaCost.parse("{1}{U}"),
        subtypes = setOf(Subtype("Wizard")),
        power = 3,
        toughness = 3,
        supertypes = setOf(Supertype.LEGENDARY),
        oracleText = "Partner",
    ).copy(keywordAbilities = listOf(KeywordAbility.partner()))

    val greenCommander = CardDefinition.creature(
        name = "Test Green Commander",
        manaCost = ManaCost.parse("{1}{G}"),
        subtypes = setOf(Subtype("Elf")),
        power = 2,
        toughness = 2,
        supertypes = setOf(Supertype.LEGENDARY),
    )

    val bear = CardDefinition.creature(
        name = "Test Partner Bear",
        manaCost = ManaCost.parse("{1}{G}"),
        subtypes = setOf(Subtype("Bear")),
        power = 2,
        toughness = 2,
    )

    /** Seat 0 commands the white/blue partners; seat 1 a single green commander. */
    fun partneredGame(): Pair<GameTestDriver, List<EntityId>> {
        val driver = GameTestDriver()
        driver.registerCards(listOf(whitePartner, bluePartner, greenCommander, bear))
        val players = driver.initMultiplayer(
            decks = listOf(Deck(cards = List(98) { bear.name }), Deck(cards = List(99) { bear.name })),
            format = Format.Commander(),
            commanders = listOf(whitePartner.name, greenCommander.name),
            partners = listOf(bluePartner.name, null),
            skipMulligans = true,
            startingPlayer = 0,
        )
        return driver to players
    }

    fun GameState.commanderNamed(owner: EntityId, name: String): EntityId =
        getZone(ZoneKey(owner, Zone.COMMAND)).single { getEntity(it)?.get<CardComponent>()?.name == name }

    /** Move [commanderId] — the real commander entity — from the command zone onto the battlefield. */
    fun GameTestDriver.commanderOntoBattlefield(owner: EntityId, commanderId: EntityId) {
        replaceState(
            state
                .removeFromZone(ZoneKey(owner, Zone.COMMAND), commanderId)
                .addToZone(ZoneKey(owner, Zone.BATTLEFIELD), commanderId)
                .updateEntity(commanderId) { it.with(ControllerComponent(owner)) }
        )
        removeSummoningSickness(commanderId)
    }

    test("both partners begin in the command zone, each a commander of its own (CR 702.124b)") {
        val (driver, players) = partneredGame()
        val (partnered, single) = players

        val commandZone = driver.state.getZone(ZoneKey(partnered, Zone.COMMAND))
        commandZone.size shouldBe 2
        commandZone.map { driver.state.getEntity(it)!!.get<CardComponent>()!!.name }
            .shouldContainExactlyInAnyOrder(whitePartner.name, bluePartner.name)
        for (commanderId in commandZone) {
            val component = driver.state.getEntity(commanderId)!!.get<CommanderComponent>()
            component shouldNotBe null
            component!!.ownerId shouldBe partnered
            component.castsFromCommandZone shouldBe 0
        }
        driver.state.getEntity(partnered)!!.get<CommanderRegistryComponent>()!!
            .commanderIds.shouldContainExactlyInAnyOrder(commandZone)

        // Neither partner was shuffled into the library: 98 + 2 = the deck's 100 cards.
        driver.state.getZone(ZoneKey(partnered, Zone.LIBRARY)).size +
            driver.state.getZone(ZoneKey(partnered, Zone.HAND)).size shouldBe 98

        // The opponent's single-commander seat is unaffected.
        driver.state.getZone(ZoneKey(single, Zone.COMMAND)).size shouldBe 1
    }

    test("each partner is taxed only for its own casts from the command zone (CR 702.124d)") {
        val (driver, players) = partneredGame()
        val partnered = players[0]
        val white = driver.state.commanderNamed(partnered, whitePartner.name)
        val blue = driver.state.commanderNamed(partnered, bluePartner.name)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.giveMana(partnered, Color.WHITE, 1)
        driver.giveColorlessMana(partnered, 1)
        driver.castSpell(partnered, white).error.shouldBeNull()

        driver.state.getEntity(white)!!.get<CommanderComponent>()!!.castsFromCommandZone shouldBe 1
        driver.state.getEntity(blue)!!.get<CommanderComponent>()!!.castsFromCommandZone shouldBe 0

        driver.bothPass()
        driver.state.getZone(ZoneKey(partnered, Zone.BATTLEFIELD)) shouldContain white

        // Put the white partner back in the command zone and price both: only white owes {2}.
        val back = driver.state
            .removeFromZone(ZoneKey(partnered, Zone.BATTLEFIELD), white)
            .addToZone(ZoneKey(partnered, Zone.COMMAND), white)
        val calculator = CostCalculator(driver.cardRegistry, PredicateEvaluator(cardRegistry = null))
        calculator.calculateEffectiveCost(back, whitePartner, partnered, fromZone = Zone.COMMAND).cmc shouldBe 4
        calculator.calculateEffectiveCost(back, bluePartner, partnered, fromZone = Zone.COMMAND).cmc shouldBe 2
    }

    test("combat damage from each partner is tallied separately (CR 702.124d, 903.10a)") {
        val (driver, players) = partneredGame()
        val (partnered, defender) = players
        val white = driver.state.commanderNamed(partnered, whitePartner.name)
        val blue = driver.state.commanderNamed(partnered, bluePartner.name)
        driver.commanderOntoBattlefield(partnered, white)
        driver.commanderOntoBattlefield(partnered, blue)

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(partnered, listOf(white, blue), defender)
        driver.passPriorityUntil(Step.COMBAT_DAMAGE)

        driver.getLifeTotal(defender) shouldBe 34
        driver.state.commanderDamageOf(white, defender) shouldBe 3
        driver.state.commanderDamageOf(blue, defender) shouldBe 3
    }

    test("21 damage split across the two partners is not a commander-damage loss; 21 from one is") {
        val (driver, players) = partneredGame()
        val (partnered, defender) = players
        val white = driver.state.commanderNamed(partnered, whitePartner.name)
        val blue = driver.state.commanderNamed(partnered, bluePartner.name)
        val check = CommanderDamageLossCheck(predicateEvaluator = PredicateEvaluator(cardRegistry = null))

        // 20 + 20 = 40 from the pair, but neither commander alone has dealt 21.
        val split = driver.state
            .recordCommanderDamage(white, defender, 20)
            .recordCommanderDamage(blue, defender, 20)
        check.check(split).newState.getEntity(defender)!!.get<PlayerLostComponent>().shouldBeNull()

        val lethal = split.recordCommanderDamage(white, defender, 1)
        check.check(lethal).newState.getEntity(defender)!!.get<PlayerLostComponent>()!!
            .reason shouldBe LossReason.COMMANDER_DAMAGE
    }

    test("commander color identity is the pair's union — what Command Tower reads (CR 702.124c)") {
        val (driver, players) = partneredGame()
        val (partnered, single) = players
        fun identityOf(player: EntityId): Set<Color> = ManaColorSetResolver.resolve(
            ManaColorSet.CommanderIdentity,
            driver.state,
            driver.state.projectedState,
            sourceId = null,
            controllerId = player,
            cardRegistry = driver.cardRegistry,
            predicateEvaluator = PredicateEvaluator(cardRegistry = null),
        )

        identityOf(partnered) shouldBe setOf(Color.WHITE, Color.BLUE)
        identityOf(single) shouldBe setOf(Color.GREEN)
    }

    test("setup refuses more than two commanders (CR 702.124g) and the same card twice") {
        val driver = GameTestDriver()
        driver.registerCards(listOf(whitePartner, bluePartner, greenCommander, bear))
        val initializer = GameInitializer(driver.cardRegistry)
        fun configWith(names: List<String>) = GameConfig(
            format = Format.Commander(),
            players = listOf(
                PlayerConfig("P1", Deck(cards = List(97) { bear.name }), commanderCardNames = names),
                PlayerConfig("P2", Deck(cards = List(99) { bear.name }), commanderCardNames = listOf(greenCommander.name)),
            ),
            skipMulligans = true,
        )

        shouldThrow<IllegalArgumentException> {
            initializer.initializeGame(configWith(listOf(whitePartner.name, bluePartner.name, greenCommander.name)))
        }
        shouldThrow<IllegalArgumentException> {
            initializer.initializeGame(configWith(listOf(whitePartner.name, whitePartner.name)))
        }
    }
})
