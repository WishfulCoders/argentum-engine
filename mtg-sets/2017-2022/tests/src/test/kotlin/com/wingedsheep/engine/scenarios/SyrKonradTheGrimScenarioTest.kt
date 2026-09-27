package com.wingedsheep.engine.scenarios

import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.eld.cards.SyrKonradTheGrim
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Syr Konrad, the Grim (ELD #107) and `ZoneChangeEvent.excludeFrom`, which its middle clause
 * introduced: "Whenever another creature dies, or a creature card is put into a graveyard from
 * anywhere other than the battlefield, or a creature card leaves your graveyard, Syr Konrad deals 1
 * damage to each opponent."
 *
 * One case per clause, the ruling that creatures dying alongside Syr Konrad still count, and the
 * boundaries: a creature dying is counted once (by the first clause, not also by the second), a
 * land card milled is not a creature card, and a creature card leaving an opponent's graveyard is
 * not leaving yours.
 */
class SyrKonradTheGrimScenarioTest : FunSpec({

    val wrath = card("Test Wrath") {
        manaCost = "{2}"
        typeLine = "Sorcery"
        spell { effect = Patterns.Group.destroyAll(GroupFilter.AllCreatures) }
    }

    val graveRobber = card("Test Grave Robber") {
        manaCost = "{1}"
        typeLine = "Instant"
        spell {
            val t = target(TargetFilter.CardInGraveyard)
            effect = Effects.Exile(t, fromZone = Zone.GRAVEYARD)
        }
    }

    fun newDriver(deck: Deck): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(SyrKonradTheGrim, wrath, graveRobber))
        driver.initMirrorMatch(deck = deck, skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.settle() {
        var guard = 0
        while (guard++ < 40) {
            when {
                isPaused -> autoResolveDecision()
                state.stack.isNotEmpty() -> bothPass()
                else -> break
            }
        }
    }

    fun GameTestDriver.cast(you: EntityId, name: String, targets: List<ChosenTarget> = emptyList()) {
        val spell = putCardInHand(you, name)
        giveColorlessMana(you, 2)
        castSpellWithTargets(you, spell, targets).error shouldBe null
        settle()
    }

    val islands = Deck.of("Island" to 40)

    test("another creature dying pings each opponent once") {
        val driver = newDriver(islands)
        val you = driver.player1
        val opponent = driver.getOpponent(you)
        driver.putCreatureOnBattlefield(you, "Syr Konrad, the Grim")
        val lions = driver.putCreatureOnBattlefield(opponent, "Savannah Lions")

        val bolt = driver.putCardInHand(you, "Lightning Bolt")
        driver.giveMana(you, Color.RED, 1)
        driver.castSpell(you, bolt, listOf(lions)).error shouldBe null
        driver.settle()

        withClue("the Lions died from the battlefield: the first clause, not also the second") {
            driver.getLifeTotal(opponent) shouldBe 19
        }
        driver.getLifeTotal(you) shouldBe 20
    }

    test("creatures that die at the same time as Syr Konrad still trigger it") {
        val driver = newDriver(islands)
        val you = driver.player1
        val opponent = driver.getOpponent(you)
        driver.putCreatureOnBattlefield(you, "Syr Konrad, the Grim")
        driver.putCreatureOnBattlefield(opponent, "Savannah Lions")
        driver.putCreatureOnBattlefield(opponent, "Centaur Courser")

        driver.cast(you, "Test Wrath")

        withClue("two other creatures died alongside it; its own death is not another creature") {
            driver.getLifeTotal(opponent) shouldBe 18
        }
    }

    test("creature cards milled into graveyards count; land cards do not") {
        val driver = newDriver(Deck.of("Savannah Lions" to 40))
        val you = driver.player1
        val opponent = driver.getOpponent(you)
        val konrad = driver.putCreatureOnBattlefield(you, "Syr Konrad, the Grim")
        val ability = SyrKonradTheGrim.script.activatedAbilities.first()
        driver.giveMana(you, Color.BLACK, 1)
        driver.giveColorlessMana(you, 1)
        driver.submit(ActivateAbility(you, konrad, ability.id)).error shouldBe null
        driver.settle()

        withClue("each player milled a creature card: two triggers from the second clause") {
            driver.getLifeTotal(opponent) shouldBe 18
        }

        val islandsDriver = newDriver(islands)
        val you2 = islandsDriver.player1
        val konrad2 = islandsDriver.putCreatureOnBattlefield(you2, "Syr Konrad, the Grim")
        islandsDriver.giveMana(you2, Color.BLACK, 1)
        islandsDriver.giveColorlessMana(you2, 1)
        islandsDriver.submit(ActivateAbility(you2, konrad2, ability.id)).error shouldBe null
        islandsDriver.settle()
        islandsDriver.getLifeTotal(islandsDriver.getOpponent(you2)) shouldBe 20
    }

    test("a creature card leaving your graveyard counts; leaving an opponent's does not") {
        val driver = newDriver(islands)
        val you = driver.player1
        val opponent = driver.getOpponent(you)
        driver.putCreatureOnBattlefield(you, "Syr Konrad, the Grim")
        val yours = driver.putCardInGraveyard(you, "Savannah Lions")
        val theirs = driver.putCardInGraveyard(opponent, "Savannah Lions")

        driver.cast(you, "Test Grave Robber", listOf(ChosenTarget.Card(theirs, opponent, Zone.GRAVEYARD)))
        withClue("the opponent's creature card left the opponent's graveyard") {
            driver.getLifeTotal(opponent) shouldBe 20
        }

        driver.cast(you, "Test Grave Robber", listOf(ChosenTarget.Card(yours, you, Zone.GRAVEYARD)))
        withClue("your creature card left your graveyard") {
            driver.getLifeTotal(opponent) shouldBe 19
        }
    }
})
