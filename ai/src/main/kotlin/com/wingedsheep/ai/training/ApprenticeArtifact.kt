package com.wingedsheep.ai.training

import com.wingedsheep.ai.engine.evaluation.CardValueTable
import com.wingedsheep.ai.engine.evaluation.RawBoardFeatures
import com.wingedsheep.ai.engine.evaluation.RawEvaluationWeights
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

const val APPRENTICE_ARTIFACT_SCHEMA_VERSION = 2

/**
 * Schema 1 carried the linear model alone. **Schema 2 adds the card table** ([CardValueTable],
 * mtg-draft-ai `docs/40`); both are loadable, and a schema-2 file with no card values is a schema-1
 * model written by a newer fitter.
 */
val APPRENTICE_ARTIFACT_SCHEMA_VERSIONS = setOf(1, 2)

/**
 * Small, immutable JVM artifact produced offline.
 *
 * Card identities were deliberately absent from schema 1. Schema 2 admits them in one place only —
 * [cardValues], a value per card per zone — and only for the zones the acting side may look at
 * (`docs/40` §1), so nothing loaded here can be played dishonestly.
 */
@Serializable
data class ApprenticeArtifact(
    val schemaVersion: Int = APPRENTICE_ARTIFACT_SCHEMA_VERSION,
    val modelId: String,
    val setCode: String? = null,
    val featureNames: List<String>,
    val sharedCoefficients: List<Double>,
    val setOverlayCoefficients: List<Double> = emptyList(),
    val intercept: Double = 0.0,
    /** Schema 2: [CardValueTable.ZONES] when [cardValues] is set, empty otherwise. */
    val cardZones: List<String> = emptyList(),
    /** Schema 2: card name -> one value per zone of [cardZones], in the model's own units. */
    val cardValues: Map<String, List<Double>> = emptyMap(),
) {
    fun toEvaluationWeights(applyOverlay: Boolean): RawEvaluationWeights {
        require(validationErrors().isEmpty())
        val coefficients = featureNames.indices.associate { index ->
            val overlay = if (applyOverlay) setOverlayCoefficients.getOrElse(index) { 0.0 } else 0.0
            featureNames[index] to (sharedCoefficients[index] + overlay)
        }
        return RawEvaluationWeights(intercept = intercept, weights = coefficients, cards = cardTable())
    }
    /** The card term, or null for a model without one. */
    fun cardTable(): CardValueTable? =
        if (cardValues.isEmpty()) null else CardValueTable(cardZones, cardValues)

    fun validationErrors(): List<String> = buildList {
        if (schemaVersion !in APPRENTICE_ARTIFACT_SCHEMA_VERSIONS) add("unsupported schema version")
        if (modelId.isBlank()) add("blank model id")
        if (featureNames.toSet() != RawBoardFeatures.names || featureNames.size != RawBoardFeatures.names.size) {
            add("feature schema mismatch")
        }
        if (sharedCoefficients.size != featureNames.size) add("shared coefficient count mismatch")
        if (setOverlayCoefficients.isNotEmpty() && setOverlayCoefficients.size != featureNames.size) {
            add("overlay coefficient count mismatch")
        }
        if (!intercept.isFinite() || sharedCoefficients.any { !it.isFinite() } || setOverlayCoefficients.any { !it.isFinite() }) {
            add("non-finite model value")
        }
        if (setOverlayCoefficients.isNotEmpty() && setCode.isNullOrBlank()) add("overlay requires set code")
        if (cardValues.isNotEmpty() || cardZones.isNotEmpty()) {
            if (schemaVersion < 2) add("card values need schema 2")
            if (cardZones != CardValueTable.ZONES) add("card zone schema mismatch")
            if (cardValues.isEmpty()) add("card zones without card values")
            else if (cardTable()?.isValid() != true) add("malformed card value")
        }
    }
}

object ApprenticeArtifactLoader {
    private val json = Json { ignoreUnknownKeys = false }

    fun decodeOrNull(text: String, expectedSet: String? = null): ApprenticeArtifact? = runCatching {
        json.decodeFromString<ApprenticeArtifact>(text)
    }.getOrNull()?.takeIf { artifact ->
        artifact.validationErrors().isEmpty() &&
            (artifact.setCode == null || expectedSet?.uppercase() == artifact.setCode.uppercase())
    }
}
