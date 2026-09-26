package com.wingedsheep.tooling.coverage

import com.wingedsheep.tooling.coverage.emitter.Emitter
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.add
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

/**
 * Pins the three player scopes of the `AtTheBeginningOfAPlayersUpkeep` phase trigger that the emitter
 * maps to a named `Triggers.*` constant — `You` -> YourUpkeep, `AnyPlayer` -> EachUpkeep, `Opponent`
 * -> EachOpponentUpkeep — and confirms the host-relative scope (`HostController`, an Aura granting an
 * upkeep trigger to the enchanted permanent's controller) still declines to a SCAFFOLD rather than
 * being silently widened to one of the named scopes.
 *
 * Each fixture is a synthetic single-trigger card whose action (this permanent deals 1 damage to the
 * triggering player) renders whole, so the only thing under test is which trigger constant the scope
 * resolves to. Hermetic: no IR download, no Scryfall cache.
 */
class UpkeepTriggerScopeTest : StringSpec({

    val effects = Registry.loadEffectSerialNames()
    val keywords = Registry.loadKeywords()

    // Builds an upkeep-trigger card. `playersScope` is the `_Players` value; `playerArg`, when non-null,
    // is the nested `_Player` value (the `SinglePlayer` shape carries one, `AnyPlayer`/`Opponent` don't).
    fun upkeepCard(playersScope: String, playerArg: String?): JsonObject = buildJsonObject {
        put("Name", JsonPrimitive("Test Upkeep $playersScope${playerArg?.let { " $it" } ?: ""}"))
        putJsonObject("Typeline") {
            putJsonArray("Supertypes") {}
            putJsonArray("Cardtypes") { add(JsonPrimitive("Enchantment")) }
            putJsonArray("Subtypes") {}
        }
        putJsonArray("ManaCost") { addJsonObject { put("_ManaSymbol", JsonPrimitive("ManaCostB")) } }
        putJsonArray("Rules") {
            addJsonObject {
                put("_Rule", JsonPrimitive("TriggerA"))
                putJsonArray("args") {
                    // The trigger node.
                    addJsonObject {
                        put("_Trigger", JsonPrimitive("AtTheBeginningOfAPlayersUpkeep"))
                        putJsonObject("args") {
                            put("_Players", JsonPrimitive(playersScope))
                            if (playerArg != null) putJsonObject("args") { put("_Player", JsonPrimitive(playerArg)) }
                        }
                    }
                    // A single renderable action: this permanent deals 1 damage to the triggering player.
                    addJsonObject {
                        put("_Actions", JsonPrimitive("ActionList"))
                        putJsonArray("args") {
                            addJsonObject {
                                put("_Action", JsonPrimitive("PermanentDealsDamage"))
                                putJsonArray("args") {
                                    addJsonObject { put("_Permanent", JsonPrimitive("ThisPermanent")) }
                                    addJsonObject {
                                        put("_GameNumber", JsonPrimitive("Integer"))
                                        put("args", JsonPrimitive(1))
                                    }
                                    addJsonObject {
                                        put("_DamageRecipient", JsonPrimitive("Player"))
                                        putJsonObject("args") { put("_Player", JsonPrimitive("Trigger_ThatPlayer")) }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    fun render(playersScope: String, playerArg: String?) =
        Emitter.renderCard(upkeepCard(playersScope, playerArg), null, effects, keywords)

    "the SinglePlayer/You scope maps to Triggers.you.beginningOf(Step.UPKEEP)" {
        val r = render("SinglePlayer", "You")
        r.complete shouldBe true
        r.text shouldContain "trigger = Triggers.you.beginningOf(Step.UPKEEP)"
    }

    "the AnyPlayer scope maps to Triggers.anyPlayer.beginningOf(Step.UPKEEP)" {
        val r = render("AnyPlayer", null)
        r.complete shouldBe true
        r.text shouldContain "trigger = Triggers.anyPlayer.beginningOf(Step.UPKEEP)"
    }

    "the Opponent scope maps to Triggers.anOpponent.beginningOf(Step.UPKEEP)" {
        val r = render("Opponent", null)
        r.complete shouldBe true
        r.text shouldContain "trigger = Triggers.anOpponent.beginningOf(Step.UPKEEP)"
    }

    "the host-relative HostController scope declines to a scaffold, not a named scope" {
        val r = render("SinglePlayer", "HostController")
        r.complete shouldBe false
        r.text shouldNotContain "Triggers.you.beginningOf(Step.UPKEEP)"
        r.text shouldNotContain "Triggers.anyPlayer.beginningOf(Step.UPKEEP)"
        r.text shouldNotContain "Triggers.anOpponent.beginningOf(Step.UPKEEP)"
    }
})
