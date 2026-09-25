package com.wingedsheep.ai.engine.rollout

import com.wingedsheep.ai.engine.budget.DecisionBudget
import com.wingedsheep.ai.engine.evaluation.ManaReserve
import com.wingedsheep.ai.engine.knowledge.IntentCatalog
import com.wingedsheep.ai.engine.lifePoolsOf
import com.wingedsheep.ai.engine.sidesFor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.model.EntityId
import java.io.BufferedWriter
import java.io.FileOutputStream
import java.util.concurrent.atomic.AtomicLong

/**
 * Which decisions get the rollouts — mtg-draft-ai `docs/55`.
 *
 * @param margin roll out only when the static leaf's best two candidates are within this many raw evaluator units.
 *   Null rolls out every decision, exactly as the ungated `rollout` token does, which is the diagnostic setting.
 * @param logPath append one JSON line per scored decision here. Null logs nothing.
 */
data class RolloutGate(val margin: Double? = null, val logPath: String? = null)

/**
 * Rollouts only where the static leaf cannot tell its best candidates apart — mtg-draft-ai `docs/55`.
 *
 * `docs/53` §5 put the whole rollout gain in search, and a decision's playout budget is a fixed count, so the only
 * way to spend less on a game is to skip decisions. The static scores cost nothing next to the playouts, so this
 * computes them first and hands a decision to [rollout] only when the gap between the static best and runner-up is
 * under [RolloutGate.margin]. A skipped decision scores with [static], the same fallback [HoldingGatedEvaluator]
 * and the rollout's own ROUTINE tier use.
 *
 * With no margin every decision goes to [rollout] and the scores are the rollout's own, so an arm with the gate's
 * log on plays the same games as the plain `rollout` token. That is the diagnostic: each line records the static
 * gap and whether the rollout changed the static pick, which is what a margin has to be chosen from.
 */
class MarginGatedEvaluator(
    private val rollout: CandidateEvaluator,
    private val static: CandidateEvaluator,
    private val gate: RolloutGate,
    private val intents: IntentCatalog? = null,
) : CandidateEvaluator {

    override fun score(root: GameState, afterAction: GameState, playerId: EntityId, budget: DecisionBudget): Double =
        scoreAll(root, listOf(afterAction), playerId, budget).first()

    override fun scoreAll(
        root: GameState,
        afterActions: List<GameState>,
        playerId: EntityId,
        budget: DecisionBudget,
    ): List<Double> {
        if (afterActions.isEmpty()) return emptyList()
        decisions.incrementAndGet()
        val staticScores = static.scoreAll(root, afterActions, playerId, budget)
        val gap = topTwoGap(staticScores)
        val margin = gate.margin
        val rolled = margin == null || gap < margin
        val start = System.nanoTime()
        val scores = if (rolled) rollout.scoreAll(root, afterActions, playerId, budget) else staticScores
        val nanos = System.nanoTime() - start
        val staticPick = argmax(staticScores)
        val pick = argmax(scores)
        if (rolled) {
            rolledOut.incrementAndGet()
            if (pick != staticPick) overturned.incrementAndGet()
        }
        gate.logPath?.let { path ->
            log(path, line(root, playerId, afterActions.size, gap, rolled, staticPick, pick, scores, nanos))
        }
        return scores
    }

    private fun line(
        root: GameState,
        playerId: EntityId,
        candidates: Int,
        gap: Double,
        rolled: Boolean,
        staticPick: Int,
        pick: Int,
        scores: List<Double>,
        nanos: Long,
    ): String {
        val sides = root.sidesFor(playerId)
        val myLife = sides?.let { root.lifePoolsOf(it.mine).minOrNull() }
        val theirLife = sides?.let { s -> s.opponents.mapNotNull { root.lifePoolsOf(it).minOrNull() }.minOrNull() }
        val holding = intents?.let {
            root.stack.isEmpty() && ManaReserve.holdsUpAnswer(root, root.projectedState, playerId, it)
        }
        // What the rollout thinks the change of pick is worth, in the same raw units as the gap.
        val regret = if (rolled && pick != staticPick) scores[pick] - scores[staticPick] else 0.0
        return buildString {
            append("{\"turn\":").append(root.turnNumber)
            append(",\"step\":\"").append(root.step.name).append('"')
            append(",\"active\":").append(root.isActiveTurnFor(playerId))
            append(",\"stack\":").append(root.stack.size)
            append(",\"life\":").append(myLife).append(",\"opp_life\":").append(theirLife)
            append(",\"holding\":").append(holding)
            append(",\"n\":").append(candidates)
            append(",\"gap\":").append(if (gap.isFinite()) "%.4f".format(gap) else "null")
            append(",\"rolled\":").append(rolled)
            append(",\"static_pick\":").append(staticPick).append(",\"pick\":").append(pick)
            append(",\"regret\":").append("%.4f".format(regret))
            append(",\"ms\":").append("%.2f".format(nanos / 1e6))
            append('}')
        }
    }

    override fun toString(): String = "rollout-when-close(${gate.margin}, $rollout)"

    companion object {
        private val decisions = AtomicLong()
        private val rolledOut = AtomicLong()
        private val overturned = AtomicLong()
        private val writers = HashMap<String, BufferedWriter>()

        /**
         * The gap between the best and second-best score, or infinity when there is no second candidate or a
         * terminal sentinel makes the gap meaningless (a win on the board needs no playout).
         */
        fun topTwoGap(scores: List<Double>): Double {
            if (scores.size < 2) return Double.POSITIVE_INFINITY
            val sorted = scores.sortedDescending()
            val gap = sorted[0] - sorted[1]
            return if (gap.isFinite()) gap else Double.POSITIVE_INFINITY
        }

        private fun argmax(scores: List<Double>): Int = scores.indices.maxByOrNull { scores[it] } ?: -1

        private fun log(path: String, line: String) = synchronized(writers) {
            val w = writers.getOrPut(path) {
                // Appends, so a resumed arena keeps the lines of the games it already played.
                FileOutputStream(path, true).bufferedWriter().also { opened ->
                    Runtime.getRuntime().addShutdownHook(Thread { synchronized(writers) { opened.flush() } })
                }
            }
            w.write(line)
            w.newLine()
        }

        /** Flush every open log. The arena calls it at the end of a run; a shutdown hook covers the rest. */
        fun flush() = synchronized(writers) { writers.values.forEach { it.flush() } }

        /** Counts across every instance in the process, for the arena's end-of-run line. */
        fun summary(): String =
            "rollout gate: ${rolledOut.get()} of ${decisions.get()} scored decisions went to the rollouts; " +
                "their pick differed from the static pick in ${overturned.get()}"
    }
}
