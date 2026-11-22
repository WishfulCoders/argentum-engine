/**
 * A cube being edited: the working copy, what changes it, and saving it back.
 *
 * Both cube editing surfaces sit on this — the cube mode of the builder (`/cubes/new`,
 * `/cubes/edit/:id`) and the compact dialog the lobby opens — so they share one set of rules for
 * adding, merging, re-rarifying and saving, and differ only in layout.
 */
import { useCallback, useEffect, useMemo, useState } from 'react'
import type { CubeBoosterStyle, CubeRarity, SharedCube, SharedCubeEntry } from '@/api/account'
import type { CardSummary } from '@/components/deckbuilder/cardFilter'
import { DEFAULT_CUBE_PACK_SIZE, cubeCardCount } from '@/store/cubeLibrary'
import { type UnifiedCube, cubeToShared, useUnifiedCubes } from '@/store/useUnifiedCubes'
import { cubeToText, mergeEntries, packHealth, rarityTallies } from './cubeModel'

/** A set the cube can take its basic-land art from. */
export interface BasicLandSetOption {
  readonly code: string
  readonly name: string
}

/** The name a new cube starts with — replaced by a set's name when the first thing added is a set. */
export const DEFAULT_CUBE_NAME = 'My Cube'

export interface CubeDraftOptions {
  /** Cube to edit, or null to start a new one. */
  readonly cube: UnifiedCube | null
  readonly availableSets: readonly BasicLandSetOption[]
  /** Case-insensitive card lookup over the catalog; undefined for a name it doesn't know. */
  readonly lookup: (name: string) => CardSummary | undefined
  /** False until the catalog has loaded — nothing is "unimplemented" before then. */
  readonly catalogReady: boolean
  /** Told about every successful save — a page uses it to move a new cube onto its own URL. */
  readonly onSaved?: (cube: UnifiedCube) => void
  /** Unsaved edits to pick up where they were left — what `workingCopy()` handed out earlier. */
  readonly resume?: CubeWorkingCopy | null
}

/** A draft's edits plus what they are measured against, so a restored draft is exactly as dirty as it was. */
export interface CubeWorkingCopy {
  readonly name: string
  readonly description: string
  readonly entries: SharedCubeEntry[]
  readonly packSize: number
  readonly boosterStyle: CubeBoosterStyle
  readonly basicLandSetCode: string
  readonly baseline: string
}

export function useCubeDraft({ cube, availableSets, lookup, catalogReady, onSaved, resume }: CubeDraftOptions) {
  const { cubes, saveCube, share, unshare, isLoggedIn } = useUnifiedCubes()

  const [saved, setSaved] = useState<UnifiedCube | null>(cube)
  const [name, setName] = useState(resume?.name ?? cube?.name ?? DEFAULT_CUBE_NAME)
  const [description, setDescription] = useState(resume?.description ?? cube?.description ?? '')
  const [entries, setEntries] = useState<SharedCubeEntry[]>(
    () => resume?.entries ?? cube?.cards.map((e) => ({ ...e })) ?? [],
  )
  const [packSize, setPackSize] = useState(resume?.packSize ?? cube?.packSize ?? DEFAULT_CUBE_PACK_SIZE)
  const [boosterStyle, setBoosterStyle] = useState<CubeBoosterStyle>(
    resume?.boosterStyle ?? cube?.boosterStyle ?? 'SHUFFLED',
  )
  const [basicLandSetCode, setBasicLandSetCode] = useState(
    resume?.basicLandSetCode ?? cube?.basicLandSetCode ?? availableSets[0]?.code ?? '',
  )
  const [saveError, setSaveError] = useState<string | null>(null)
  const [saving, setSaving] = useState(false)
  const [notice, setNotice] = useState<string | null>(null)

  const shared: SharedCube = useMemo(
    () =>
      cubeToShared({
        name: name.trim() || 'Untitled cube',
        description: description.trim(),
        cards: entries,
        basicLandSetCode,
        packSize,
        boosterStyle,
      }),
    [name, description, entries, basicLandSetCode, packSize, boosterStyle],
  )

  /** What was last saved (or opened), to tell unsaved edits apart from a clean cube. */
  const [baseline, setBaseline] = useState(() => resume?.baseline ?? JSON.stringify(shared))
  const dirty = JSON.stringify(shared) !== baseline

  /** The unsaved edits, for a page to stash while the player is elsewhere; null when there are none. */
  const workingCopy = (): CubeWorkingCopy | null =>
    dirty ? { name, description, entries, packSize, boosterStyle, basicLandSetCode, baseline } : null

  // The editor renders before the server's set list has arrived; adopt the first set once it does —
  // into the baseline too, since that is a default arriving, not an edit.
  useEffect(() => {
    if (basicLandSetCode !== '' || !availableSets[0]) return
    const code = availableSets[0].code
    setBasicLandSetCode(code)
    setBaseline((prev) => JSON.stringify({ ...JSON.parse(prev), basicLandSetCode: code }))
  }, [availableSets, basicLandSetCode])

  // A tab close would drop unsaved work without asking; the browser's own prompt is the only one
  // that can run there. In-app exits ask through the editor's confirm bar instead.
  useEffect(() => {
    if (!dirty) return
    const warn = (e: BeforeUnloadEvent) => e.preventDefault()
    window.addEventListener('beforeunload', warn)
    return () => window.removeEventListener('beforeunload', warn)
  }, [dirty])

  const totalCards = cubeCardCount(entries)
  /** Physical copies per card name, the shape the card grid badges its tiles with. */
  const counts = useMemo(() => Object.fromEntries(entries.map((e) => [e.name, e.count])), [entries])
  const inCube = useMemo(() => new Set(entries.map((e) => e.name.toLowerCase())), [entries])
  /** Names the catalog doesn't know: the cube is unplayable until these are gone. */
  const unresolved = useMemo(
    () => (catalogReady ? entries.filter((e) => !lookup(e.name)) : []),
    [entries, lookup, catalogReady],
  )
  const health = useMemo(
    () => packHealth(boosterStyle, rarityTallies(entries, (n) => lookup(n)?.rarity).distinct),
    [boosterStyle, entries, lookup],
  )
  const shortSlots = useMemo(
    () => new Set<string>(health?.lines.filter((l) => !l.ok).map((l) => l.slot) ?? []),
    [health],
  )

  const flash = useCallback((message: string) => {
    setNotice(message)
    window.setTimeout(() => setNotice((current) => (current === message ? null : current)), 2400)
  }, [])

  // Stable, so the cube view's memoized rows don't re-render on every keystroke elsewhere.
  const setCount = useCallback((cardName: string, count: number) => {
    setEntries((prev) =>
      count <= 0 ? prev.filter((e) => e.name !== cardName) : prev.map((e) => (e.name === cardName ? { ...e, count } : e)),
    )
  }, [])

  const setRarity = useCallback((cardName: string, rarity: CubeRarity | null) => {
    setEntries((prev) =>
      prev.map((e) => {
        if (e.name !== cardName) return e
        return rarity ? { ...e, rarity } : { name: e.name, count: e.count }
      }),
    )
  }, [])

  /** One more copy of a card: a new entry, or a second copy of one already in. */
  function addOne(cardName: string) {
    setEntries((prev) => {
      const existing = prev.find((e) => e.name.toLowerCase() === cardName.toLowerCase())
      return existing
        ? prev.map((e) => (e === existing ? { ...e, count: e.count + 1 } : e))
        : [...prev, { name: cardName, count: 1 }]
    })
  }

  /** One fewer copy; the last copy leaves the cube. */
  function removeOne(cardName: string) {
    setEntries((prev) => {
      const existing = prev.find((e) => e.name.toLowerCase() === cardName.toLowerCase())
      if (!existing) return prev
      return existing.count <= 1
        ? prev.filter((e) => e !== existing)
        : prev.map((e) => (e === existing ? { ...e, count: e.count - 1 } : e))
    })
  }

  function addFromSet(additions: SharedCubeEntry[], set: { code: string; name: string }) {
    const wasEmpty = entries.length === 0
    const { entries: merged, added } = mergeEntries(entries, additions)
    setEntries(merged)
    // The first set into an empty cube is what the cube *is* — offer its name and its basics.
    if (wasEmpty) {
      if (name.trim() === '' || name === DEFAULT_CUBE_NAME) setName(`${set.name} Cube`)
      if (availableSets.some((s) => s.code === set.code)) setBasicLandSetCode(set.code)
    }
    flash(`Added ${added} card${added === 1 ? '' : 's'} from ${set.name}`)
  }

  function importList(list: SharedCubeEntry[], replace: boolean) {
    if (replace) {
      setEntries(list)
      flash(`Imported ${cubeCardCount(list)} cards`)
    } else {
      const { entries: merged, added } = mergeEntries(entries, list)
      setEntries(merged)
      flash(`Added ${added} card${added === 1 ? '' : 's'}`)
    }
  }

  function dropUnresolved() {
    const misses = new Set(unresolved.map((e) => e.name))
    setEntries((prev) => prev.filter((e) => !misses.has(e.name)))
  }

  async function save(): Promise<UnifiedCube | null> {
    setSaveError(null)
    setSaving(true)
    try {
      // Route back to the copy this editor opened, or to a same-named cube in the library.
      const target = saved ?? cubes.find((c) => c.name.toLowerCase() === shared.name.toLowerCase())
      const result = await saveCube(shared, target)
      setSaved(result)
      setBaseline(JSON.stringify(shared))
      flash(result.online ? 'Saved to your account' : 'Saved on this device')
      onSaved?.(result)
      return result
    } catch (err) {
      setSaveError(err instanceof Error ? err.message : 'Failed to save cube')
      return null
    } finally {
      setSaving(false)
    }
  }

  async function copyList() {
    try {
      await navigator.clipboard.writeText(cubeToText(shared))
      flash('Cube list copied')
    } catch {
      setSaveError('Your browser blocked the clipboard.')
    }
  }

  /** Share-dialog hooks that also keep this draft's copy of the share code current. */
  const shareSaved = async (c: UnifiedCube) => {
    const code = await share(c)
    setSaved((prev) => (prev ? { ...prev, shareCode: code } : prev))
    return code
  }
  const unshareSaved = async (c: UnifiedCube) => {
    await unshare(c)
    setSaved((prev) => (prev ? { ...prev, shareCode: null } : prev))
  }

  const canUse = totalCards > 0 && unresolved.length === 0 && basicLandSetCode !== ''
  const saveState = notice
    ?? (dirty ? 'Unsaved changes' : saved ? (saved.online ? 'Saved to your account' : 'Saved on this device') : '')
  const landOptions = availableSets.some((s) => s.code === basicLandSetCode) || basicLandSetCode === ''
    ? availableSets
    : [{ code: basicLandSetCode, name: basicLandSetCode }, ...availableSets]

  return {
    // The working copy
    name, setName,
    description, setDescription,
    entries, setEntries,
    packSize, setPackSize,
    boosterStyle, setBoosterStyle,
    basicLandSetCode, setBasicLandSetCode,
    shared,
    // Derived
    totalCards, counts, inCube, unresolved, shortSlots, canUse, landOptions,
    // Editing
    setCount, setRarity, addOne, removeOne, addFromSet, importList, dropUnresolved,
    // Saving and sharing
    saved, dirty, saving, saveError, notice, saveState, isLoggedIn, workingCopy,
    save, copyList, shareSaved, unshareSaved,
  }
}

export type CubeDraft = ReturnType<typeof useCubeDraft>
