package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * "Exile [this permanent], then return it to the battlefield transformed" acts on a permanent.
 * When the permanent has left the battlefield before the ability resolves, the card in its new zone
 * is a new object (CR 400.7) that the instruction can't find: it is not exiled from that zone and
 * nothing returns. The rest of the ability still happens (Jace, Vryn's Prodigy still draws and
 * discards).
 */
class ExileAndReturnTransformedSourceGoneTest : FunSpec({

    val adept: CardDefinition = CardDefinition.doubleFacedPermanent(
        frontFace = card("Test Flip Adept") {
            manaCost = "{U}"
            typeLine = "Creature — Human Wizard"
            power = 1
            toughness = 1
            activatedAbility {
                cost = Costs.Mana("{1}")
                effect = Effects.GainLife(1) then Effects.ExileAndReturnTransformed(EffectTarget.Self)
            }
        },
        backFace = card("Test Flip Master") {
            manaCost = ""
            colorIndicator = "U"
            typeLine = "Creature — Human Wizard"
            power = 4
            toughness = 4
        },
    )
    val bounce = card("Test Flip Bounce") {
        manaCost = "{1}"
        typeLine = "Instant"
        spell {
            val t = target(TargetFilter.Creature)
            effect = Effects.ReturnToHand(t)
        }
    }

    fun driver() = GameTestDriver().also {
        it.registerCards(TestCards.all + listOf(adept, bounce))
        it.initMirrorMatch(Deck.of("Island" to 40), skipMulligans = true, startingPlayer = 0)
        it.passPriorityUntil(Step.PRECOMBAT_MAIN)
    }
    val abilityId = adept.script.activatedAbilities.single().id

    test("control: on the battlefield it is exiled and returns transformed") {
        val d = driver()
        val permanent = d.putCreatureOnBattlefield(d.player1, "Test Flip Adept")
        d.giveColorlessMana(d.player1, 1)
        d.submit(ActivateAbility(d.player1, permanent, abilityId)).error shouldBe null
        d.bothPass()
        d.findPermanent(d.player1, "Test Flip Master") shouldNotBe null
    }

    test("bounced in response, it stays in its owner's hand and the rest of the ability resolves") {
        val d = driver()
        val permanent = d.putCreatureOnBattlefield(d.player1, "Test Flip Adept")
        val life = d.getLifeTotal(d.player1)
        d.giveColorlessMana(d.player1, 1)
        d.submit(ActivateAbility(d.player1, permanent, abilityId)).error shouldBe null
        val response = d.putCardInHand(d.player1, bounce.name)
        d.giveColorlessMana(d.player1, 1)
        d.castSpell(d.player1, response, listOf(permanent)).error shouldBe null
        d.bothPass()
        d.getHand(d.player1).contains(permanent) shouldBe true
        d.bothPass()

        withClue("CR 400.7: the card in hand is a new object — not exiled, not returned") {
            d.getHand(d.player1).contains(permanent) shouldBe true
            d.findPermanent(d.player1, "Test Flip Master") shouldBe null
            d.findPermanent(d.player1, "Test Flip Adept") shouldBe null
        }
        d.getLifeTotal(d.player1) shouldBe life + 1
    }
})
