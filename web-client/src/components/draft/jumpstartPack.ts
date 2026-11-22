import type { SealedCardInfo } from '@/types'
import { getCardColors, getCmc } from './draftPool'

/** WUBRG order, the order every pip row is drawn in. */
export const COLOR_ORDER = ['W', 'U', 'B', 'R', 'G'] as const
export type ColorCode = (typeof COLOR_ORDER)[number]

const BASIC_COLOR: Record<string, ColorCode> = { Plains: 'W', Island: 'U', Swamp: 'B', Mountain: 'R', Forest: 'G' }

const COLOR_NAME: Record<ColorCode, string> = { W: 'White', U: 'Blue', B: 'Black', R: 'Red', G: 'Green' }

const PAIR_NAME: Record<string, string> = {
  WU: 'Azorius', WB: 'Orzhov', WR: 'Boros', WG: 'Selesnya', UB: 'Dimir',
  UR: 'Izzet', UG: 'Simic', BR: 'Rakdos', BG: 'Golgari', RG: 'Gruul',
}

export interface PackEntry {
  readonly card: SealedCardInfo
  readonly count: number
}

export interface PackSection {
  readonly key: 'creatures' | 'spells' | 'lands'
  readonly label: string
  readonly entries: readonly PackEntry[]
  readonly total: number
}

export interface PackSummary {
  /** The pack's colors in WUBRG order. */
  readonly colors: readonly ColorCode[]
  /** Nonland cards by mana value: index 0 = 0–1, …, index 5 = 6+. */
  readonly curve: readonly number[]
  readonly creatures: number
  readonly spells: number
  readonly lands: number
  /** Rares and mythics first, then uncommons — the cards worth naming on a pack face. */
  readonly highlights: readonly SealedCardInfo[]
  readonly sections: readonly PackSection[]
}

const RARITY_RANK: Record<string, number> = { MYTHIC: 0, RARE: 1, UNCOMMON: 2, COMMON: 3 }

const isLand = (card: SealedCardInfo) => /\bLand\b/.test(card.typeLine)
const isCreature = (card: SealedCardInfo) => /\bCreature\b/.test(card.typeLine)

function group(cards: readonly SealedCardInfo[]): PackEntry[] {
  const byName = new Map<string, PackEntry>()
  for (const card of cards) {
    const prev = byName.get(card.name)
    byName.set(card.name, { card, count: (prev?.count ?? 0) + 1 })
  }
  return [...byName.values()]
}

const byCurveThenName = (a: PackEntry, b: PackEntry) =>
  getCmc(a.card) - getCmc(b.card) || a.card.name.localeCompare(b.card.name)

/**
 * A Jumpstart pack's colors come from its basics — every published pack carries the lands for
 * exactly its colors — topped up by any color that two or more spells ask for, which catches the
 * rare splash card a pack's basics don't cover.
 */
function packColors(cards: readonly SealedCardInfo[]): ColorCode[] {
  const found = new Set<ColorCode>()
  const spellColorCounts = new Map<ColorCode, number>()
  for (const card of cards) {
    const basic = BASIC_COLOR[card.name]
    if (basic) { found.add(basic); continue }
    if (isLand(card)) continue
    for (const c of getCardColors(card)) {
      spellColorCounts.set(c as ColorCode, (spellColorCounts.get(c as ColorCode) ?? 0) + 1)
    }
  }
  if (found.size === 0) {
    for (const [c, n] of spellColorCounts) if (n >= 2) found.add(c)
  }
  return COLOR_ORDER.filter((c) => found.has(c))
}

export function summarizePack(cards: readonly SealedCardInfo[]): PackSummary {
  const curve = [0, 0, 0, 0, 0, 0]
  let creatures = 0, spells = 0, lands = 0
  for (const card of cards) {
    if (isLand(card)) { lands++; continue }
    if (isCreature(card)) creatures++
    else spells++
    const bucket = Math.min(Math.max(getCmc(card), 1), 6) - 1
    curve[bucket]!++
  }
  const entries = group(cards)
  const section = (key: PackSection['key'], label: string, pick: (c: SealedCardInfo) => boolean): PackSection => {
    const list = entries.filter((e) => pick(e.card)).sort(byCurveThenName)
    return { key, label, entries: list, total: list.reduce((n, e) => n + e.count, 0) }
  }
  const highlights = entries
    .filter((e) => !isLand(e.card) && (RARITY_RANK[e.card.rarity] ?? 3) <= 2)
    .map((e) => e.card)
    .sort((a, b) => (RARITY_RANK[a.rarity] ?? 3) - (RARITY_RANK[b.rarity] ?? 3) || getCmc(b) - getCmc(a))
  return {
    colors: packColors(cards),
    curve,
    creatures,
    spells,
    lands,
    highlights,
    sections: [
      section('creatures', 'Creatures', (c) => !isLand(c) && isCreature(c)),
      section('spells', 'Spells', (c) => !isLand(c) && !isCreature(c)),
      section('lands', 'Lands', isLand),
    ].filter((s) => s.total > 0),
  }
}

/** The card a pack is pictured by: its rarest, most expensive nonland card. */
export function faceCard(cards: readonly SealedCardInfo[]): SealedCardInfo | undefined {
  return summarizePack(cards).highlights[0] ?? cards.find((c) => !isLand(c)) ?? cards[0]
}

/** "Mono-Red", "Izzet", "Three colors" — how a finished deck's colors read. */
export function colorsLabel(colors: readonly ColorCode[]): string {
  if (colors.length === 0) return 'Colorless'
  if (colors.length === 1) return `Mono-${COLOR_NAME[colors[0]!]}`
  if (colors.length === 2) return PAIR_NAME[colors.join('')] ?? colors.join('')
  return `${colors.length} colors`
}

export function mergeColors(...sets: ReadonlyArray<readonly ColorCode[]>): ColorCode[] {
  const all = new Set(sets.flat())
  return COLOR_ORDER.filter((c) => all.has(c))
}
