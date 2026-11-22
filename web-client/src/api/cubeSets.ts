/**
 * A set's implemented cards, shaped for adding to a cube: each card's rarity *in that set*, whether
 * it is a reprint, and which optional product (if any) carries it. Public, like the rest of `/api/sets`.
 */
import type { SetCubeCards } from '@/components/cube/cubeModel'

export async function getSetCubeCards(setCode: string): Promise<SetCubeCards> {
  const res = await fetch(`/api/sets/${encodeURIComponent(setCode)}/cube-cards`)
  if (res.status === 404) throw new Error(`No cards found for set ${setCode}`)
  if (!res.ok) throw new Error(`Failed to load ${setCode} (${res.status})`)
  return (await res.json()) as SetCubeCards
}

/** A catalogued set, as `GET /api/sets` lists it. */
export interface CatalogSet {
  readonly code: string
  readonly name: string
  readonly releaseDate: string | null
}

let catalogCache: Promise<CatalogSet[]> | null = null

/** Every catalogued set, newest first; fetched once per page load. */
export function listCatalogSets(): Promise<CatalogSet[]> {
  catalogCache ??= fetch('/api/sets')
    .then((res) => (res.ok ? (res.json() as Promise<CatalogSet[]>) : []))
    .then((sets) => [...sets].sort((a, b) => (b.releaseDate ?? '').localeCompare(a.releaseDate ?? '')))
    .catch(() => {
      catalogCache = null
      return []
    })
  return catalogCache
}

/**
 * Sets a cube can borrow basic-land art from. Basics come with a playable set, so the source is the
 * booster-ready list — the same kind of set the lobby offers.
 */
export async function listBasicLandSets(): Promise<{ code: string; name: string }[]> {
  try {
    const res = await fetch('/api/sets/booster-ready')
    if (!res.ok) return []
    const sets = (await res.json()) as { setCode: string; setName: string }[]
    return sets.map((s) => ({ code: s.setCode, name: s.setName })).sort((a, b) => a.name.localeCompare(b.name))
  } catch {
    return []
  }
}
