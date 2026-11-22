/**
 * The lobby's settings, grouped under the words the wizard already taught.
 *
 * A host configuring a booster draft with Commander rules and two sets used to face **twenty stacked
 * rows, around 1500px, some sixty interactive elements** in a 756px column — with the player list and
 * the Start button below all of it. Every row was individually justified; the total was not.
 *
 * ## Why these five groups and not some other five
 *
 * Four of them are the axes (`axes.ts`), which the player has just answered in the wizard and which
 * `LobbyAxisSummary` already shows as chips at the top of this very screen. Introducing a second
 * vocabulary here — "Pool", "Timing", "Advanced" — would be the exact mistake this project spent
 * Phase 2 undoing, when "Format" meant two things and "Mode" meant three.
 *
 * The grouping is also what the code already believes: `TournamentLobbySettings` gates the Commander
 * deckbuild knobs on `axes.rules === 'COMMANDER'`, the matchup count on `axes.event === 'ROUND_ROBIN'`,
 * and the attack rule on a Free-for-All *table*. Each of those rows is already a refinement of one
 * axis; the group is just where it was always pointing.
 *
 * The fifth, **This lobby**, is the honest remainder: visibility, AI assistance and the AI seat's
 * deck are facts about *this room*, not about the game being played, and they are the rows a saved
 * setup is least likely to be about.
 *
 * ## Grouped, never hidden
 *
 * Every relevant row stays visible. The five headers turn the long form into answers that can be
 * scanned without making discoverability depend on opening the right section.
 *
 * The project's standing rule is *disabled-with-reason over hiding* — every greyed option is a
 * tracked server gap, and rendering it teaches the shape of the system. Nothing here bends it: each
 * group header carries its axis's full button strip, every value keeps its reason and its `⇄`, and
 * the refinements below remain on screen.
 *
 * Two guards, still:
 *
 * - a group holding the reason Start is disabled shows a `!`; the mapping reads the same
 *   `startBlockReason` that writes the button's tooltip;
 * - a group is never a scroll container. `.lobbyOverlay` remains the single one, which is the lesson
 *   written into `GameUI.module.css` after an earlier attempt at a scrolling settings panel produced
 *   three competing scrollbars.
 */
import { cardsKindTopicId, eventTopicId, rulesTopicId, tableTopicId } from './axes'
import type { UnifiedLobbyView } from './lobbyViewModel'

export type GroupId = 'CARDS' | 'RULES' | 'TABLE' | 'EVENT' | 'LOBBY'

/** Reading order, and the order they render in: what deck → under what rules → at what table →
 *  over how many games → and how this particular room is set up. */
export const GROUP_IDS: readonly GroupId[] = ['CARDS', 'RULES', 'TABLE', 'EVENT', 'LOBBY']

export function groupLabel(id: GroupId): string {
  switch (id) {
    case 'CARDS': return 'Cards'
    case 'RULES': return 'Rules'
    case 'TABLE': return 'Table'
    case 'EVENT': return 'Event'
    case 'LOBBY': return 'This lobby'
  }
}

/** The help topic bound to the *value in effect*, so a group's `?` explains what is selected. */
export function groupTopicId(id: GroupId, view: UnifiedLobbyView): string | null {
  switch (id) {
    case 'CARDS': return cardsKindTopicId(view.axes.cards.kind)
    case 'RULES': return rulesTopicId(view.axes.rules)
    case 'TABLE': return tableTopicId(view.axes.table)
    case 'EVENT': return eventTopicId(view.axes.event)
    case 'LOBBY': return null
  }
}

/**
 * Which group holds the reason the host can't press Start, if any.
 *
 * Derived from `view.blockReason`, which is the same value that becomes the Start button's tooltip —
 * so the group that opens itself and the sentence the host reads are guaranteed to be about the same
 * thing. A second, hand-maintained "which row fixes this" table is exactly what would rot.
 *
 * Null means there is nothing in the settings to open: "Need at least 2 players" and "all connected
 * players must submit a deck" are answered by the player list, not by a knob.
 */
export function blockingGroupFor(view: UnifiedLobbyView): GroupId | null {
  return view.blockGroup
}
