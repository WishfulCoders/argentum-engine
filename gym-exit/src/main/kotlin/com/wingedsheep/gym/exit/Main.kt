package com.wingedsheep.gym.exit

import kotlinx.serialization.json.JsonObject
import java.io.File

/**
 * mtg-draft-ai docs/65 §3 (E1) entry points.
 *
 *     parity FIXTURES_DIR MODEL_DIR [limit]   featurizer + net + heads against scripts/e1_record.py fixtures
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
        else -> error("usage: parity FIXTURES MODEL [limit]")
    }
}
