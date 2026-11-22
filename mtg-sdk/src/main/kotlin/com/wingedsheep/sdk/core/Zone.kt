package com.wingedsheep.sdk.core

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
/**
 * @property displayName the zone as a standalone phrase, article included ("a library", "the
 *   battlefield", "exile") — for "in ${'$'}displayName", "from ${'$'}displayName".
 * @property noun the bare zone noun ("library", "battlefield") — for text where a possessive
 *   precedes the zone ("your library", "its owner's graveyard"), which must never read "your a library".
 */
enum class Zone(val displayName: String, val noun: String) {
    @SerialName("Library") LIBRARY("a library", "library"),
    @SerialName("Hand") HAND("a hand", "hand"),
    @SerialName("Battlefield") BATTLEFIELD("the battlefield", "battlefield"),
    @SerialName("Graveyard") GRAVEYARD("a graveyard", "graveyard"),
    @SerialName("Stack") STACK("the stack", "stack"),
    @SerialName("Exile") EXILE("exile", "exile"),
    @SerialName("Command") COMMAND("the command zone", "command zone"),

    /**
     * A player's sideboard — the cards they own *outside the game* (CR 100.4, 400.11a). Strictly,
     * "outside the game is not a zone" (CR 400.11), but the engine models the sideboard as a
     * private per-player pseudo-zone so the "wish" mechanic (Burning Wish, Living Wish, …) can
     * reach it with the ordinary Gather → Select → Move pipeline instead of bespoke machinery.
     *
     * It is private to its owner (masked from opponents, like [LIBRARY]/[HAND]) and, consistent
     * with CR 400.11c ("cards outside the game can't be affected by spells or abilities"), nothing
     * but a wish effect should ever gather from it. In constructed it is an explicit list (≤15,
     * CR 100.4a); in Limited it is every card in the player's pool not in their deck (CR 100.4b).
     */
    @SerialName("Sideboard") SIDEBOARD("a sideboard", "sideboard"),

    /**
     * The ante — the zone of cards played "for keeps" (CR 407). It exists only in a game played
     * for ante: each player antes a random card of their deck before the opening draw (407.2),
     * and only the handful of cards that read "Remove this card from your deck before playing if
     * you're not playing for ante" ever move cards into or out of it (407.3). Public (400.2) and
     * shared (400.1); like [EXILE] the engine keys it by owner, so "the cards you own in the ante"
     * is one zone read. When the game ends, its winner becomes the owner of everything in it.
     */
    @SerialName("Ante") ANTE("the ante", "ante");

    val isPublic: Boolean
        get() = this in listOf(BATTLEFIELD, GRAVEYARD, STACK, EXILE, COMMAND, ANTE)

    val isHidden: Boolean
        get() = this in listOf(LIBRARY, HAND, SIDEBOARD)

    val isShared: Boolean
        get() = this in listOf(BATTLEFIELD, STACK, EXILE, ANTE)
}
