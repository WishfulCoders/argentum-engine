package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.evaluation.EvalWeights
import com.wingedsheep.ai.engine.rollout.RolloutSettings

object AiProfiles {

    /**
     * Parse a `+`-joined profile name into an [AiProfile], applying its tokens left to right to
     * [AiProfile.CURRENT], the default AI.
     *
     * This is how a run names the agent it is talking about: the arena's `-Darena.profile` /
     * `-Darena.targetProfile`, and a gym env's pilot seat, both resolve a name through here, so
     * `raceclock+timing+correction-actions` means the same player whether a policy is being trained
     * against it or evaluated against it.
     *
     * - `current`: nothing.
     * - `raceclock`: the discounted race clock ([AiProfile.discountedRaceClock]; mtg-draft-ai `docs/28` §6).
     * - `intent`: card knowledge ([AiProfile.useCardIntent]); `timing`: that plus the upstream hold rules, no
     *   rollouts (`docs/27` §3, §7); `reserve`: `timing` plus [com.wingedsheep.ai.engine.evaluation.ManaReserve]
     *   at `-Darena.reserveWeight` (default 1.5), scaled by the deck's answers with `-Darena.reserveScaled=true`.
     * - `apprentice`: priority choices (not combat or decisions) scored by the linear model in
     *   `shared-apprentice.json` under `-Dargentum.ai.apprentice.dir` (the gameplay pilot, `docs/28`);
     *   `correction`: priority choices add the linear term in `shared-correction.json` (same directory) to the
     *   profile's own score; `correction-actions`: the same term choosing only which action once the
     *   uncorrected score has chosen to act (`docs/28` §5).
     *
     * - `rollout`: upstream's rollout evaluator on every decision; `holdup`: the same only where keeping mana up is
     *   the question ([AiProfile.rolloutsOnlyWhenHolding], `docs/28` §7), with the static leaf's share of a gated score at
     *   `-Darena.holdupStaticWeight` (default upstream's 0.75). Both sample the opponent's hidden cards
     *   ([AiProfile.determinizeHiddenInformation]), so a playout never plays their real hand.
     *
     * - `lookahead`: the one-response lookahead ([AiProfile.opponentRespondsInSimulation], `docs/27` §7.6). The
     *   measured arm was `timing+lookahead`, against `timing`.
     * - `idle`: in the last sorcery-speed window of its turn, a sorcery-speed cast needs to beat passing only by
     *   `-Darena.idleAllowance` (default 1.0) ([AiProfile.spendIdleManaAtSorcerySpeed], `docs/33` §13).
     *
     * - `eot`: the same for instant-speed casts in the opponent's end step, at `-Darena.eotAllowance` (default 3.0)
     *   ([AiProfile.spendIdleManaInTheirEndStep], `docs/33` §13.6).
     *
     * So `raceclock+timing+correction-actions` is the race clock, the hold rules and the correction together.
     * An apprentice or correction that did not load is an error, not a silent fallback to the default evaluator.
     */
    fun parse(name: String): AiProfile =
        name.split('+').fold(AiProfile.CURRENT) { profile, token -> withToken(profile, token) }
    
    private fun withToken(p: AiProfile, token: String): AiProfile {
        val id = if (p.id == AiProfile.CURRENT.id) "current-$token" else "${p.id}-$token"
        return when (token) {
            "current" -> p
            "raceclock" -> p.copy(id = id, discountedRaceClock = true)
            "intent" -> p.copy(id = id, useCardIntent = true)
            "timing" -> p.copy(
                id = id,
                useCardIntent = true,
                holdRemovalForBetterTargets = true,
                holdCountersForBetterSpells = true,
                cashCantripsInTheEndStep = true,
                holdFlashPermanentsForAmbush = true,
                holdExpiringGrantsForCombat = true,
                // combatTricksWaitForBlocks is left off: upstream pairs it with TieredBudgetPolicy and says the
                // two do not separate.
            )
            "reserve" -> {
                val weight = System.getProperty("arena.reserveWeight")?.toDouble() ?: 1.5
                val scaled = System.getProperty("arena.reserveScaled").toBoolean()
                withToken(p, "timing").copy(
                    id = "${id}-$weight" + if (scaled) "-scaled" else "",
                    manaReserveWeight = weight, manaReserveScalesWithDeck = scaled,
                )
            }
            "lookahead" -> p.copy(id = id, opponentRespondsInSimulation = true)
            "idle" -> {
                val allowance = System.getProperty("arena.idleAllowance")?.toDouble() ?: 1.0
                p.copy(id = "$id-$allowance", spendIdleManaAtSorcerySpeed = allowance)
            }
            "eot" -> {
                val allowance = System.getProperty("arena.eotAllowance")?.toDouble() ?: 3.0
                p.copy(id = "$id-$allowance", spendIdleManaInTheirEndStep = allowance)
            }
            "rollout" -> p.copy(id = id, rollouts = RolloutSettings.DEFAULT, determinizeHiddenInformation = true)
            "holdup" -> {
                val staticWeight = System.getProperty("arena.holdupStaticWeight")?.toDouble()
                p.copy(
                    id = id + (staticWeight?.let { "-$it" } ?: ""),
                    rollouts = staticWeight?.let { RolloutSettings.DEFAULT.copy(staticWeight = it) } ?: RolloutSettings.DEFAULT,
                    rolloutsOnlyWhenHolding = true, determinizeHiddenInformation = true,
                )
            }
            "apprentice" -> {
                require(EvalWeights.isRawProfile("shared-apprentice")) {
                    "no valid shared-apprentice.json under -Dargentum.ai.apprentice.dir"
                }
                p.copy(id = id, priorityEvalWeightsId = "shared-apprentice")
            }
            "correction", "correction-actions" -> {
                requireNotNull(EvalWeights.correction("shared-correction")) {
                    "no valid shared-correction.json under -Dargentum.ai.apprentice.dir"
                }
                p.copy(
                    id = id, priorityCorrectionId = "shared-correction",
                    priorityCorrectionChoosesActionOnly = token == "correction-actions",
                )
            }
            else -> error("unknown profile token $token in profile name")
        }
    }
}
