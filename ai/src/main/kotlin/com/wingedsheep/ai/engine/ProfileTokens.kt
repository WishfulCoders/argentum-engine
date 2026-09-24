package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.budget.LegacyBudgetPolicy
import com.wingedsheep.ai.engine.budget.RolloutBudgetPolicy
import com.wingedsheep.ai.engine.evaluation.EvalWeights
import com.wingedsheep.ai.engine.rollout.RolloutSettings

/**
 * A profile named by `+`-joined tokens, applied left to right to [AiProfile.CURRENT], the default AI.
 *
 * The arena reads the name from `-Darena.profile` (both seats) and `-Darena.targetProfile` (the target's
 * seat only); the game-server reads it from `game.ai.profile`, so a human can play the same profile an
 * arena run measured (mtg-draft-ai `docs/44`).
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
 *   ([AiProfile.determinizeHiddenInformation]), so a playout never plays their real hand. `rollout` also reads
 *   `-Darena.rolloutCutoff` (the playout's early cutoff margin) and `-Darena.rolloutPlayouts` (playouts per
 *   decision, default 16), both mtg-draft-ai `docs/54`.
 *
 * - `determinize`: the AI samples the opponent's hidden cards instead of reading them
 *   ([AiProfile.determinizeHiddenInformation]) — the fairness token for playing a human, see below.
 * - `lookahead`: the one-response lookahead ([AiProfile.opponentRespondsInSimulation], `docs/27` §7.6). The
 *   measured arm was `timing+lookahead`, against `timing`.
 * - `fixing`: the AI can see a colour (mtg-draft-ai `docs/46`) — a land that makes no mana is priced
 *   as one ([AiProfile.priceSacrificeLandsAsNoMana], so it finally cracks a fetch land) and a land
 *   search is ranked by the colour it buys ([AiProfile.choosesLandsByColour]), and the board is
 *   charged for a colour the hand needs and cannot reach ([AiProfile.chargesForUnavailableColours],
 *   which is what fixes the *land drop*). Implies `intent`, where "makes no mana and eats itself"
 *   is read from.
 * - `idle`: in the last sorcery-speed window of its turn, a sorcery-speed cast needs to beat passing only by
 *   `-Darena.idleAllowance` (default 1.0) ([AiProfile.spendIdleManaAtSorcerySpeed], `docs/33` §13).
 *
 * - `eot`: the same for instant-speed casts in the opponent's end step, at `-Darena.eotAllowance` (default 3.0)
 *   ([AiProfile.spendIdleManaInTheirEndStep], `docs/33` §13.6).
 *
 * So `raceclock+timing+correction-actions` is the race clock, the hold rules and the correction together.
 * An apprentice or correction that did not load is an error, not a silent fallback to the default evaluator.
 */
fun profileFromTokens(name: String): AiProfile =
    name.split('+').fold(AiProfile.CURRENT) { profile, token -> withToken(profile, token) }

private fun withToken(p: AiProfile, token: String): AiProfile {
    val id = if (p.id == AiProfile.CURRENT.id) "current-$token" else "${p.id}-$token"
    return when (token) {
        "current" -> p
        "raceclock" -> p.copy(id = id, discountedRaceClock = true)
        "intent" -> p.copy(id = id, useCardIntent = true)
        "locked" -> p.copy(
            id = id,
            creatureValuation = p.creatureValuation.copy(lockedCreaturesAreInert = true),
        )
        "grants" -> p.copy(
            id = id,
            useCardIntent = true,
            holdExpiringGrantsForCombat = true,
            expiringGrantsNeedACombat = true,
            tapCostsKeepBlockersUp = true,
        )
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
        /*
         * Install the determinizer without the rollout stack that normally brings it.
         *
         * A profile without it evaluates the *real* state, and `RawBoardFeatures` reads card names
         * out of both hands for `removalInHandDifference` — so the AI knows how much removal its
         * opponent is holding. Symmetric and therefore harmless in an arena arm where both seats do
         * it; not symmetric when a human is in one of the seats, which is what this token is for
         * (mtg-draft-ai `docs/44` §4).
         */
        "determinize" -> p.copy(id = id, determinizeHiddenInformation = true)
        "lookahead" -> p.copy(id = id, opponentRespondsInSimulation = true)
        "fixing" -> p.copy(
            id = id,
            useCardIntent = true,
            priceSacrificeLandsAsNoMana = true,
            choosesLandsByColour = true,
            chargesForUnavailableColours = true,
        )
        "idle" -> {
            val allowance = System.getProperty("arena.idleAllowance")?.toDouble() ?: 1.0
            p.copy(id = "$id-$allowance", spendIdleManaAtSorcerySpeed = allowance)
        }
        "eot" -> {
            val allowance = System.getProperty("arena.eotAllowance")?.toDouble() ?: 3.0
            p.copy(id = "$id-$allowance", spendIdleManaInTheirEndStep = allowance)
        }
        "rollout" -> {
            // mtg-draft-ai docs/54: stop a playout once its leaf is this far from even. Unset keeps the default.
            val cutoff = System.getProperty("arena.rolloutCutoff")?.toDouble()
            // docs/54 §5: playouts per decision. RolloutBudgetPolicy(n) is the legacy budget with only the playout
            // count changed, so it is a single knob only on a legacy-budget profile; 16 reproduces the default.
            val playouts = System.getProperty("arena.rolloutPlayouts")?.toInt()
            if (playouts != null) {
                require(p.budgetPolicy === LegacyBudgetPolicy) { "arena.rolloutPlayouts needs the legacy budget policy" }
            }
            p.copy(
                id = id + (cutoff?.let { "-cut$it" } ?: "") + (playouts?.let { "-p$it" } ?: ""),
                rollouts = RolloutSettings.DEFAULT.copy(earlyCutoffMargin = cutoff),
                budgetPolicy = playouts?.let { RolloutBudgetPolicy(it) } ?: p.budgetPolicy,
                determinizeHiddenInformation = true,
            )
        }
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
        else -> error("unknown profile token $token")
    }
}
