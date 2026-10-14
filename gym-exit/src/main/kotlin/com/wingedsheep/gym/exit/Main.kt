package com.wingedsheep.gym.exit

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import java.io.File

/**
 * mtg-draft-ai docs/65 §3 (E1) entry points.
 *
 *     parity FIXTURES_DIR MODEL_DIR [limit]   featurizer + net + heads against scripts/e1_record.py fixtures
 *     gate DEALS MODEL_DIR OUT ARM [lanes] [deals per lane]
 *                                             T1's deals (scripts/e1_deals.py) with one arm (A0, A0g, S<n>), lanes as
 *                                             threads over one batching queue; -De1.stream=64 is T1's deal stream
 */
fun main(args: Array<String>) {
    when (args.firstOrNull()) {
        "parity" -> {
            val fixtures = File(args[1]).listFiles { f -> f.name.endsWith(".jsonl.gz") }!!.sorted()
            val limit = args.getOrNull(3)?.toInt() ?: Int.MAX_VALUE
            PolicyNet(File(args[2]), optLevel = System.getProperty("e1.opt") ?: "all").use { net ->
                val batch = System.getProperty("e1.batch")?.toInt() ?: 16
                val report = Parity(GameplayPolicy(net), batch).run(fixtures, limit)
                println(gymJson.encodeToString(JsonObject.serializer(), report))
            }
        }
        "gate" -> {
            val deals = Gate.readDeals(File(args[1]))
            val arm = args[4]
            val lanes = args.getOrNull(5)?.toInt() ?: 1
            val limit = args.getOrNull(6)?.toInt() ?: Int.MAX_VALUE
            val engine = Engine()
            PolicyNet(File(args[2])).use { net ->
                val policy = GameplayPolicy(net)
                BatchingEvaluator(policy).use { batcher ->
                    val gate = Gate(engine, policy, batcher, arm, System.getProperty("e1.stream")?.toLong() ?: 64L, Gate.parseArm(arm))
                    val t0 = System.nanoTime()
                    val threads = (0 until lanes).map { lane ->
                        Thread({ gate.runLane(deals, File(args[3]), lane, lanes, limit) }, "lane$lane").apply { start() }
                    }
                    threads.forEach { it.join() }
                    val rows = batcher.rows.get()
                    System.err.println(
                        "wall ${"%.1f".format((System.nanoTime() - t0) / 1e9)} s; net ${batcher.batches.get()} batches, " +
                            "$rows rows (${"%.2f".format(rows.toDouble() / maxOf(1, batcher.batches.get()))}/batch), " +
                            "${"%.1f".format(batcher.inferenceNanos.get() / 1e9)} s inference",
                    )
                }
            }
        }
        "netbench" -> {
            // Encoder cost alone on recorded observations: batch sizes 1, 4, 16, 64, ms per batch and per row.
            val n = args.getOrNull(3)?.toInt() ?: 512
            val prepared = ArrayList<Prepared>()
            PolicyNet(File(args[2]), optLevel = System.getProperty("e1.opt") ?: "all").use { net ->
                val policy = GameplayPolicy(net)
                val file = File(args[1]).listFiles { f -> f.name.endsWith(".jsonl.gz") }!!.sorted().first()
                java.util.zip.GZIPInputStream(file.inputStream()).bufferedReader().useLines { lines ->
                    for (line in lines) {
                        if (prepared.size >= n) break
                        val json = gymJson.parseToJsonElement(line).jsonObject
                        val obs = gymJson.decodeFromJsonElement(com.wingedsheep.gym.contract.TrainingObservation.serializer(), json.getValue("obs"))
                        try { prepared += policy.prepare(obs) } catch (_: InferenceError) {}
                    }
                }
                for (round in 0 until 2) for (b in listOf(1, 4, 16, 64)) {
                    val t0 = System.nanoTime()
                    var batches = 0
                    for (i in 0 until prepared.size step b) { policy.encode(prepared.subList(i, minOf(prepared.size, i + b))); batches++ }
                    val ms = (System.nanoTime() - t0) / 1e6
                    if (round == 1) println("batch $b: ${"%.3f".format(ms / batches)} ms/batch, ${"%.3f".format(ms / prepared.size)} ms/row")
                }
            }
        }
        else -> error("usage: parity FIXTURES MODEL [limit] | gate DEALS MODEL OUT ARM [lanes] [limit]")
    }
}
