/**
 * The handoff between the playtest page and the board.
 *
 * The page picks a matchup; the board is the normal App, which owns the WebSocket and the store.
 * Rather than give the page its own connection, it parks the chosen matchup here and navigates to
 * `/?playtest=<id>`; App fires it once it is connected, exactly as it already does for the
 * `/?spectate=<id>` deep-link.
 *
 * sessionStorage and not the URL because a matchup carries two 40-card decklists.
 */

const KEY = 'argentum-playtest-pending'

/** A matchup row, as `scripts/playtest/build_matchups.py` writes it. */
export interface PlaytestDeck {
  readonly id: string
  readonly colors: string
  readonly rank: string | null
  readonly games: number | null
  readonly wins: number | null
  readonly winRate: number | null
  readonly instants: number
  readonly deckList: Record<string, number>
}

export interface PlaytestMatchup {
  readonly id: string
  /** Which curation bucket chose it: `instants`, `arena`, `winrate`, `spread`. */
  readonly bucket: string
  /** Why this pairing is worth playing, in words, from the curation script. */
  readonly reason: string
  readonly set: string
  readonly you: PlaytestDeck
  readonly opponent: PlaytestDeck
}

export function parkMatchup(matchup: PlaytestMatchup): void {
  try {
    sessionStorage.setItem(KEY, JSON.stringify(matchup))
  } catch {
    // Private browsing, or storage disabled. The board falls back to a normal game rather than
    // failing the navigation, and the page below says so.
  }
}

/** Read and clear the parked matchup. Clearing makes a reload start a normal game, not a rematch. */
export function takeMatchup(): PlaytestMatchup | null {
  try {
    const raw = sessionStorage.getItem(KEY)
    if (!raw) return null
    sessionStorage.removeItem(KEY)
    return JSON.parse(raw) as PlaytestMatchup
  } catch {
    return null
  }
}
