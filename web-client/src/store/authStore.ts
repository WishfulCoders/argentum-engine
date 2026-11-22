/**
 * Standalone account/auth store (separate from the game store — accounts are orthogonal to a live
 * game). Holds the signed-in user and drives the magic-link login flow. The auth token itself lives
 * in localStorage (see api/account); this store mirrors the derived user for the UI.
 *
 * Resolving the session needs the server. When it can't answer — the page loaded while it was down or
 * restarting — the store settles as anonymous so the entry screens aren't held back, then keeps
 * re-asking (on a backoff, and straight away when the game socket reconnects) until it gets a real
 * answer. Otherwise the account menu stays gone until a manual refresh, though the token is still there.
 */
import { create } from 'zustand'
import {
  type AccountUser,
  type LoginResponse,
  clearAuthToken,
  fetchAppConfig,
  fetchMe,
  setAuthToken,
  updateAvatar,
  updateProfile,
} from '@/api/account'

export type AuthStatus = 'idle' | 'loading' | 'authenticated' | 'anonymous'

interface AuthState {
  user: AccountUser | null
  status: AuthStatus
  /**
   * Whether the server runs the accounts subsystem. Defaults to false (hide all account UI) until
   * {@link init} learns otherwise, so a server with accounts disabled never shows a sign-in form
   * that can only fail.
   */
  accountsEnabled: boolean
  /** Resolve server config + the current session from a stored token (call once on app start). */
  init: () => Promise<void>
  /**
   * Re-resolve now if the last attempt couldn't reach the server; a no-op once it has answered. Call
   * when a connection to the server comes back.
   */
  retryIfUnresolved: () => void
  /** Apply a fresh login (stores the token and the user). */
  setSession: (login: LoginResponse) => void
  /** Change the signed-in user's display name (persists to the server, then updates the store). */
  updateDisplayName: (displayName: string) => Promise<void>
  /** Pick a preset avatar (or null for the initial); persists to the server, then updates the store. */
  updateAvatar: (avatar: string | null) => Promise<void>
  /** Patch fields of the signed-in user in the store (e.g. after toggling presence visibility). */
  patchUser: (patch: Partial<AccountUser>) => void
  /** Sign out: drop the token and the user. */
  logout: () => void
}

const RETRY_BASE_MS = 2_000
const RETRY_MAX_MS = 30_000

/** Set while the last resolve couldn't reach the server, so a retry is owed. */
let unresolved = false
let retryTimer: ReturnType<typeof setTimeout> | null = null
let retryDelayMs = RETRY_BASE_MS
let resolving = false
/** Bumped by sign-in and sign-out, so a resolve already in flight can't overwrite their result. */
let sessionGeneration = 0

function cancelRetry() {
  unresolved = false
  retryDelayMs = RETRY_BASE_MS
  if (retryTimer) clearTimeout(retryTimer)
  retryTimer = null
}

export const useAuthStore = create<AuthState>((set, get) => {
  const resolveSession = async () => {
    if (resolving) return
    resolving = true
    const generation = sessionGeneration
    try {
      const config = await fetchAppConfig()
      // A network failure must still settle the status: the entry screens hold their name entry back
      // until auth resolves, and a stuck 'loading' would leave a guest with no way in.
      const user = config?.accountsEnabled ? await fetchMe().catch(() => undefined) : null
      if (generation !== sessionGeneration) return
      if (config === null || user === undefined) {
        set({ user: null, status: 'anonymous', accountsEnabled: config?.accountsEnabled ?? get().accountsEnabled })
        scheduleRetry()
        return
      }
      cancelRetry()
      set(
        user
          ? { user, status: 'authenticated', accountsEnabled: true }
          : { user: null, status: 'anonymous', accountsEnabled: config.accountsEnabled },
      )
    } finally {
      resolving = false
    }
  }

  const scheduleRetry = () => {
    unresolved = true
    if (retryTimer) clearTimeout(retryTimer)
    retryTimer = setTimeout(() => {
      retryTimer = null
      void resolveSession()
    }, retryDelayMs)
    retryDelayMs = Math.min(retryDelayMs * 2, RETRY_MAX_MS)
  }

  return {
    user: null,
    status: 'idle',
    accountsEnabled: false,

    init: async () => {
      set({ status: 'loading' })
      await resolveSession()
    },

    retryIfUnresolved: () => {
      if (!unresolved) return
      retryDelayMs = RETRY_BASE_MS
      if (retryTimer) clearTimeout(retryTimer)
      retryTimer = null
      void resolveSession()
    },

    setSession: (login) => {
      sessionGeneration++
      cancelRetry()
      setAuthToken(login.authToken)
      // A successful login means the server runs the accounts subsystem, so flip accountsEnabled on
      // here too. Otherwise the account UI (AuthWidget, profile) stays hidden until a manual refresh
      // re-runs init() — the magic-link flow lands on '/' without ever calling init().
      set({ user: login.user, status: 'authenticated', accountsEnabled: true })
    },

    updateDisplayName: async (displayName) => {
      const user = await updateProfile(displayName)
      set({ user })
    },

    updateAvatar: async (avatar) => {
      const user = await updateAvatar(avatar)
      set({ user })
    },

    patchUser: (patch) => set((s) => (s.user ? { user: { ...s.user, ...patch } } : {})),

    logout: () => {
      sessionGeneration++
      cancelRetry()
      clearAuthToken()
      set({ user: null, status: 'anonymous' })
    },
  }
})
