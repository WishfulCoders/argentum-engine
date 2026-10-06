import type { SealedCardInfo } from '@/types'

/** How the drafted pool is split into columns on the mobile Pool tab. */
export type PoolGrouping = 'color' | 'type' | 'curve'

export interface PoolEntry {
  readonly card: SealedCardInfo
  /** Copies of this card in the group (duplicates are shown once). */
  readonly count: number
}

export interface PoolGroup {
  readonly key: string
  readonly label: string
  readonly entries: readonly PoolEntry[]
  /** Card count including duplicates. */
  readonly total: number
}

const COLOR_GROUPS: ReadonlyArray<readonly [string, string]> = [
  ['W', 'White'], ['U', 'Blue'], ['B', 'Black'], ['R', 'Red'], ['G', 'Green'], ['M', 'Multicolor'], ['C', 'Colorless'],
]

// Checked in order, so an artifact creature lands in Creature.
const TYPE_GROUPS: ReadonlyArray<readonly [string, string]> = [
  ['creature', 'Creature'], ['planeswalker', 'Planeswalker'], ['instant', 'Instant'], ['sorcery', 'Sorcery'],
  ['enchantment', 'Enchantment'], ['artifact', 'Artifact'], ['land', 'Land'],
]

const CURVE_GROUPS: ReadonlyArray<readonly [string, string]> = [
  ['0', '0–1'], ['2', '2'], ['3', '3'], ['4', '4'], ['5', '5'], ['6', '6+'],
]

/** Colors from the mana cost, matching the desktop pool sidebar. */
export function getCardColors(card: SealedCardInfo): Set<string> {
  const cost = card.manaCost || ''
  const colors = new Set<string>()
  for (const c of ['W', 'U', 'B', 'R', 'G']) {
    if (cost.includes(c)) colors.add(c)
  }
  return colors
}

export function getCmc(card: SealedCardInfo): number {
  const cost = card.manaCost || ''
  let cmc = 0
  const matches = cost.match(/\{([^}]+)\}/g) || []
  for (const match of matches) {
    const inner = match.slice(1, -1)
    const num = parseInt(inner, 10)
    if (!isNaN(num)) {
      cmc += num
    } else if (inner !== 'X') {
      cmc += 1
    }
  }
  return cmc
}

/** W/U/B/R/G for a mono-colored card, M for multicolor, C for colorless. */
export function getColorKey(card: SealedCardInfo): string {
  const colors = getCardColors(card)
  if (colors.size === 0) return 'C'
  if (colors.size > 1) return 'M'
  return [...colors][0]!
}

function groupKey(card: SealedCardInfo, grouping: PoolGrouping): string {
  switch (grouping) {
    case 'color':
      return getColorKey(card)
    case 'type': {
      const typeLine = card.typeLine.toLowerCase()
      return TYPE_GROUPS.find(([t]) => typeLine.includes(t))?.[0] ?? 'other'
    }
    case 'curve': {
      const cmc = getCmc(card)
      return cmc <= 1 ? '0' : String(Math.min(cmc, 6))
    }
  }
}

function groupOrder(grouping: PoolGrouping): ReadonlyArray<readonly [string, string]> {
  switch (grouping) {
    case 'color':
      return COLOR_GROUPS
    case 'type':
      return [...TYPE_GROUPS, ['other', 'Other']]
    case 'curve':
      return CURVE_GROUPS
  }
}

/**
 * Splits the drafted pool into display groups. Empty groups are dropped, duplicates collapse into
 * one entry with a count, and each group is sorted by mana value then name.
 */
export function groupPool(cards: readonly SealedCardInfo[], grouping: PoolGrouping): PoolGroup[] {
  const byKey = new Map<string, Map<string, { card: SealedCardInfo; count: number }>>()
  for (const card of cards) {
    const key = groupKey(card, grouping)
    let entries = byKey.get(key)
    if (!entries) {
      entries = new Map()
      byKey.set(key, entries)
    }
    const existing = entries.get(card.name)
    if (existing) existing.count++
    else entries.set(card.name, { card, count: 1 })
  }

  const groups: PoolGroup[] = []
  for (const [key, label] of groupOrder(grouping)) {
    const entries = byKey.get(key)
    if (!entries) continue
    const sorted = [...entries.values()].sort((a, b) => getCmc(a.card) - getCmc(b.card) || a.card.name.localeCompare(b.card.name))
    groups.push({ key, label, entries: sorted, total: sorted.reduce((sum, e) => sum + e.count, 0) })
  }
  return groups
}
