package com.wingedsheep.sdk.scripting.values

/**
 * Marks an SDK node whose meaning depends on the *order* of cards in a graveyard (CR 404.1 lets a
 * graveyard's order matter only when a card says so): "the number of creature cards above this
 * card in your graveyard" (Nether Shadow).
 *
 * The engine keeps graveyard arrival order silently and asks an owner to order several cards
 * entering their graveyard at once only in a game where some card carries one of these nodes —
 * see [com.wingedsheep.sdk.model.CardDefinition.readsGraveyardOrder]. A new positional reader
 * implements this interface and turns the prompt on without touching the engine.
 */
interface ReadsGraveyardOrder
