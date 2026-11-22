package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.engine.view.ClientStateTransformer
import com.wingedsheep.engine.view.projection.StackTextRenderer
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.conditions.Compare
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.conditions.Exists
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * The text a spell shows on the stack collapses an `Effects.If` to the branch it will take only
 * when the cast settled the condition. Any other condition renders in full: one reading a hidden
 * zone would otherwise tell every viewer what that zone holds, and one reading the board would
 * state a guess about resolution as fact.
 */
class StackTextConditionalCollapseTest : FunSpec({

    // Settled by the cast: X is locked in once the spell is on the stack.
    val surge = card("Settled Surge") {
        manaCost = "{X}{B}"
        typeLine = "Sorcery"
        spell {
            effect = Effects.If(
                condition = Compare(DynamicAmount.XValue, ComparisonOperator.GTE, DynamicAmount.Fixed(3)),
                then = Effects.DrawCards(2),
                otherwise = Effects.DrawCards(1)
            )
        }
    }

    // Reads the caster's library — hidden from everyone, the caster included.
    val augury = card("Library Augury") {
        manaCost = "{B}"
        typeLine = "Sorcery"
        spell {
            effect = Effects.If(
                condition = Exists(Player.You, Zone.LIBRARY, GameObjectFilter.Creature),
                then = Effects.DrawCards(2),
                otherwise = Effects.GainLife(1)
            )
        }
    }

    // Reads the live board, which can change before the spell resolves — View from Above's shape.
    val glance = card("Board Glance") {
        manaCost = "{B}"
        typeLine = "Instant"
        spell {
            val creature = target(TargetFilter.Creature)
            effect = Effects.GrantKeyword(Keyword.FLYING, creature) then Effects.If(
                condition = Exists(Player.You, Zone.BATTLEFIELD, GameObjectFilter.Permanent.withColor(Color.WHITE)),
                then = Effects.ReturnToHand(EffectTarget.Self)
            )
        }
    }

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(surge, augury, glance))
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun stackTextFor(driver: GameTestDriver, viewer: EntityId): String? {
        val spellId = driver.state.stack.single()
        return ClientStateTransformer(driver.cardRegistry, predicateEvaluator = PredicateEvaluator(cardRegistry = null))
            .transform(driver.state, viewer).cards[spellId]!!.stackText
    }

    fun castAndRead(
        setup: (GameTestDriver, EntityId) -> Unit,
        cardName: String,
        xValue: Int? = null,
        targetCreature: Boolean = false
    ): Pair<String?, String?> {
        val driver = newDriver()
        val caster = driver.activePlayer!!
        val opponent = driver.getOpponent(caster)
        setup(driver, caster)
        val targets = if (targetCreature) listOf(driver.putCreatureOnBattlefield(caster, "Centaur Courser")) else emptyList()
        val spell = driver.putCardInHand(caster, cardName)
        driver.giveMana(caster, Color.BLACK, 1 + (xValue ?: 0))
        val result = if (xValue != null) driver.castXSpell(caster, spell, xValue) else driver.castSpell(caster, spell, targets)
        result.outcome shouldBe Outcome.Done
        return stackTextFor(driver, caster) to stackTextFor(driver, opponent)
    }

    test("a condition the cast settled collapses to the branch it will take") {
        castAndRead({ _, _ -> }, "Settled Surge", xValue = 3) shouldBe ("Draw 2 cards" to "Draw 2 cards")
        castAndRead({ _, _ -> }, "Settled Surge", xValue = 1) shouldBe ("Draw a card" to "Draw a card")
    }

    test("a condition over the caster's library renders the same text whatever the library holds") {
        val (casterWithout, opponentWithout) = castAndRead({ _, _ -> }, "Library Augury")
        val (casterWith, opponentWith) = castAndRead(
            { driver, caster -> driver.putCardOnTopOfLibrary(caster, "Centaur Courser") },
            "Library Augury"
        )
        opponentWith shouldBe opponentWithout
        casterWith shouldBe casterWithout
        opponentWith shouldBe "If a creature is in your library, draw 2 cards. Otherwise, you gain 1 life"
    }

    test("a condition over the live board renders in full, naming the spell where it refers to itself") {
        val (_, withoutWhite) = castAndRead({ _, _ -> }, "Board Glance", targetCreature = true)
        val (_, withWhite) = castAndRead(
            { driver, caster -> driver.putCreatureOnBattlefield(caster, "Savannah Lions") },
            "Board Glance",
            targetCreature = true
        )
        withWhite shouldBe withoutWhite
        withoutWhite shouldBe "Target creature gains flying until end of turn. " +
            "If you control a white permanent, return Board Glance to its owner's hand"
    }

    test("a spell's self-reference is named, but not inside an ability it grants") {
        StackTextRenderer.nameSelf("Return this creature to its owner's hand", "Repel") shouldBe
            "Return Repel to its owner's hand"
        StackTextRenderer.nameSelf(
            "Target creature gains 'When this creature dies, return it to its owner's hand.' Exile this permanent",
            "Last Rites"
        ) shouldBe "Target creature gains 'When this creature dies, return it to its owner's hand.' Exile Last Rites"
        StackTextRenderer.nameSelf("Create a token with \"This creature can't block\"", "Rally") shouldBe
            "Create a token with \"This creature can't block\""
    }
})
