/**
 * How a cube list is sectioned on screen. A curator balances a cube along three axes — what the
 * cards do (type), who plays them (colour), and how often they open (rarity) — so the list can be
 * grouped by any of them. Pure: the card lookup is passed in.
 */
import type { SharedCubeEntry } from '@/api/account'
import type { CardSummary } from '@/components/deckbuilder/cardFilter'
import { CUBE_RARITIES, RARITY_LABEL, resolveRarity } from './cubeModel'

export type CubeGroupBy = 'TYPE' | 'COLOR' | 'RARITY'

export const GROUP_BY_LABEL: Record<CubeGroupBy, string> = {
  TYPE: 'Type',
  COLOR: 'Colour',
  RARITY: 'Rarity',
}

/** Deck-list ordering for the type view: the groups a curator actually thinks in. */
const TYPE_ORDER = ['Creature', 'Planeswalker', 'Instant', 'Sorcery', 'Artifact', 'Enchantment', 'Battle', 'Land'] as const

/** Catalog colours arrive as enum names (`BLUE`), so the label is looked up rather than derived. */
const COLOR_ORDER = [
  ['WHITE', 'White'],
  ['BLUE', 'Blue'],
  ['BLACK', 'Black'],
  ['RED', 'Red'],
  ['GREEN', 'Green'],
] as const

const NOT_IMPLEMENTED = 'Not implemented'

export function primaryType(card: CardSummary | undefined): string {
  if (!card) return NOT_IMPLEMENTED
  const types = card.cardTypes.map((t) => t.charAt(0) + t.slice(1).toLowerCase())
  for (const candidate of TYPE_ORDER) {
    if (types.includes(candidate)) return candidate
  }
  return types[0] ?? 'Other'
}

/** Mono colours by name; gold, colourless and lands as their own groups, the way cube lists read. */
export function colorGroup(card: CardSummary | undefined): string {
  if (!card) return NOT_IMPLEMENTED
  if (card.cardTypes.includes('LAND')) return 'Lands'
  const identity = card.colorIdentity ?? []
  if (identity.length === 0) return 'Colourless'
  if (identity.length > 1) return 'Multicolour'
  return COLOR_ORDER.find(([key]) => key === identity[0])?.[1] ?? 'Colourless'
}

function groupOrder(by: CubeGroupBy): readonly string[] {
  switch (by) {
    case 'TYPE': return [...TYPE_ORDER, 'Other', NOT_IMPLEMENTED]
    case 'COLOR': return [...COLOR_ORDER.map(([, label]) => label), 'Multicolour', 'Colourless', 'Lands', NOT_IMPLEMENTED]
    case 'RARITY': return [...CUBE_RARITIES].reverse().map((r) => RARITY_LABEL[r])
  }
}

export interface CubeGroup {
  readonly label: string
  readonly entries: readonly SharedCubeEntry[]
}

/** Section the entries, each section name-sorted, sections in the axis's natural order. */
export function groupCubeEntries(
  entries: readonly SharedCubeEntry[],
  lookup: (name: string) => CardSummary | undefined,
  by: CubeGroupBy,
): CubeGroup[] {
  const groups = new Map<string, SharedCubeEntry[]>()
  for (const entry of entries) {
    const card = lookup(entry.name)
    const key = by === 'TYPE'
      ? primaryType(card)
      : by === 'COLOR'
        ? colorGroup(card)
        : RARITY_LABEL[resolveRarity(entry, card?.rarity).rarity]
    const bucket = groups.get(key)
    if (bucket) bucket.push(entry)
    else groups.set(key, [entry])
  }
  const order = groupOrder(by)
  const rank = (label: string) => {
    const i = order.indexOf(label)
    return i < 0 ? order.length - 1 : i
  }
  return [...groups.entries()]
    .sort((a, b) => rank(a[0]) - rank(b[0]) || a[0].localeCompare(b[0]))
    .map(([label, list]) => ({ label, entries: [...list].sort((x, y) => x.name.localeCompare(y.name)) }))
}
