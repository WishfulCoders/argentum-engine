import type { AnteResult, AnteScope, AnteTransfer } from '@/types'

/**
 * How a finished game's ante settlement (CR 407) reads from one seat — pure, so the game-over
 * overlay and the spectator banner tell the same story and it can be tested without a DOM.
 *
 * The server decides every transfer; this only sorts them by whether the viewer gained or lost
 * the card. A viewer outside the transfers (a spectator, the third seat of a pod) gets them all as
 * neutral "X won Y from Z" lines.
 */
export interface AnteResultView {
  /** Cards the viewer gained. Empty for a spectator. */
  readonly won: readonly AnteTransfer[]
  /** Cards the viewer gave up. Empty for a spectator. */
  readonly lost: readonly AnteTransfer[]
  /** Transfers the viewer took no part in — everything, for a spectator. */
  readonly others: readonly AnteTransfer[]
  /** What the transfers mean beyond this game; null when nothing changed hands. */
  readonly scopeLine: string | null
  /** Shown instead of any card when nothing changed hands. */
  readonly emptyLine: string | null
  /** "Your deck gained a Swamp to stay at 40 cards." — one per refilled loss, viewer-relative. */
  readonly refillLines: readonly string[]
}

/** What the settlement means beyond this game, said to the viewer when they won or lost a card. */
export function anteScopeLine(scope: AnteScope, side: 'won' | 'lost' | 'other' = 'other'): string {
  if (scope === 'EVENT') {
    switch (side) {
      case 'won': return 'They join your pool for the rest of the tournament.'
      case 'lost': return 'They leave your pool for the rest of the tournament.'
      case 'other': return 'These cards move to the winner’s pool for the rest of the tournament.'
    }
  }
  return side === 'won' ? 'Added to the spoils on your profile.' : 'Added to the winner’s spoils on their profile.'
}

export function viewAnteResult(ante: AnteResult, viewerId: string | null): AnteResultView {
  const won: AnteTransfer[] = []
  const lost: AnteTransfer[] = []
  const others: AnteTransfer[] = []
  for (const t of ante.transfers) {
    // A card moving between a player and themselves isn't a transfer worth showing.
    if (t.fromPlayerId === t.toPlayerId) continue
    if (viewerId !== null && t.toPlayerId === viewerId) won.push(t)
    else if (viewerId !== null && t.fromPlayerId === viewerId) lost.push(t)
    else others.push(t)
  }
  const any = won.length + lost.length + others.length > 0
  return {
    won,
    lost,
    others,
    scopeLine: any
      ? anteScopeLine(ante.scope, won.length > 0 && lost.length === 0 ? 'won' : lost.length > 0 && won.length === 0 ? 'lost' : 'other')
      : null,
    emptyLine: any ? null : 'The ante returns to its owners.',
    refillLines: [...lost, ...others].flatMap((t) =>
      t.refilledWith
        ? [`${t.fromPlayerId === viewerId ? 'Your' : `${t.fromPlayerName}’s`} deck gained ${article(t.refilledWith)} ${t.refilledWith} to stay at 40 cards.`]
        : [],
    ),
  }
}

/** "a Swamp", "an Island". */
function article(word: string): string {
  return /^[aeiou]/i.test(word) ? 'an' : 'a'
}

/**
 * The one-line caption under a card: who it came from or went to. A card won from the ante is
 * "won"; one an ante card's effect moved (Darkpact, Bronze Tablet) was "taken".
 */
export function anteTransferCaption(t: AnteTransfer, side: 'won' | 'lost' | 'other'): string {
  const taken = t.cause === 'OWNERSHIP'
  switch (side) {
    case 'won': return `${taken ? 'taken' : 'won'} from ${t.fromPlayerName}`
    case 'lost': return `${taken ? 'taken by' : 'lost to'} ${t.toPlayerName}`
    case 'other': return `${t.toPlayerName} ${taken ? 'took' : 'won'} it from ${t.fromPlayerName}`
  }
}
