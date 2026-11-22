/**
 * The "server is restarting" window: from the moment the server says it is going down (or closes
 * the socket with 1012) until it answers our reconnect. While it is open the client keeps the
 * player's screen, shows the update notice, and retries on a short flat interval.
 *
 * The window is mirrored to sessionStorage so a reload mid-update — the frontend container is
 * replaced in the same deploy — still explains why nothing connects, instead of a bare spinner.
 */
import { getWebSocket } from './shared'

export interface ServerRestartState {
  /** Epoch ms the restart was first noticed. */
  readonly startedAt: number
  /** Downtime the server quoted, in seconds. */
  readonly expectedSeconds: number
}

/** Retry interval while the server is restarting: back within a few seconds of it being up. */
export const RESTART_RETRY_MS = 3_000

/** Quoted when the restart was inferred from a close code alone. Matches the server's default. */
export const DEFAULT_RESTART_SECONDS = 60

const STORAGE_KEY = 'argentum-server-restart'

/** A stored window older than this is a leftover, not an update still in progress. */
const STALE_AFTER_MS = 15 * 60_000

export function loadServerRestart(): ServerRestartState | null {
  try {
    const raw = sessionStorage.getItem(STORAGE_KEY)
    if (!raw) return null
    const parsed = JSON.parse(raw) as Partial<ServerRestartState>
    if (typeof parsed.startedAt !== 'number' || typeof parsed.expectedSeconds !== 'number') return null
    if (Date.now() - parsed.startedAt > STALE_AFTER_MS) return null
    return { startedAt: parsed.startedAt, expectedSeconds: parsed.expectedSeconds }
  } catch {
    return null
  }
}

function saveServerRestart(state: ServerRestartState | null): void {
  try {
    if (state) sessionStorage.setItem(STORAGE_KEY, JSON.stringify(state))
    else sessionStorage.removeItem(STORAGE_KEY)
  } catch {
    // Storage unavailable (private mode, blocked) — the in-memory window still works.
  }
}

interface RestartStore {
  serverRestart: ServerRestartState | null
  serverBackAt: number | null
}
type Set = (partial: Partial<RestartStore>) => void
type Get = () => RestartStore

/** Open the restart window (idempotent: a later signal keeps the original start time). */
export function beginServerRestart(set: Set, get: Get, expectedSeconds: number): void {
  const current = get().serverRestart
  const next = current ?? { startedAt: Date.now(), expectedSeconds }
  if (!current) {
    set({ serverRestart: next, serverBackAt: null })
    saveServerRestart(next)
  }
  getWebSocket()?.setFixedRetryInterval(RESTART_RETRY_MS)
}

/** Close the window once the server has answered our connect; flags the "back online" moment. */
export function endServerRestart(set: Set, get: Get): void {
  if (!get().serverRestart) return
  set({ serverRestart: null, serverBackAt: Date.now() })
  saveServerRestart(null)
  getWebSocket()?.setFixedRetryInterval(null)
}
