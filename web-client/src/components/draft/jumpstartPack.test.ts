import { describe, expect, it } from 'vitest'
import type { SealedCardInfo } from '@/types'
import { colorsLabel, faceCard, mergeColors, summarizePack } from './jumpstartPack'

const card = (name: string, manaCost: string | null, typeLine: string, rarity = 'COMMON'): SealedCardInfo =>
  ({ name, manaCost, typeLine, rarity, imageUri: null })

const goblins: SealedCardInfo[] = [
  card('Goblin Warchief', '{1}{R}{R}', 'Creature — Goblin Warrior', 'UNCOMMON'),
  card('Krenko, Tin Street Kingpin', '{2}{R}', 'Legendary Creature — Goblin Warrior', 'RARE'),
  card('Shock', '{R}', 'Instant'),
  card('Shock', '{R}', 'Instant'),
  card('Fling', '{1}{R}', 'Instant'),
  card('Thriving Bluff', null, 'Land'),
  ...Array.from({ length: 7 }, () => card('Mountain', null, 'Basic Land — Mountain')),
]

describe('summarizePack', () => {
  const summary = summarizePack(goblins)

  it('reads its colors from the basics', () => {
    expect(summary.colors).toEqual(['R'])
  })

  it('counts creatures, spells and lands separately', () => {
    expect([summary.creatures, summary.spells, summary.lands]).toEqual([2, 3, 8])
  })

  it('buckets the nonland curve and folds duplicates into one entry', () => {
    expect(summary.curve).toEqual([2, 1, 2, 0, 0, 0])
    const spells = summary.sections.find((s) => s.key === 'spells')!
    expect(spells.entries.map((e) => [e.card.name, e.count])).toEqual([['Shock', 2], ['Fling', 1]])
    expect(summary.sections.find((s) => s.key === 'lands')!.total).toBe(8)
  })

  it('pictures the pack by its rare', () => {
    expect(faceCard(goblins)?.name).toBe('Krenko, Tin Street Kingpin')
    expect(summary.highlights.map((c) => c.name)).toEqual(['Krenko, Tin Street Kingpin', 'Goblin Warchief'])
  })

  it('falls back to spell colors when a pack has no basics', () => {
    expect(summarizePack([card('A', '{U}', 'Instant'), card('B', '{1}{U}', 'Sorcery'), card('C', '{G}', 'Instant')]).colors)
      .toEqual(['U'])
  })
})

describe('colorsLabel', () => {
  it('names mono, guild and wider decks', () => {
    expect(colorsLabel(['R'])).toBe('Mono-Red')
    expect(colorsLabel(mergeColors(['R'], ['U']))).toBe('Izzet')
    expect(colorsLabel(['W', 'B', 'G'])).toBe('3 colors')
  })
})
