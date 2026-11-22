package com.wingedsheep.sdk.core

import kotlinx.serialization.Serializable

/**
 * A **single-holder player designation** — a title exactly one player can have at a time, that moves
 * from player to player rather than being granted to several (CR 725.3, CR 726.3).
 *
 * Both share their whole shape: there is none in a game until an effect hands one out, taking it
 * strips it from whoever had it, combat damage dealt to the holder moves it to the attacker's
 * controller, and it passes on when the holder leaves the game (CR 725.4, CR 726.4). What differs is
 * the inherent triggered abilities that come with holding it (CR 725.2, CR 726.2), which the engine
 * owns.
 *
 * Not for designations every player may hold at once and never lose — the city's blessing and an
 * enduring story are per-player markers with their own conditions.
 */
@Serializable
enum class PlayerDesignation(
    /** "the monarch" / "the initiative" — the noun the rules text uses. */
    val noun: String,
) {
    /** The monarch (CR 725). */
    MONARCH("the monarch"),

    /** The initiative (CR 726). */
    INITIATIVE("the initiative");
}
