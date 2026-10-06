package com.wingedsheep.engine.mechanics.targeting

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.state.components.identity.ProtectionComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.ProtectionScope
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * "Protection from spells that are one or more colors" (Emrakul, the Aeons Torn) —
 * [ProtectionScope.ColoredSpells], read through [SourceKindProtection] as
 * [SourceKind.COLORED_SPELL].
 *
 * Pins each leg of CR 702.16 for the quality (a spell with one or more colors, CR 105.2): it can't
 * be the target of such a spell (702.16b), damage such a spell would deal it is prevented
 * (702.16e), and the narrowing holds in both directions — a colorless spell, an ability from a
 * colored source, and a colored *permanent* all still reach it.
 */
class ColoredSpellProtectionTest : FunSpec({

    // The protection on a 3/3 body, so damage is observable without the creature dying.
    val warded = card("Test Aeons Ward") {
        manaCost = "{3}"
        typeLine = "Creature — Eldrazi"
        power = 3
        toughness = 3
        keywordAbility(KeywordAbility.Protection(ProtectionScope.ColoredSpells))
    }
    val colorlessBolt = card("Test Colorless Bolt") {
        manaCost = "{1}"
        typeLine = "Instant"
        spell {
            val t = target(Targets.Any)
            effect = Effects.DealDamage(2, t)
        }
    }
    fun sweeper(name: String, cost: String) = card(name) {
        manaCost = cost
        typeLine = "Sorcery"
        spell {
            effect = Effects.ForEachInGroup(
                GroupFilter(GameObjectFilter.Creature),
                Effects.DealDamage(2, EffectTarget.IterationEntity)
            )
        }
    }
    val redSweeper = sweeper("Test Red Sweeper", "{R}")
    val colorlessSweeper = sweeper("Test Colorless Sweeper", "{1}")
    val redPinger = card("Test Red Pinger") {
        manaCost = "{R}"
        typeLine = "Artifact"
        activatedAbility {
            cost = Costs.Mana("{1}")
            val t = target(Targets.Any)
            effect = Effects.DealDamage(1, t)
        }
    }

    fun driver() = GameTestDriver().also {
        it.registerCards(TestCards.all + listOf(warded, colorlessBolt, redSweeper, colorlessSweeper, redPinger))
        it.initMirrorMatch(Deck.of("Forest" to 40), skipMulligans = true, startingPlayer = 0)
        it.passPriorityUntil(Step.PRECOMBAT_MAIN)
    }
    fun GameTestDriver.damage(id: EntityId) = state.getEntity(id)?.get<DamageComponent>()?.amount ?: 0
    fun GameTestDriver.offeredTargets(player: EntityId, matches: (com.wingedsheep.engine.core.GameAction) -> Boolean) =
        legalActions(player).first { matches(it.action) }.let { la ->
            la.targetRequirements?.firstOrNull()?.validTargets ?: la.validTargets.orEmpty()
        }

    test("a colored spell can't target it — not even its controller's — and it isn't offered (CR 702.16b)") {
        val d = driver()
        val ward = d.putCreatureOnBattlefield(d.player1, warded.name)
        val bear = d.putCreatureOnBattlefield(d.player2, "Grizzly Bears")
        val bolt = d.putCardInHand(d.player1, "Lightning Bolt")
        d.giveMana(d.player1, Color.RED, 1)
        val offered = d.offeredTargets(d.player1) { it is CastSpell && it.cardId == bolt }
        offered.contains(ward) shouldBe false
        offered.contains(bear) shouldBe true
        d.castSpell(d.player1, bolt, listOf(ward)).error shouldNotBe null
    }

    test("a colorless spell can target and damage it") {
        val d = driver()
        val ward = d.putCreatureOnBattlefield(d.player2, warded.name)
        val spell = d.putCardInHand(d.player1, colorlessBolt.name)
        d.giveColorlessMana(d.player1, 1)
        d.offeredTargets(d.player1) { it is CastSpell && it.cardId == spell }.contains(ward) shouldBe true
        d.castSpell(d.player1, spell, listOf(ward)).error shouldBe null
        d.bothPass()
        d.damage(ward) shouldBe 2
    }

    test("damage from a colored spell that doesn't target is prevented (CR 702.16e)") {
        val d = driver()
        val ward = d.putCreatureOnBattlefield(d.player1, warded.name)
        d.putCreatureOnBattlefield(d.player2, "Grizzly Bears")
        val spell = d.putCardInHand(d.player1, redSweeper.name)
        d.giveMana(d.player1, Color.RED, 1)
        d.castSpell(d.player1, spell).error shouldBe null
        d.bothPass()
        d.damage(ward) shouldBe 0
        d.findPermanent(d.player2, "Grizzly Bears") shouldBe null
    }

    test("damage from a colorless spell that doesn't target is dealt") {
        val d = driver()
        val ward = d.putCreatureOnBattlefield(d.player1, warded.name)
        val spell = d.putCardInHand(d.player1, colorlessSweeper.name)
        d.giveColorlessMana(d.player1, 1)
        d.castSpell(d.player1, spell).error shouldBe null
        d.bothPass()
        d.damage(ward) shouldBe 2
    }

    test("an ability from a colored source is not a spell: it can target and damage it") {
        val d = driver()
        val ward = d.putCreatureOnBattlefield(d.player2, warded.name)
        val source = d.putPermanentOnBattlefield(d.player1, redPinger.name)
        val pingId = redPinger.script.activatedAbilities.single().id
        d.giveColorlessMana(d.player1, 1)
        d.offeredTargets(d.player1) { it is ActivateAbility && it.sourceId == source }.contains(ward) shouldBe true
        d.submit(ActivateAbility(d.player1, source, pingId, listOf(ChosenTarget.Permanent(ward)))).error shouldBe null
        d.bothPass()
        d.damage(ward) shouldBe 1
    }

    test("combat damage from a colored creature is dealt — a permanent is not a spell") {
        val d = driver()
        val ward = d.putCreatureOnBattlefield(d.player2, warded.name)
        val bear = d.putCreatureOnBattlefield(d.player1, "Grizzly Bears")
        d.removeSummoningSickness(bear)
        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(d.player1, listOf(bear), d.player2).error shouldBe null
        d.passPriorityUntil(Step.DECLARE_BLOCKERS)
        d.declareBlockers(d.player2, mapOf(ward to listOf(bear))).error shouldBe null
        d.passPriorityUntil(Step.END_COMBAT)
        d.damage(ward) shouldBe 2
    }

    test("a target that gains the protection before a colored spell resolves is illegal (CR 608.2b)") {
        val d = driver()
        val bear = d.putCreatureOnBattlefield(d.player2, "Grizzly Bears")
        val bolt = d.putCardInHand(d.player1, "Lightning Bolt")
        d.giveMana(d.player1, Color.RED, 1)
        d.castSpell(d.player1, bolt, listOf(bear)).error shouldBe null
        d.addComponent(bear, ProtectionComponent(colors = emptySet(), sourceKinds = setOf(SourceKind.COLORED_SPELL)))
        d.bothPass()
        d.findPermanent(d.player2, "Grizzly Bears") shouldBe bear
        d.damage(bear) shouldBe 0
    }
})
