import { describe, expect, it } from 'vitest'
import type { SealedCardInfo } from '@/types'
import { getCmc, getColorKey, groupPool } from './draftPool'

function card(name: string, manaCost: string | null, typeLine: string): SealedCardInfo {
  return { name, manaCost, typeLine, rarity: 'COMMON', imageUri: null }
}

const POOL = [
  card('Shock', '{R}', 'Instant'),
  card('Sparksmith', '{1}{R}', 'Creature — Goblin'),
  card('Glory Seeker', '{1}{W}', 'Creature — Human Soldier'),
  card('Sparksmith', '{1}{R}', 'Creature — Goblin'),
  card('Lightning Helix', '{R}{W}', 'Instant'),
  card('Ornithopter', '{0}', 'Artifact Creature — Thopter'),
  card('Mountain', null, 'Basic Land — Mountain'),
  card('Exalted Angel', '{4}{W}{W}', 'Creature — Angel'),
  card('Fireball', '{X}{R}', 'Sorcery'),
]

describe('getCmc', () => {
  it('counts generic and colored symbols and treats X as zero', () => {
    expect(getCmc(card('a', '{4}{W}{W}', ''))).toBe(6)
    expect(getCmc(card('b', '{X}{R}', ''))).toBe(1)
    expect(getCmc(card('c', null, ''))).toBe(0)
  })
})

describe('getColorKey', () => {
  it('splits mono, multicolor and colorless', () => {
    expect(getColorKey(card('a', '{1}{R}', ''))).toBe('R')
    expect(getColorKey(card('b', '{R}{W}', ''))).toBe('M')
    expect(getColorKey(card('c', '{3}', ''))).toBe('C')
  })
})

describe('groupPool', () => {
  it('groups by color in WUBRG-M-C order, dropping empty groups', () => {
    const groups = groupPool(POOL, 'color')
    expect(groups.map((g) => g.label)).toEqual(['White', 'Red', 'Multicolor', 'Colorless'])
    expect(groups.map((g) => g.total)).toEqual([2, 4, 1, 2])
  })

  it('collapses duplicates into one entry with a count', () => {
    const red = groupPool(POOL, 'color').find((g) => g.key === 'R')!
    expect(red.entries.map((e) => [e.card.name, e.count])).toEqual([
      ['Fireball', 1], ['Shock', 1], ['Sparksmith', 2],
    ])
  })

  it('sorts each group by mana value, then name', () => {
    const white = groupPool(POOL, 'color').find((g) => g.key === 'W')!
    expect(white.entries.map((e) => e.card.name)).toEqual(['Glory Seeker', 'Exalted Angel'])
  })

  it('groups by card type, putting artifact creatures with creatures', () => {
    const groups = groupPool(POOL, 'type')
    expect(groups.map((g) => [g.label, g.total])).toEqual([
      ['Creature', 5], ['Instant', 2], ['Sorcery', 1], ['Land', 1],
    ])
  })

  it('groups by curve with 0–1 and 6+ buckets', () => {
    const groups = groupPool(POOL, 'curve')
    expect(groups.map((g) => [g.label, g.total])).toEqual([
      ['0–1', 4], ['2', 4], ['6+', 1],
    ])
  })

  it('returns no groups for an empty pool', () => {
    expect(groupPool([], 'color')).toEqual([])
  })
})
