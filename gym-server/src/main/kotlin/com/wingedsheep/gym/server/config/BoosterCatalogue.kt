package com.wingedsheep.gym.server.config

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Which card names each set's limited pool holds, per Scryfall — the gym's reader of the same baked
 * catalogue `game-server`'s `SetCoverageService.limitedCardNames` reads (`coverage/set-totals.json`,
 * copied onto this module's classpath by `gym-server/build.gradle.kts`).
 *
 * The per-card `metadata.inBooster` flag cannot be trusted for a set Scryfall has not finished
 * flagging: before Reality Fracture's release, 270 of its 285 cards carry `booster: false`, so
 * `BoosterGenerator`'s `inBooster` filter left FRA sealed pools drawing from ~15 cards. The
 * catalogue's rule — the set's booster (`draft`) list, or its whole card list when it has none —
 * is what the game server's lobby builds its pools from, and this keeps the gym on the same pool.
 */
internal object BoosterCatalogue {
    const val RESOURCE_PATH = "coverage/set-totals.json"

    @Serializable
    private data class CatalogueCard(val name: String)

    @Serializable
    private data class CatalogueSet(
        val code: String,
        val draft: List<CatalogueCard> = emptyList(),
        val extra: List<CatalogueCard> = emptyList(),
    ) {
        /** Mirrors `SetCoverageService.CanonicalSet.mainCards`. */
        val mainCards: List<CatalogueCard> get() = draft.ifEmpty { extra }
    }

    private val byCode: Map<String, CatalogueSet> by lazy {
        val text = BoosterCatalogue::class.java.classLoader.getResourceAsStream(RESOURCE_PATH)
            ?.bufferedReader()?.use { it.readText() }
            ?: return@lazy emptyMap()
        Json { ignoreUnknownKeys = true }.decodeFromString<List<CatalogueSet>>(text).associateBy { it.code }
    }

    /** True when the catalogue resource is on the classpath at all. */
    val available: Boolean get() = byCode.isNotEmpty()

    /**
     * Distinct names eligible for [setCode]'s limited pool, or null when the catalogue has no row
     * for the set (the caller then falls back to the cards' own `inBooster` flags).
     */
    fun limitedCardNames(setCode: String): Set<String>? =
        byCode[setCode.uppercase()]?.mainCards?.mapTo(linkedSetOf()) { it.name }
}
