import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

const api = vi.hoisted(() => ({
  fetchAppConfig: vi.fn<() => Promise<{ accountsEnabled: boolean } | null>>(),
  fetchMe: vi.fn<() => Promise<unknown>>(),
}))
vi.mock('@/api/account', async (importOriginal) => ({
  ...(await importOriginal<typeof import('@/api/account')>()),
  fetchAppConfig: api.fetchAppConfig,
  fetchMe: api.fetchMe,
}))

import type { AccountUser } from '@/api/account'
import { useAuthStore } from './authStore'

const user = { id: 'u1', displayName: 'Ada' } as AccountUser

describe('authStore session resolution', () => {
  beforeEach(() => {
    vi.useFakeTimers()
    const storage = new Map<string, string>()
    vi.stubGlobal('localStorage', {
      getItem: (k: string) => storage.get(k) ?? null,
      setItem: (k: string, v: string) => storage.set(k, v),
      removeItem: (k: string) => storage.delete(k),
    })
    api.fetchAppConfig.mockReset()
    api.fetchMe.mockReset()
    useAuthStore.getState().logout()
    useAuthStore.setState({ status: 'idle', accountsEnabled: false })
  })
  afterEach(() => {
    vi.useRealTimers()
    vi.unstubAllGlobals()
  })

  it('settles as anonymous while the server is down, then signs in once it answers', async () => {
    api.fetchAppConfig.mockResolvedValue(null)
    await useAuthStore.getState().init()
    expect(useAuthStore.getState().status).toBe('anonymous')

    api.fetchAppConfig.mockResolvedValue({ accountsEnabled: true })
    api.fetchMe.mockResolvedValue(user)
    await vi.advanceTimersByTimeAsync(2_000)

    expect(useAuthStore.getState()).toMatchObject({ status: 'authenticated', accountsEnabled: true, user })
  })

  it('retries straight away when the connection comes back', async () => {
    api.fetchAppConfig.mockResolvedValue({ accountsEnabled: true })
    api.fetchMe.mockRejectedValue(new Error('502'))
    await useAuthStore.getState().init()
    expect(useAuthStore.getState().status).toBe('anonymous')

    api.fetchMe.mockResolvedValue(user)
    useAuthStore.getState().retryIfUnresolved()
    await vi.advanceTimersByTimeAsync(0)

    expect(useAuthStore.getState().status).toBe('authenticated')
  })

  it('stops asking once the server has answered', async () => {
    api.fetchAppConfig.mockResolvedValue({ accountsEnabled: true })
    api.fetchMe.mockResolvedValue(null)
    await useAuthStore.getState().init()
    useAuthStore.getState().retryIfUnresolved()
    await vi.advanceTimersByTimeAsync(60_000)

    expect(api.fetchAppConfig).toHaveBeenCalledTimes(1)
    expect(useAuthStore.getState().status).toBe('anonymous')
  })

  it('does not let a pending retry overwrite a fresh sign-in', async () => {
    api.fetchAppConfig.mockResolvedValue(null)
    await useAuthStore.getState().init()
    useAuthStore.getState().setSession({ authToken: 't', user } as Parameters<ReturnType<typeof useAuthStore.getState>['setSession']>[0])
    await vi.advanceTimersByTimeAsync(60_000)

    expect(api.fetchAppConfig).toHaveBeenCalledTimes(1)
    expect(useAuthStore.getState().status).toBe('authenticated')
  })
})
