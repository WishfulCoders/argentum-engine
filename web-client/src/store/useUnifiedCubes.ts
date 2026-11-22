/**
 * One cube library that merges the user's account (cloud) cubes with their browser-only (localStorage)
 * cubes, tagging each with where it lives — the cube twin of {@link ./useUnifiedDecks useUnifiedDecks},
 * deliberately the same shape so the cube picker behaves like the deck picker.
 *
 * When signed in, cloud cubes are fetched in full (one `?full` request) so they carry their card lists
 * and can be used or edited without a second round-trip. A local cube whose name matches a cloud cube
 * is treated as the same cube (the cloud copy wins) so it isn't shown twice. Saves, renames and
 * deletes route to the right backing store automatically, which is what lets a guest build a cube and
 * keep it after signing in.
 *
 * Share links belong to cloud cubes only — a link has to resolve on someone else's machine — so
 * {@link share}/{@link unshare} take a cloud cube and patch its `shareCode` in place.
 */
import { useCallback, useEffect, useMemo, useState } from 'react'
import {
  type CubeDetail,
  type SharedCube,
  deleteCube as apiDeleteCube,
  listCubeDetails,
  saveCube as apiSaveCube,
  shareCube as apiShareCube,
  unshareCube as apiUnshareCube,
  updateCube as apiUpdateCube,
} from '@/api/account'
import { type CubeEntry, type SavedCube, useCubeLibrary } from '@/store/cubeLibrary'
import { useAuthStore } from '@/store/authStore'

export interface UnifiedCube extends SavedCube {
  /** True = backed up in the account (cloud); false = browser-only (localStorage). */
  online: boolean
  /** Server id, present only for cloud cubes (used to route updates/deletes). */
  cloudId?: number
  /** The cube's public link code, when its owner has shared it. Cloud cubes only. */
  shareCode?: string | null
}

/** Copy an entry, keeping its rarity only when it has one (so stored JSON stays sparse). */
function copyEntry(entry: CubeEntry): CubeEntry {
  return entry.rarity
    ? { name: entry.name, count: entry.count, rarity: entry.rarity }
    : { name: entry.name, count: entry.count }
}

/** Strip the storage-location fields, leaving the wire/cloud shape. */
export function cubeToShared(cube: SharedCube | SavedCube): SharedCube {
  return {
    name: cube.name,
    ...(cube.description ? { description: cube.description } : {}),
    cards: cube.cards.map(copyEntry),
    basicLandSetCode: cube.basicLandSetCode,
    packSize: cube.packSize,
    ...(cube.boosterStyle && cube.boosterStyle !== 'SHUFFLED' ? { boosterStyle: cube.boosterStyle } : {}),
  }
}

function detailToUnified(detail: CubeDetail): UnifiedCube {
  return {
    ...cubeToShared(detail.cube),
    cards: detail.cube.cards.map(copyEntry),
    id: `cloud:${detail.id}`,
    cloudId: detail.id,
    online: true,
    shareCode: detail.shareCode ?? null,
    updatedAt: Date.parse(detail.updatedAt) || 0,
  }
}

export function useUnifiedCubes() {
  const authStatus = useAuthStore((s) => s.status)
  const isLoggedIn = authStatus === 'authenticated'
  const localCubes = useCubeLibrary((s) => s.cubes)
  const hydrate = useCubeLibrary((s) => s.hydrate)
  const hydrated = useCubeLibrary((s) => s.hydrated)
  const saveLocal = useCubeLibrary((s) => s.saveCube)
  const deleteLocal = useCubeLibrary((s) => s.deleteCube)
  const renameLocal = useCubeLibrary((s) => s.renameCube)

  const [cloud, setCloud] = useState<CubeDetail[]>([])
  const [loading, setLoading] = useState(false)
  /**
   * Whose cloud list `cloud` holds: the signed-in account's once fetched, `anonymous` when there is
   * no account to fetch from. Keyed rather than a boolean so signing in can't read as "loaded" on
   * the render before the account's fetch has even started.
   */
  const [loadedFor, setLoadedFor] = useState<'anonymous' | 'account' | null>(null)

  useEffect(() => {
    if (!hydrated) hydrate()
  }, [hydrated, hydrate])

  const reload = useCallback(() => {
    if (!isLoggedIn) {
      setCloud([])
      setLoadedFor('anonymous')
      return
    }
    setLoading(true)
    void listCubeDetails()
      .then(setCloud)
      .catch(() => setCloud([]))
      .finally(() => {
        setLoading(false)
        setLoadedFor('account')
      })
  }, [isLoggedIn])

  useEffect(() => {
    reload()
  }, [reload])

  const cubes: UnifiedCube[] = useMemo(() => {
    const cloudCubes = cloud.map(detailToUnified)
    const cloudNames = new Set(cloudCubes.map((c) => c.name.toLowerCase()))
    const locals: UnifiedCube[] = localCubes
      .filter((c) => !cloudNames.has(c.name.toLowerCase()))
      .map((c) => ({ ...c, online: false }))
    return [...cloudCubes, ...locals].sort((a, b) => b.updatedAt - a.updatedAt)
  }, [cloud, localCubes])

  /**
   * Save a cube to wherever it belongs: the account when signed in (overwriting the same-named cube
   * rather than duplicating it), otherwise localStorage. `existing` routes an edit back to the copy it
   * came from even if the name changed. Resolves to the cube as saved, so a caller holding a brand-new
   * cube learns the id it now lives under.
   */
  const saveCube = useCallback(
    async (cube: SharedCube, existing?: UnifiedCube): Promise<UnifiedCube> => {
      if (isLoggedIn) {
        const cloudId =
          existing?.cloudId ??
          cloud.find((c) => c.name.toLowerCase() === cube.name.toLowerCase())?.id
        const detail = cloudId != null ? await apiUpdateCube(cloudId, cube) : await apiSaveCube(cube)
        setCloud((prev) => [detail, ...prev.filter((c) => c.id !== detail.id)])
        return detailToUnified(detail)
      }
      const saved = saveLocal({
        ...(existing && existing.cloudId == null ? { id: existing.id } : {}),
        ...cubeToShared(cube),
      })
      return { ...saved, online: false }
    },
    [isLoggedIn, cloud, saveLocal],
  )

  const removeCube = useCallback(
    async (cube: UnifiedCube) => {
      if (cube.cloudId != null) {
        await apiDeleteCube(cube.cloudId)
        setCloud((prev) => prev.filter((c) => c.id !== cube.cloudId))
      } else {
        deleteLocal(cube.id)
      }
    },
    [deleteLocal],
  )

  const renameCube = useCallback(
    async (cube: UnifiedCube, name: string) => {
      if (cube.cloudId != null) {
        await apiUpdateCube(cube.cloudId, { ...cubeToShared(cube), name })
        reload()
      } else {
        renameLocal(cube.id, name)
      }
    },
    [renameLocal, reload],
  )

  /** Patch one cloud cube's share code in place, so a link appears without a full reload. */
  const setShareCode = useCallback((cloudId: number, shareCode: string | null) => {
    setCloud((prev) => prev.map((c) => (c.id === cloudId ? { ...c, shareCode } : c)))
  }, [])

  /** Create (or fetch the existing) share link for a cloud cube; resolves to its code. */
  const share = useCallback(
    async (cube: UnifiedCube): Promise<string> => {
      if (cube.cloudId == null) throw new Error('Only cubes saved to your account can be shared by link')
      const code = await apiShareCube(cube.cloudId)
      setShareCode(cube.cloudId, code)
      return code
    },
    [setShareCode],
  )

  const unshare = useCallback(
    async (cube: UnifiedCube): Promise<void> => {
      if (cube.cloudId == null) return
      await apiUnshareCube(cube.cloudId)
      setShareCode(cube.cloudId, null)
    },
    [setShareCode],
  )

  /**
   * Every source has answered: the local library is read, sign-in has settled, and a signed-in
   * user's cloud cubes have arrived. Until then "no such cube" can't be told apart from "not yet".
   */
  const ready = hydrated && authStatus !== 'idle' && authStatus !== 'loading' &&
    loadedFor === (isLoggedIn ? 'account' : 'anonymous')

  return { cubes, loading, ready, reload, saveCube, removeCube, renameCube, share, unshare, isLoggedIn }
}
