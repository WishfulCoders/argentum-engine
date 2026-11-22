import { describe, expect, it } from 'vitest'
import type { CardSummary } from '@/components/deckbuilder/cardFilter'
import { colorGroup, groupCubeEntries } from '../cubeGrouping'

function card(name: string, over: Partial<CardSummary>): CardSummary {
  return {
    name,
    manaCost: '',
    cmc: 1,
    colors: [],
    colorIdentity: [],
    cardTypes: ['CREATURE'],
    supertypes: [],
    subtypes: [],
    basicLand: false,
    rarity: 'COMMON',
    setCode: 'BLB',
    collectorNumber: null,
    ...over,
  }
}

const catalog = new Map<string, CardSummary>([
  ['bolt', card('Bolt', { cardTypes: ['INSTANT'], colorIdentity: ['RED'], rarity: 'COMMON' })],
  ['elf', card('Elf', { colorIdentity: ['GREEN'], rarity: 'UNCOMMON' })],
  ['gold', card('Gold', { colorIdentity: ['GREEN', 'WHITE'], rarity: 'RARE' })],
  ['grotto', card('Grotto', { cardTypes: ['LAND'], rarity: 'COMMON' })],
])
const lookup = (name: string) => catalog.get(name.toLowerCase())

describe('groupCubeEntries', () => {
  const entries = [
    { name: 'Gold', count: 1 },
    { name: 'Bolt', count: 1 },
    { name: 'Elf', count: 1, rarity: 'MYTHIC' as const },
    { name: 'Grotto', count: 1 },
    { name: 'Unknown', count: 1 },
  ]

  it('groups by type in deck-list order, unimplemented last', () => {
    expect(groupCubeEntries(entries, lookup, 'TYPE').map((g) => [g.label, g.entries.map((e) => e.name)])).toEqual([
      ['Creature', ['Elf', 'Gold']],
      ['Instant', ['Bolt']],
      ['Land', ['Grotto']],
      ['Not implemented', ['Unknown']],
    ])
  })

  it('groups by colour with gold, colourless and lands apart', () => {
    expect(groupCubeEntries(entries, lookup, 'COLOR').map((g) => g.label)).toEqual([
      'Red',
      'Green',
      'Multicolour',
      'Lands',
      'Not implemented',
    ])
    expect(colorGroup(card('Rock', { cardTypes: ['ARTIFACT'] }))).toBe('Colourless')
  })

  it('groups by the rarity the cube prints, overrides included, mythic first', () => {
    expect(groupCubeEntries(entries, lookup, 'RARITY').map((g) => [g.label, g.entries.map((e) => e.name)])).toEqual([
      ['Mythic', ['Elf']],
      ['Rare', ['Gold']],
      ['Common', ['Bolt', 'Grotto', 'Unknown']],
    ])
  })
})
