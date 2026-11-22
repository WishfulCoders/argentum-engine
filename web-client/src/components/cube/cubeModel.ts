/**
 * Pure cube arithmetic shared by the editor, the library, the shared-cube page and the lobby panel:
 * what each pack style means, what rarity a card is printed at, whether a cube can fill a pack, how a
 * set's cards fold into a cube, and the two wire shapes a cube leaves the client in (a lobby update
 * and a plain-text list).
 *
 * No React, no fetches — every function here is a value in, value out, so the rules the UI explains
 * are the same ones the tests pin down.
 */
import type { CubeBoosterStyle, CubeRarity, SharedCube, SharedCubeEntry } from '@/api/account'
import { cubeCardNames } from '@/store/cubeLibrary'
import type { RecipeCube } from '@/components/lobby/lobbyRecipe'

/** Booster rarities in printed order, common first. */
export const CUBE_RARITIES: readonly CubeRarity[] = ['COMMON', 'UNCOMMON', 'RARE', 'MYTHIC']

export const RARITY_LABEL: Record<CubeRarity, string> = {
  COMMON: 'Common',
  UNCOMMON: 'Uncommon',
  RARE: 'Rare',
  MYTHIC: 'Mythic',
}

/** The one-letter set-symbol shorthand players already read rarity in. */
export const RARITY_LETTER: Record<CubeRarity, string> = {
  COMMON: 'C',
  UNCOMMON: 'U',
  RARE: 'R',
  MYTHIC: 'M',
}

/** What one pack of a style is made of — mirrors the server's booster strategies for that style. */
export interface PackRecipe {
  readonly commons: number
  readonly uncommons: number
  /** Rare-or-mythic slots (mythic one time in eight). */
  readonly rares: number
  /** Slots that roll any rarity. */
  readonly wildcards: number
}

export interface BoosterStyleInfo {
  readonly style: CubeBoosterStyle
  readonly label: string
  /** One plain sentence for the picker. */
  readonly blurb: string
  /** Null for a shuffled cube, whose pack contents are just the next N cards. */
  readonly pack: PackRecipe | null
}

export const BOOSTER_STYLES: readonly BoosterStyleInfo[] = [
  {
    style: 'SHUFFLED',
    label: 'Shuffled cube',
    blurb: 'The whole cube is shuffled and dealt. Every card shows up at most once per event.',
    pack: null,
  },
  {
    style: 'PLAY_BOOSTER',
    label: 'Play Boosters',
    blurb: 'Packs are printed like a modern set: 7 commons, 3 uncommons, a rare or mythic and 2 wildcards.',
    pack: { commons: 7, uncommons: 3, rares: 1, wildcards: 2 },
  },
  {
    style: 'DRAFT_BOOSTER',
    label: 'Draft Boosters',
    blurb: 'Classic draft packs: 10 commons, 3 uncommons and a rare or mythic.',
    pack: { commons: 10, uncommons: 3, rares: 1, wildcards: 0 },
  },
]

export function boosterStyleInfo(style: CubeBoosterStyle | null | undefined): BoosterStyleInfo {
  return BOOSTER_STYLES.find((s) => s.style === style) ?? BOOSTER_STYLES[0]!
}

/** Whether packs are printed by rarity (as opposed to dealt from a shuffled cube). */
export function isBoosterStyle(style: CubeBoosterStyle | null | undefined): boolean {
  return boosterStyleInfo(style).pack !== null
}

/** Cards in one pack of a booster style; null for a shuffled cube (whose size the host sets). */
export function boosterPackSize(style: CubeBoosterStyle | null | undefined): number | null {
  const pack = boosterStyleInfo(style).pack
  return pack ? pack.commons + pack.uncommons + pack.rares + pack.wildcards : null
}

/**
 * A rarity string from the server, folded onto the four a pack slot can hold. Special and bonus-sheet
 * cards have no slot of their own, so they ride in the rare slot; anything unrecognised is null.
 */
export function toCubeRarity(raw: string | null | undefined): CubeRarity | null {
  switch (raw?.toUpperCase()) {
    case 'COMMON': return 'COMMON'
    case 'UNCOMMON': return 'UNCOMMON'
    case 'RARE':
    case 'SPECIAL':
    case 'BONUS': return 'RARE'
    case 'MYTHIC': return 'MYTHIC'
    default: return null
  }
}

/** The rarity a cube card is printed at, and whether the curator set it or it came from the card. */
export interface ResolvedRarity {
  readonly rarity: CubeRarity
  readonly explicit: boolean
}

/**
 * An entry's override wins; otherwise the card's own catalog rarity; otherwise common — the server's
 * fallback for a card it knows nothing better about, so a pack built here and there agree.
 */
export function resolveRarity(entry: SharedCubeEntry, catalogRarity: string | null | undefined): ResolvedRarity {
  if (entry.rarity) return { rarity: entry.rarity, explicit: true }
  return { rarity: toCubeRarity(catalogRarity) ?? 'COMMON', explicit: false }
}

export type RarityTally = Record<CubeRarity, number>

export function emptyTally(): RarityTally {
  return { COMMON: 0, UNCOMMON: 0, RARE: 0, MYTHIC: 0 }
}

/**
 * Distinct names and physical copies per rarity. Distinct is what a pack needs (no name repeats
 * within a pack); physical is what a curator counts.
 */
export function rarityTallies(
  entries: readonly SharedCubeEntry[],
  catalogRarity: (name: string) => string | null | undefined,
): { distinct: RarityTally; physical: RarityTally } {
  const distinct = emptyTally()
  const physical = emptyTally()
  for (const entry of entries) {
    const { rarity } = resolveRarity(entry, catalogRarity(entry.name))
    distinct[rarity] += 1
    physical[rarity] += entry.count
  }
  return { distinct, physical }
}

export interface PackHealthLine {
  /** The pack slot this line is about. */
  readonly slot: 'common' | 'uncommon' | 'rare'
  readonly label: string
  readonly needed: number
  readonly have: number
  readonly ok: boolean
}

export interface PackHealth {
  readonly ok: boolean
  readonly lines: readonly PackHealthLine[]
}

/**
 * Can this cube fill every slot of a booster style? Commons and uncommons need as many distinct names
 * as their slots (a pack never repeats a name); the rare slot is satisfied by a rare *or* a mythic.
 * Null for a shuffled cube, which has no slots.
 */
export function packHealth(style: CubeBoosterStyle | null | undefined, distinct: RarityTally): PackHealth | null {
  const pack = boosterStyleInfo(style).pack
  if (!pack) return null
  const lines: PackHealthLine[] = [
    { slot: 'common', label: 'commons', needed: pack.commons, have: distinct.COMMON, ok: distinct.COMMON >= pack.commons },
    { slot: 'uncommon', label: 'uncommons', needed: pack.uncommons, have: distinct.UNCOMMON, ok: distinct.UNCOMMON >= pack.uncommons },
    {
      slot: 'rare',
      label: 'rares or mythics',
      needed: pack.rares,
      have: distinct.RARE + distinct.MYTHIC,
      ok: distinct.RARE + distinct.MYTHIC >= pack.rares,
    },
  ]
  return { ok: lines.every((l) => l.ok), lines }
}

/** Players a shuffled cube seats at `packs` packs each. */
export function shuffledSeats(totalCards: number, packSize: number, packs = 3): number {
  if (packSize <= 0 || packs <= 0) return 0
  return Math.floor(totalCards / (packSize * packs))
}

/* ── Adding cards ─────────────────────────────────────────────────────── */

/**
 * Fold `additions` into a cube: a name already in it (case-insensitively) is left exactly as the
 * curator has it — count and rarity — rather than bumped or overwritten. Returns what was skipped so
 * the UI can say "12 already in cube".
 */
export function mergeEntries(
  existing: readonly SharedCubeEntry[],
  additions: readonly SharedCubeEntry[],
): { entries: SharedCubeEntry[]; added: number; skipped: number } {
  const seen = new Set(existing.map((e) => e.name.toLowerCase()))
  const entries = [...existing]
  let added = 0
  let skipped = 0
  for (const entry of additions) {
    const key = entry.name.toLowerCase()
    if (seen.has(key)) {
      skipped += 1
      continue
    }
    seen.add(key)
    entries.push(entry)
    added += 1
  }
  return { entries, added, skipped }
}

/** One card of `/api/sets/{code}/cube-cards`. */
export interface SetCubeCard {
  readonly name: string
  readonly rarity: string
  /** First printed in an earlier set. */
  readonly reprint: boolean
  /** Null = in the set's booster pool; otherwise the optional product that carries it. */
  readonly product: string | null
}

export interface SetCubeCards {
  readonly code: string
  readonly name: string
  readonly releaseDate: string | null
  readonly cards: readonly SetCubeCard[]
}

/** Which slices of a set to take. */
export interface SetImportChoice {
  readonly boosterCards: boolean
  readonly products: ReadonlySet<string>
  readonly reprints: boolean
}

/**
 * The default slices: the booster pool when the set has one, and reprints only for a set that has
 * one. A set with no booster pool of new cards — a Commander product, say — is mostly reprints of
 * staples, and what a curator borrows from it is its own new cards.
 */
export function defaultImportChoice(set: SetCubeCards): SetImportChoice {
  const boosterNew = set.cards.filter((c) => c.product === null && !c.reprint).length
  const boosterAll = set.cards.filter((c) => c.product === null).length
  const reprintsDominate = boosterAll > 0 && boosterNew / boosterAll < 0.5
  return { boosterCards: true, products: new Set(), reprints: !reprintsDominate }
}

/** The cube entries a choice selects, each carrying the rarity the set printed it at. */
export function selectSetCards(set: SetCubeCards, choice: SetImportChoice): SharedCubeEntry[] {
  return set.cards
    .filter((card) => (card.product === null ? choice.boosterCards : choice.products.has(card.product)))
    .filter((card) => choice.reprints || !card.reprint)
    .map((card) => {
      const rarity = toCubeRarity(card.rarity)
      return rarity ? { name: card.name, count: 1, rarity } : { name: card.name, count: 1 }
    })
}

/** `play_booster_extras` → `Play booster extras`. Product ids are machine names; show them as words. */
export function humanizeProduct(id: string): string {
  const words = id.replace(/[_-]+/g, ' ').trim().toLowerCase()
  return words.charAt(0).toUpperCase() + words.slice(1)
}

/* ── Leaving the client ───────────────────────────────────────────────── */

/** Card name → rarity, for the entries whose rarity the curator set. */
export function rarityOverrides(entries: readonly SharedCubeEntry[]): Record<string, CubeRarity> {
  const out: Record<string, CubeRarity> = {}
  for (const entry of entries) if (entry.rarity) out[entry.name] = entry.rarity
  return out
}

/** Everything `updateLobbySettings` needs to put a cube on the table, sent as one message. */
export function cubeLobbyUpdate(cube: SharedCube) {
  const style = cube.boosterStyle ?? 'SHUFFLED'
  return {
    cubeCards: cubeCardNames(cube.cards),
    cubeName: cube.name,
    packSize: cube.packSize,
    cubeBasicLandSetCode: cube.basicLandSetCode,
    cubeBoosterStyle: style,
    cubeRarities: rarityOverrides(cube.cards),
  }
}

/**
 * The cube as a paste-able list in MTG Arena's shape — an `About` block naming it, then `count name`
 * per line — which the cube editor's own Paste tab reads back name and all, and which every other
 * list tool understands. Rarity doesn't survive plain text.
 */
export function cubeToText(cube: Pick<SharedCube, 'name' | 'cards'>): string {
  const lines = [...cube.cards]
    .sort((a, b) => a.name.localeCompare(b.name))
    .map((entry) => `${entry.count} ${entry.name}`)
  return ['About', `Name ${cube.name}`, '', 'Deck', ...lines].join('\n')
}

/**
 * A cube by value, as a saved setup or a "play this cube" launch carries it. Pool Play is a lobby
 * setting rather than a property of the cube, so the caller says which.
 */
export function recipeCubeOf(cube: SharedCube, poolPlay: boolean): RecipeCube {
  const rarities = rarityOverrides(cube.cards)
  return {
    name: cube.name,
    cards: cubeCardNames(cube.cards),
    basicLandSetCode: cube.basicLandSetCode,
    packSize: cube.packSize,
    poolPlay,
    ...(cube.boosterStyle && cube.boosterStyle !== 'SHUFFLED' ? { boosterStyle: cube.boosterStyle } : {}),
    ...(Object.keys(rarities).length > 0 ? { rarities } : {}),
  }
}

/**
 * A name no cube in the library has yet: `base`, else `base (2)`, `base (3)`… Saving matches cubes by
 * name (a same-named cube is overwritten), so a duplicate or a saved copy must never reuse one.
 */
export function uniqueCubeName(base: string, existing: readonly string[]): string {
  const taken = new Set(existing.map((n) => n.trim().toLowerCase()))
  const trimmed = base.trim() || 'Untitled cube'
  if (!taken.has(trimmed.toLowerCase())) return trimmed
  for (let i = 2; ; i++) {
    const candidate = `${trimmed} (${i})`
    if (!taken.has(candidate.toLowerCase())) return candidate
  }
}
