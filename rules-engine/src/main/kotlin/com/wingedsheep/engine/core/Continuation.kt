package com.wingedsheep.engine.core

import kotlinx.serialization.Serializable

/**
 * Stored execution stack entries. A [Suspension] consumes a player's answer;
 * [AutomaticContinuation] resumes work by its position in the stack.
 */
@Serializable
sealed interface ContinuationFrame

/** Work waiting underneath a question. Its stack position supplies its relationship. */
@Serializable
sealed interface AutomaticContinuation : ContinuationFrame

/**
 * The data needed to consume one answer. It has no independent routing identity and cannot be
 * pushed onto the execution stack alone: [Suspension] associates it with the question it answers.
 */
@Serializable
sealed interface AnswerContinuation

/**
 * An [AnswerContinuation] whose question's *existence* is hidden from every player but the one
 * asked. Some questions are asked only because of a hidden fact — the miracle reveal question
 * exists only when the card just drawn has miracle — so even "your opponent is making a choice",
 * or a log line for a declined answer, would tell the table what the card is.
 *
 * The server sends no opponent or spectator decision status for such a question, and the answer
 * adds no line to the shared log ([pendingQuestionIsPrivate]). Contrast
 * [SecretChoiceContinuation], whose question is public and only its answer withheld.
 */
interface PrivateQuestion

/** Whether the question waiting on this state is a [PrivateQuestion]. */
val com.wingedsheep.engine.state.GameState.pendingQuestionIsPrivate: Boolean
    get() = (peekContinuation() as? Suspension)?.answer is PrivateQuestion
