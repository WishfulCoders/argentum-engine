/**
 * Table talk: the preset emotes players send during a game, and the result screen's social actions
 * (rematch, add friend, block) after a human 1v1. Standalone, like the friends store — none of it is
 * game state, and keeping it out of the game store means an emote doesn't re-render the board.
 *
 * The server owns everything that matters here: it rate-limits emotes, drops the ones from players
 * you blocked, and sends the whole post-game state on every change. What lives only on this side is
 * presentation — which bubble is showing — and muting, which is a per-game "not now" rather than a
 * block.
 */
import { create } from 'zustand'
import type { Emote, EntityId, PostGameMessage } from '@/types'
import { getWebSocket } from './slices/shared'

/** How long a bubble stays up. */
export const EMOTE_BUBBLE_MS = 3200
/** The client's own spacing between sends — inside the server's limit, so it never trips. */
export const EMOTE_COOLDOWN_MS = 2500

export interface EmoteBubble {
  readonly emote: Emote
  /** Distinguishes two identical emotes in a row, so the second one replays the animation. */
  readonly id: number
}

interface TableTalkState {
  /** The emote currently showing over each seat. */
  bubbles: Readonly<Record<EntityId, EmoteBubble>>
  /** Seats whose emotes you've muted for this game. */
  muted: Readonly<Record<EntityId, true>>
  /** When you last sent one; drives the picker's cooldown ring. */
  lastSentAt: number
  postGame: PostGameMessage | null

  sendEmote: (emote: Emote) => void
  receiveEmote: (playerId: EntityId, emote: Emote) => void
  clearBubble: (playerId: EntityId, id: number) => void
  toggleMute: (playerId: EntityId) => void

  setPostGame: (msg: PostGameMessage) => void
  requestRematch: (want: boolean) => void
  addFriend: () => void
  block: (block: boolean) => void
  /** Leaving the result screen: tell the server, so the opponent's rematch button says so. */
  leavePostGame: () => void
  /** A new game: bubbles and mutes belong to the one that ended. */
  resetForNewGame: () => void
}

let nextBubbleId = 1
const bubbleTimers = new Map<EntityId, ReturnType<typeof setTimeout>>()

export const useTableTalkStore = create<TableTalkState>((set, get) => ({
  bubbles: {},
  muted: {},
  lastSentAt: 0,
  postGame: null,

  sendEmote: (emote) => {
    const now = Date.now()
    if (now - get().lastSentAt < EMOTE_COOLDOWN_MS) return
    set({ lastSentAt: now })
    getWebSocket()?.send({ type: 'sendEmote', emote })
  },

  receiveEmote: (playerId, emote) => {
    if (get().muted[playerId]) return
    const id = nextBubbleId++
    set((s) => ({ bubbles: { ...s.bubbles, [playerId]: { emote, id } } }))
    const previous = bubbleTimers.get(playerId)
    if (previous) clearTimeout(previous)
    bubbleTimers.set(playerId, setTimeout(() => get().clearBubble(playerId, id), EMOTE_BUBBLE_MS))
  },

  clearBubble: (playerId, id) => {
    if (get().bubbles[playerId]?.id !== id) return
    bubbleTimers.delete(playerId)
    set((s) => {
      const { [playerId]: _gone, ...rest } = s.bubbles
      return { bubbles: rest }
    })
  },

  toggleMute: (playerId) => {
    set((s) => {
      if (s.muted[playerId]) {
        const { [playerId]: _unmuted, ...rest } = s.muted
        return { muted: rest }
      }
      const { [playerId]: _hidden, ...bubbles } = s.bubbles
      return { muted: { ...s.muted, [playerId]: true }, bubbles }
    })
  },

  setPostGame: (msg) => set({ postGame: msg }),

  requestRematch: (want) => {
    const pg = get().postGame
    if (!pg) return
    getWebSocket()?.send({ type: 'postGameRematch', gameId: pg.gameId, want })
  },

  addFriend: () => {
    const pg = get().postGame
    if (!pg) return
    getWebSocket()?.send({ type: 'postGameAddFriend', gameId: pg.gameId })
  },

  block: (block) => {
    const pg = get().postGame
    if (!pg) return
    getWebSocket()?.send({ type: 'postGameBlock', gameId: pg.gameId, block })
  },

  leavePostGame: () => {
    const pg = get().postGame
    if (!pg) return
    getWebSocket()?.send({ type: 'postGameLeave', gameId: pg.gameId })
    set({ postGame: null })
  },

  resetForNewGame: () => {
    bubbleTimers.forEach(clearTimeout)
    bubbleTimers.clear()
    set({ bubbles: {}, muted: {}, postGame: null })
  },
}))
