/**
 * Player-preferences sub-slice — client-only presentation choices that never reach the server:
 * the player's own arrangement of their hand, and which stack announcements to toast.
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

export interface PlayerPrefsSliceState {
  /** The player's chosen left-to-right order of their hand (card ids). Empty = server order. */
  handOrder: readonly EntityId[]
  announcementMode: AnnouncementMode
}

export interface PlayerPrefsSliceActions {
  setHandOrder: (order: readonly EntityId[]) => void
  setAnnouncementMode: (mode: AnnouncementMode) => void
}

export type PlayerPrefsSlice = PlayerPrefsSliceState & PlayerPrefsSliceActions

export const createPlayerPrefsSlice: SliceCreator<PlayerPrefsSlice> = (set) => ({
  handOrder: [],
  announcementMode: loadAnnouncementMode(),

  setHandOrder: (order) => set({ handOrder: order }),

  setAnnouncementMode: (mode) => {
    try {
      localStorage.setItem(ANNOUNCEMENT_KEY, mode)
    } catch {
      // Private window / blocked storage: the choice still holds for this session.
    }
    set({ announcementMode: mode })
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
