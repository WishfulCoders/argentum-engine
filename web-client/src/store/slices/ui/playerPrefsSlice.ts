/**
 * Player-preferences sub-slice — client-only presentation choices that never reach the server:
 * the player's own arrangement of their hand, which stack announcements to toast, and how large
 * the battlefield draws its cards.
 *
 * The hand order is a display permutation over the server's hand zone. The server's order stays
 * authoritative for everything else; cards the permutation doesn't know yet (a fresh draw) are
 * appended on the right, and ids that left the hand are ignored, so a stale order is harmless.
 */
import type { SliceCreator, EntityId } from '../types'

/** Which new stack objects the announcement feed toasts. */
export type AnnouncementMode = 'off' | 'opponent' | 'all'

const ANNOUNCEMENT_KEY = 'argentum-announcements'

function loadAnnouncementMode(): AnnouncementMode {
  try {
    const v = localStorage.getItem(ANNOUNCEMENT_KEY)
    return v === 'off' || v === 'all' || v === 'opponent' ? v : 'opponent'
  } catch {
    return 'opponent'
  }
}

/**
 * Card-size choices offered in the gameplay settings. 1 is the layout's own sizing; anything larger
 * also packs the rows tightly (see `LayoutEnv.preferSize`), which is where most of the gain is.
 */
export const CARD_SCALES = [0.85, 1, 1.15, 1.3, 1.5] as const

/** L: on a 1080p window the layout's own sizing left battlefield cards at 84 px with half the height spent on spacing. */
export const DEFAULT_CARD_SCALE = 1.15

const CARD_SCALE_KEY = 'argentum-card-scale'
const COMPACT_LANDS_KEY = 'argentum-compact-lands'

function loadCompactLands(): boolean {
  try {
    return localStorage.getItem(COMPACT_LANDS_KEY) === 'true'
  } catch {
    return false
  }
}

function loadCardScale(): number {
  try {
    const v = Number(localStorage.getItem(CARD_SCALE_KEY))
    return (CARD_SCALES as readonly number[]).includes(v) ? v : DEFAULT_CARD_SCALE
  } catch {
    return DEFAULT_CARD_SCALE
  }
}

export interface PlayerPrefsSliceState {
  /** The player's chosen left-to-right order of their hand (card ids). Empty = server order. */
  handOrder: readonly EntityId[]
  announcementMode: AnnouncementMode
  /** Multiplier on the battlefield's card-size ceiling (see `useResponsive`); one of [CARD_SCALES]. */
  cardScale: number
  /** Draw the lands row smaller so creatures get the height (see `COMPACT_BACK_ROW_SCALE`). */
  compactLands: boolean
}

export interface PlayerPrefsSliceActions {
  setHandOrder: (order: readonly EntityId[]) => void
  setAnnouncementMode: (mode: AnnouncementMode) => void
  setCardScale: (scale: number) => void
  setCompactLands: (on: boolean) => void
}

export type PlayerPrefsSlice = PlayerPrefsSliceState & PlayerPrefsSliceActions

export const createPlayerPrefsSlice: SliceCreator<PlayerPrefsSlice> = (set) => ({
  handOrder: [],
  announcementMode: loadAnnouncementMode(),
  cardScale: loadCardScale(),
  compactLands: loadCompactLands(),

  setHandOrder: (order) => set({ handOrder: order }),

  setAnnouncementMode: (mode) => {
    try {
      localStorage.setItem(ANNOUNCEMENT_KEY, mode)
    } catch {
      // Private window / blocked storage: the choice still holds for this session.
    }
    set({ announcementMode: mode })
  },

  setCardScale: (scale) => {
    try {
      localStorage.setItem(CARD_SCALE_KEY, String(scale))
    } catch {
      // Private window / blocked storage: the choice still holds for this session.
    }
    set({ cardScale: scale })
  },

  setCompactLands: (on) => {
    try {
      localStorage.setItem(COMPACT_LANDS_KEY, String(on))
    } catch {
      // Private window / blocked storage: the choice still holds for this session.
    }
    set({ compactLands: on })
  },
})

/**
 * [cards] in the player's chosen order: known ids first, in [order]; anything new after them in
 * the server's order. Returns the input array unchanged when the order says nothing about it.
 */
export function applyHandOrder<T extends { id: EntityId }>(cards: readonly T[], order: readonly EntityId[]): readonly T[] {
  if (order.length === 0 || cards.length < 2) return cards
  const pos = new Map<EntityId, number>()
  order.forEach((id, i) => pos.set(id, i))
  const known = cards.filter((c) => pos.has(c.id)).sort((a, b) => pos.get(a.id)! - pos.get(b.id)!)
  if (known.length === 0) return cards
  const fresh = cards.filter((c) => !pos.has(c.id))
  return [...known, ...fresh]
}
