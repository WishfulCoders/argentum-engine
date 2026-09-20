package com.wingedsheep.gym.server.dto

import com.wingedsheep.gym.EnvStatus
import com.wingedsheep.gym.contract.ActionParams
import com.wingedsheep.gym.contract.Observation
import com.wingedsheep.gym.service.EnvId
import com.wingedsheep.gym.service.SnapshotHandle
import kotlinx.serialization.Serializable

/**
 * Response for `POST /envs` (and `POST /envs/deckbuild`). Combines the new env's ID
 * with its opening observation so a caller only has to round-trip once to start.
 * The [observation] is a discriminated union — `TrainingObservation` for a game env,
 * `DeckbuildObservation` for a deckbuild env (see the `type` field).
 */
@Serializable
data class CreateEnvResponse(
    val envId: EnvId,
    val observation: Observation,
    /** Episode bookkeeping, including the seed this game was initialised with. */
    val status: EnvStatus = EnvStatus(terminated = false),
)

/**
 * Body for `POST /envs/{id}/step`.
 *
 * [params] completes an action whose enumerated form is a template — which creatures attack and
 * whom, which blocks are made, a spell's targets, X. Omit it for actions that need no choice beyond
 * their ID; see [ActionParams] for what is (and isn't) expressible here.
 */
@Serializable
data class StepBody(
    val actionId: Int,
    val params: ActionParams = ActionParams.EMPTY
)

/** Single entry for `POST /envs/step-batch`. */
@Serializable
data class StepBatchItem(
    val envId: EnvId,
    val actionId: Int,
    val params: ActionParams = ActionParams.EMPTY
)

/**
 * Result entry for `POST /envs/step-batch`.
 *
 * [status] rides along on every stepped env so a training loop learns that an episode ended — and
 * what it paid — without a second round trip per decision.
 */
@Serializable
data class StepBatchResult(
    val envId: EnvId,
    val observation: Observation,
    val status: EnvStatus = EnvStatus(terminated = false),
)

/**
 * Result for `POST /envs/{id}/playout`.
 *
 * The caller of a playout wants the outcome, not the position, so [status] — which carries the
 * terminal reward and the truncation reason — is the part that does the work here. The observation
 * rides along because a playout that stopped short of a terminal leaves the env on a live decision.
 */
@Serializable
data class PlayoutResult(
    val observation: Observation,
    val status: EnvStatus,
)

/** Body for `POST /envs/{id}/restore`. */
@Serializable
data class RestoreBody(val handle: SnapshotHandle)

/** Body for `DELETE /envs`. List of envs to dispose. */
@Serializable
data class DisposeBody(val envIds: List<EnvId>)

/** Response for `GET /schema-hash` and `GET /health`. */
@Serializable
data class SchemaHashResponse(val schemaHash: String)

@Serializable
data class HealthResponse(val status: String = "ok")

/** Shared error envelope for `@ExceptionHandler` responses. */
@Serializable
data class ErrorResponse(
    val code: String,
    val message: String
)
