/**
 * Destination lanes for a two-way card split (scry, surveil, "put one into your hand and the rest
 * on the bottom", Fact-or-Fiction piles).
 *
 * The engine asks these as a [SelectCardsDecision] whose `selectedLabel` / `remainderLabel` name
 * where each half goes — scry is "Selected → Put on bottom / Not selected → Put on top". The lane
 * UI shows the two destinations as drop zones instead, so a card's position on screen is where it
 * will end up and nobody has to read which way "selected" runs.
 *
 * A lane whose label puts cards into a library is ordered left to right, top-most first: the engine
 * follows the split with a [ReorderLibraryDecision] for those cards (for the top it asks even for
 * one card, to show it; for the bottom, when more than one goes there and the card lets you order
 * them). The order the player already arranged in the lane is remembered here and answers that
 * follow-up automatically, so a scry is one screen, not two.
 */
import type { EntityId, PendingDecision, ReorderLibraryDecision, SelectCardsDecision } from '@/types'

export type LaneKey = 'selected' | 'remainder'

export interface Lane {
  readonly key: LaneKey
  readonly label: string
  /** Cards here land on top of a library, in lane order (index 0 = top). */
  readonly isLibraryTop: boolean
  /** Cards here go into a library, top or bottom; the lane order is the order they land in. */
  readonly isLibrary: boolean
}

export interface LaneState {
  readonly selected: readonly EntityId[]
  readonly remainder: readonly EntityId[]
}

/** True when [label] sends cards to the bottom of a library ("Put on the bottom of your library"). */
export function isLibraryBottomLabel(label: string): boolean {
  return /\bbottom\b/i.test(label)
}

/** Most cards a lane screen lays out; a larger choice (a whole graveyard) keeps the grid picker. */
export const MAX_LANE_CARDS = 12

/** True when [label] sends cards to the top of a library ("Put on top", "Leave on top of your library"). */
export function isLibraryTopLabel(label: string): boolean {
  return /\btop\b/i.test(label) && !/\bbottom\b/i.test(label)
}

/**
 * Whether [decision] is a plain two-destination split the lane UI can carry. Anything with a
 * per-selection restriction (one per colour, a mana-value budget, ordered picks, conditional
 * minimums) stays on the grid picker, which knows how to enforce those.
 */
export function isLaneDecision(decision: SelectCardsDecision): boolean {
  if (!decision.selectedLabel || !decision.remainderLabel) return false
  if (decision.ordered) return false
  if (decision.onePerCardType || decision.onePerColor || decision.onePerBasicLandType || decision.onePerPower) return false
  if (decision.maxTotalManaValue != null || decision.minTotalManaValue != null || decision.maxTotalPower != null) return false
  if ((decision.conditionalMinimums ?? []).length > 0) return false
  const total = decision.options.length + (decision.nonSelectableOptions ?? []).length
  return total > 0 && total <= MAX_LANE_CARDS
}

/** The decision's two lanes, the library-top lane first (it reads as the top of the pile). */
export function lanesFor(decision: SelectCardsDecision): Lane[] {
  const lane = (key: LaneKey, label: string): Lane => ({
    key,
    label,
    isLibraryTop: isLibraryTopLabel(label),
    isLibrary: isLibraryTopLabel(label) || isLibraryBottomLabel(label),
  })
  const lanes: Lane[] = [
    lane('selected', decision.selectedLabel ?? 'Selected'),
    lane('remainder', decision.remainderLabel ?? 'Not selected'),
  ]
  return lanes.sort((a, b) => Number(b.isLibraryTop) - Number(a.isLibraryTop))
}

/** Every card starts where an empty selection would send it, in the order the engine listed them. */
export function initialLaneState(decision: SelectCardsDecision): LaneState {
  return { selected: [], remainder: [...decision.options, ...(decision.nonSelectableOptions ?? [])] }
}

/**
 * Move [cardId] into [toLane] at [index] (clamped; omitted = the end). Returns [state] unchanged
 * when the move would put a card the engine won't let you pick into the selected lane, or overfill
 * it past [maxSelected].
 */
export function moveCard(
  state: LaneState,
  cardId: EntityId,
  toLane: LaneKey,
  index: number | undefined,
  selectable: ReadonlySet<EntityId>,
  maxSelected: number,
): LaneState {
  const fromLane: LaneKey | null = state.selected.includes(cardId)
    ? 'selected'
    : state.remainder.includes(cardId) ? 'remainder' : null
  if (fromLane === null) return state
  if (toLane === 'selected' && !selectable.has(cardId)) return state
  if (toLane === 'selected' && fromLane !== 'selected' && state.selected.length >= maxSelected) return state

  const from = state[fromLane]
  const without = { ...state, [fromLane]: from.filter((id) => id !== cardId) } as LaneState
  const target = [...without[toLane]]
  let at = index ?? target.length
  // Dropping a card further along its own lane: the indices after it shifted left by one.
  if (fromLane === toLane && index !== undefined && from.indexOf(cardId) < index) at -= 1
  at = Math.max(0, Math.min(at, target.length))
  target.splice(at, 0, cardId)
  return { ...without, [toLane]: target }
}

// ---------------------------------------------------------------------------
// The planned library order, carried from the split to the reorder that follows it
// ---------------------------------------------------------------------------

interface PlannedOrder {
  /** Top-most first. */
  readonly order: readonly EntityId[]
  /** The reorder decision it answers, once one has matched; it answers no other. */
  boundTo: string | null
}

interface Plan {
  /** The split decision that produced it. */
  readonly fromDecisionId: string
  /** One order per library lane (a scry's top, an Anticipate's bottom). */
  readonly orders: PlannedOrder[]
}

let planned: Plan | null = null

/** Remember the orders the player gave the library lanes of split [fromDecisionId]. */
export function planLibraryOrder(fromDecisionId: string, ...orders: (readonly EntityId[])[]): void {
  const kept = orders.filter((o) => o.length > 0).map((o) => ({ order: [...o], boundTo: null }))
  planned = kept.length > 0 ? { fromDecisionId, orders: kept } : null
}

/**
 * The planned order when [decision] is the follow-up reorder of exactly the cards the player
 * arranged; null otherwise (a different set of cards is a different question and gets asked).
 */
export function plannedOrderFor(decision: ReorderLibraryDecision): readonly EntityId[] | null {
  if (!planned) return null
  const bound = planned.orders.find((p) => p.boundTo === decision.id)
  if (bound) return bound.order
  const asked = new Set(decision.cards)
  const match = planned.orders.find((p) =>
    p.boundTo === null && p.order.length === decision.cards.length && p.order.every((id) => asked.has(id)),
  )
  if (!match) return null
  match.boundTo = decision.id
  return match.order
}

/**
 * Called on every new pending decision: a plan survives only until the reorder it was made for.
 * Any other decision in between (a trigger, a replacement choice) means the flow went somewhere
 * the plan didn't foresee, so it is dropped rather than risk answering an unrelated reorder.
 */
export function notePendingDecision(decision: PendingDecision | null): void {
  if (!planned || !decision) return
  if (decision.id === planned.fromDecisionId) return
  if (decision.type === 'ReorderLibraryDecision' && plannedOrderFor(decision)) return
  planned = null
}

export function clearPlannedOrder(): void {
  planned = null
}
