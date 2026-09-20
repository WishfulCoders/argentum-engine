package com.wingedsheep.arena

import com.wingedsheep.engine.core.DeclareAttackers
import com.wingedsheep.engine.core.DeclareBlockers
import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.PassPriority
import com.wingedsheep.engine.legalactions.AdditionalCostData
import com.wingedsheep.engine.legalactions.CounterRemovalCreatureData
import com.wingedsheep.engine.legalactions.LegalAction
import com.wingedsheep.sdk.scripting.AbilityId
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class GameplayPolicyBridgeTest : FunSpec({
    val player = EntityId("player")
    val defender = EntityId("defender")
    val attacker1 = EntityId("attacker-1")
    val attacker2 = EntityId("attacker-2")
    val blocker1 = EntityId("blocker-1")
    val blocker2 = EntityId("blocker-2")

    test("attacker telemetry accepts either factorisation order but no duplicate or missing pair") {
        val action = DeclareAttackers(player, mapOf(attacker1 to defender, attacker2 to defender))
        GameplayPolicyBridge.combatOrderMatches(
            action, listOf(listOf(attacker2, defender), listOf(attacker1, defender)),
        ) shouldBe true
        GameplayPolicyBridge.combatOrderMatches(
            action, listOf(listOf(attacker1, defender), listOf(attacker1, defender)),
        ) shouldBe false
    }

    test("blocker telemetry preserves the order of multiple attackers assigned to one blocker") {
        val action = DeclareBlockers(
            player, mapOf(blocker1 to listOf(attacker2, attacker1), blocker2 to listOf(attacker1)),
        )
        GameplayPolicyBridge.combatOrderMatches(
            action,
            listOf(
                listOf(blocker2, attacker1),
                listOf(blocker1, attacker2),
                listOf(blocker1, attacker1),
            ),
        ) shouldBe true
        GameplayPolicyBridge.combatOrderMatches(
            action,
            listOf(
                listOf(blocker1, attacker1),
                listOf(blocker1, attacker2),
                listOf(blocker2, attacker1),
            ),
        ) shouldBe false
    }

    test("non-combat actions require an empty combat order") {
        GameplayPolicyBridge.combatOrderMatches(PassPriority(player), emptyList()) shouldBe true
        GameplayPolicyBridge.combatOrderMatches(
            PassPriority(player), listOf(listOf(attacker1, defender)),
        ) shouldBe false
    }

    test("actions whose selections ActionParams cannot carry are not callable") {
        GameplayPolicyBridge.policyCallable(
            LegalAction(PassPriority(player), "PassPriority", "Pass"),
        ) shouldBe true
        GameplayPolicyBridge.policyCallable(
            LegalAction(
                PassPriority(player), "CastSpell", "Cast with target tax",
                manaCostPerExtraTarget = "{1}",
            ),
        ) shouldBe false
        GameplayPolicyBridge.policyCallable(
            LegalAction(PassPriority(player), "CrewVehicle", "Crew"),
        ) shouldBe false
    }

    test("only automatic self-sacrifice additional costs enter the policy boundary") {
        val source = EntityId("source")
        val ability = ActivateAbility(player, source, AbilityId("activated"))
        val selfPayment = AdditionalCostData(
            "Sacrifice this", "SacrificeSelf", validSacrificeTargets = listOf(source),
        )
        val selfCost = LegalAction(
            ability, "ActivateAbility", "Sacrifice this",
            additionalCostInfo = selfPayment,
        )
        GameplayPolicyBridge.policyCallable(selfCost) shouldBe true
        GameplayPolicyBridge.policyCallable(
            selfCost.copy(additionalCostInfo = selfPayment.copy(
                costType = "TapPermanents", validTapTargets = listOf(source), tapCount = 1,
            )),
        ) shouldBe false
        GameplayPolicyBridge.policyCallable(
            selfCost.copy(additionalCostInfo = selfPayment.copy(
                validSacrificeTargets = listOf(EntityId("other")),
            )),
        ) shouldBe false
        GameplayPolicyBridge.policyCallable(
            selfCost.copy(additionalCostInfo = selfPayment.copy(
                counterRemovalCreatures = listOf(CounterRemovalCreatureData(source, "Source", 1)),
            )),
        ) shouldBe false
        GameplayPolicyBridge.policyCallable(selfCost.copy(hasConvoke = true)) shouldBe false
    }

    test("shadow-teacher telemetry uses stable coarse action families") {
        GameRunner.policyActionFamily(PassPriority(player)) shouldBe "pass"
        GameRunner.policyActionFamily(
            DeclareAttackers(player, mapOf(attacker1 to defender)),
        ) shouldBe "attack"
        GameRunner.policyActionFamily(
            DeclareBlockers(player, mapOf(blocker1 to listOf(attacker1))),
        ) shouldBe "block"
    }
})
