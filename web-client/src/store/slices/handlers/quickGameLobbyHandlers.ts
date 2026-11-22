/**
 * Server-message handlers for the Quick Game Lobby flow. Wires lobby state-snapshot updates
 * and lobby-closed notifications into the [quickGameLobbySlice].
 */
import type { MessageHandlers } from '@/network/messageHandlers.ts'
import { ErrorCode } from '@/types'
import { clearPendingLobby, takePendingLobbyApply } from '../pendingLobbyIntent'
import type { SetState, GetState } from './types'

type QuickGameLobbyHandlerKeys =
  | 'onQuickGameLobbyState'
  | 'onQuickGameLobbyClosed'
  | 'onMatchmakingStatus'
  | 'onMatchFound'
  | 'onMatchmakingQueues'

export function createQuickGameLobbyHandlers(
  set: SetState,
  get: GetState
): Pick<MessageHandlers, QuickGameLobbyHandlerKeys> {
  return {
    onQuickGameLobbyState: (msg) => {
      // The queue found a match while this player was warming up against the AI: the server has
      // already ended that game quietly, so leave it here too and show the new lobby.
      const s = get()
      if (msg.matchmade && (s.gameState || s.gameOverState || s.mulliganState || s.waitingForOpponentMulligan)) {
        s.returnToMenu()
      }
      set({ quickGameLobbyState: msg })
      // A recipe's leftovers, flushed by the first broadcast for the lobby it created. Everything a
      // quick lobby has rides its create message except the AI's deck and your Random-deck sets,
      // which are keyed on the lobby existing — so these have to wait. See `pendingLobbyIntent.ts`.
      const pending = takePendingLobbyApply()
      if (pending?.randomSetCodes) get().setQuickGameLobbySetCode(pending.randomSetCodes)
      if (pending?.aiDeck) get().setQuickGameAiDeck(pending.aiDeck)
    },

    onQuickGameLobbyClosed: (msg) => {
      // Anything a recipe queued for this lobby is dead with it — a create that never landed must
      // not reconfigure whichever lobby happens to come next.
      clearPendingLobby()
      // The server broadcasts the closure to everyone still listed in the lobby — which includes
      // the host who just closed it by leaving. `leaveQuickGameLobby` clears the slice first, so a
      // null slice here means this notice is about a lobby we already walked away from, and
      // reporting it would be a red "Host left the lobby" banner over our own deliberate action.
      // That is most visible when switching a lobby onto the other kind (`useLobbyCommands`
      // leaves and immediately recreates), where the new lobby is already on screen.
      const alreadyLeft = get().quickGameLobbyState === null
      set({ quickGameLobbyState: null })
      if (alreadyLeft) return
      // Otherwise surface the reason via the existing global error channel, so the home screen
      // renders it like any other connection-time error message.
      get().setError({ message: msg.reason, code: ErrorCode.INVALID_ACTION, timestamp: Date.now() })
    },

    // Any status supersedes an open prompt: searching again means it fell through, idle means it
    // was declined, expired, or confirmed (the lobby state follows a confirmation).
    onMatchmakingStatus: (msg) => {
      set({ matchmaking: msg, matchOffer: null })
    },

    onMatchFound: (msg) => {
      const current = get().matchOffer
      // An "accepted" echo of the prompt already on screen keeps its original countdown.
      const receivedAt = current?.matchId === msg.matchId ? current.receivedAt : Date.now()
      set({ matchOffer: { ...msg, receivedAt } })
    },

    onMatchmakingQueues: (msg) => {
      set({ matchmakingQueues: msg.queues })
    },
  }
}
