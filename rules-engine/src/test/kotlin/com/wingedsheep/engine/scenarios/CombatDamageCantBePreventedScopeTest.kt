package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.scripting.DamageCantBePrevented
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.events.DamageType
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * A `DamageCantBePrevented` scoped to **combat** damage ("Combat damage can't be prevented" —
 * Frenzied Baloth; "Combat damage that would be dealt by creatures you control can't be
 * prevented" — Questing Beast) shuts prevention off for combat damage only (CR 510.2, 615.12).
 *
 * Disciple of Law (protection from red) is the prevention under test: protection prevents damage
 * from red sources (CR 702.16e). A red creature's combat damage gets through; Pyroclasm's
 * noncombat damage is still prevented.
 */
class CombatDamageCantBePreventedScopeTest : ScenarioTestBase() {

    private val combatUnpreventer = card("Test Combat Unpreventer") {
        manaCost = "{2}{G}"
        colorIdentity = "G"
        typeLine = "Enchantment"
        oracleText = "Combat damage can't be prevented."
        replacementEffect(DamageCantBePrevented(appliesTo = EventPattern.DamageEvent(damageType = DamageType.Combat)))
    }

    private val sourceScopedUnpreventer = card("Test Questing Unpreventer") {
        manaCost = "{2}{G}"
        colorIdentity = "G"
        typeLine = "Enchantment"
        oracleText = "Combat damage that would be dealt by creatures you control can't be prevented."
        replacementEffect(
            DamageCantBePrevented(
                appliesTo = EventPattern.DamageEvent(
                    source = GameObjectFilter.Creature.youControl(),
                    damageType = DamageType.Combat,
                )
            )
        )
    }

    init {
        cardRegistry.register(combatUnpreventer)
        cardRegistry.register(sourceScopedUnpreventer)

        for (unpreventer in listOf("Test Combat Unpreventer", "Test Questing Unpreventer")) {
            context(unpreventer) {

                test("noncombat damage is still prevented") {
                    val game = scenario()
                        .withPlayers("Player", "Opponent")
                        .withCardOnBattlefield(1, unpreventer)
                        .withCardInHand(1, "Pyroclasm")
                        .withLandsOnBattlefield(1, "Mountain", 2)
                        .withCardOnBattlefield(2, "Disciple of Law")
                        .withActivePlayer(1)
                        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                        .build()
                    game.castSpell(1, "Pyroclasm").error shouldBe null
                    game.resolveStack()
                    withClue("protection from red prevented Pyroclasm's 2") {
                        game.isOnBattlefield("Disciple of Law") shouldBe true
                    }
                }

                test("combat damage from a red creature isn't prevented") {
                    val game = scenario()
                        .withPlayers("Player", "Opponent")
                        .withCardOnBattlefield(1, unpreventer)
                        .withCardOnBattlefield(1, "Goblin Piker", summoningSickness = false)
                        .withCardOnBattlefield(2, "Disciple of Law")
                        .withActivePlayer(1)
                        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                        .build()
                    game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    game.declareAttackers(mapOf("Goblin Piker" to 2)).error shouldBe null
                    game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                    game.declareBlockers(mapOf("Disciple of Law" to listOf("Goblin Piker"))).error shouldBe null
                    game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
                    withClue("the Piker's 2 combat damage got through protection") {
                        game.isOnBattlefield("Disciple of Law") shouldBe false
                    }
                }
            }
        }
    }
}
