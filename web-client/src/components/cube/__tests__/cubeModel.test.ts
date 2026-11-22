import { describe, expect, it } from 'vitest'
import { parseArenaDeckList } from '@/components/deckbuilder/parseArenaDeck'
import {
  boosterPackSize,
  cubeLobbyUpdate,
  cubeToText,
  defaultImportChoice,
  emptyTally,
  mergeEntries,
  packHealth,
  rarityOverrides,
  rarityTallies,
  recipeCubeOf,
  resolveRarity,
  selectSetCards,
  shuffledSeats,
  toCubeRarity,
  uniqueCubeName,
  type SetCubeCards,
} from '../cubeModel'

describe('rarity resolution', () => {
  it('folds special and bonus onto the rare slot, and rejects unknowns', () => {
    expect(toCubeRarity('common')).toBe('COMMON')
    expect(toCubeRarity('SPECIAL')).toBe('RARE')
    expect(toCubeRarity('BONUS')).toBe('RARE')
    expect(toCubeRarity('MYTHIC')).toBe('MYTHIC')
    expect(toCubeRarity('TIMESHIFTED')).toBeNull()
    expect(toCubeRarity(null)).toBeNull()
  })

  it('prefers the override, then the catalog rarity, then common', () => {
    expect(resolveRarity({ name: 'A', count: 1, rarity: 'MYTHIC' }, 'COMMON')).toEqual({ rarity: 'MYTHIC', explicit: true })
    expect(resolveRarity({ name: 'A', count: 1 }, 'UNCOMMON')).toEqual({ rarity: 'UNCOMMON', explicit: false })
    expect(resolveRarity({ name: 'A', count: 1 }, undefined)).toEqual({ rarity: 'COMMON', explicit: false })
  })

  it('tallies distinct names and physical copies separately', () => {
    const { distinct, physical } = rarityTallies(
      [
        { name: 'A', count: 2 },
        { name: 'B', count: 1, rarity: 'RARE' },
      ],
      () => 'COMMON',
    )
    expect(distinct).toEqual({ COMMON: 1, UNCOMMON: 0, RARE: 1, MYTHIC: 0 })
    expect(physical).toEqual({ COMMON: 2, UNCOMMON: 0, RARE: 1, MYTHIC: 0 })
  })
})

describe('pack health', () => {
  it('is null for a shuffled cube', () => {
    expect(packHealth('SHUFFLED', emptyTally())).toBeNull()
    expect(boosterPackSize('SHUFFLED')).toBeNull()
  })

  it('sizes the booster styles like the server does', () => {
    expect(boosterPackSize('PLAY_BOOSTER')).toBe(13)
    expect(boosterPackSize('DRAFT_BOOSTER')).toBe(14)
  })

  it('needs distinct commons and uncommons per slot, and counts mythics towards the rare slot', () => {
    const health = packHealth('PLAY_BOOSTER', { COMMON: 7, UNCOMMON: 2, RARE: 0, MYTHIC: 1 })!
    expect(health.ok).toBe(false)
    expect(health.lines.filter((l) => !l.ok).map((l) => l.slot)).toEqual(['uncommon'])
    expect(packHealth('DRAFT_BOOSTER', { COMMON: 9, UNCOMMON: 3, RARE: 1, MYTHIC: 0 })!.ok).toBe(false)
    expect(packHealth('DRAFT_BOOSTER', { COMMON: 10, UNCOMMON: 3, RARE: 1, MYTHIC: 0 })!.ok).toBe(true)
  })

  it('counts seats for a shuffled cube at three packs', () => {
    expect(shuffledSeats(360, 15)).toBe(8)
    expect(shuffledSeats(359, 15)).toBe(7)
    expect(shuffledSeats(10, 0)).toBe(0)
  })
})

describe('adding a set', () => {
  const blc: SetCubeCards = {
    code: 'BLC',
    name: 'Bloomburrow Commander',
    releaseDate: '2024-08-02',
    cards: [
      { name: 'Hidden Grotto', rarity: 'RARE', reprint: false, product: null },
      { name: 'Zinnia, Valley’s Voice', rarity: 'MYTHIC', reprint: false, product: null },
      { name: 'Sol Ring', rarity: 'UNCOMMON', reprint: true, product: null },
      { name: 'Arcane Signet', rarity: 'COMMON', reprint: true, product: null },
      { name: 'Command Tower', rarity: 'COMMON', reprint: true, product: null },
      { name: 'Promo Thing', rarity: 'SPECIAL', reprint: false, product: 'promo_pack' },
    ],
  }

  it('turns reprints off by default when they dominate the booster pool', () => {
    expect(defaultImportChoice(blc).reprints).toBe(false)
    const blb: SetCubeCards = { ...blc, cards: blc.cards.map((c) => ({ ...c, reprint: false })) }
    expect(defaultImportChoice(blb).reprints).toBe(true)
  })

  it('selects booster cards, chosen products and (optionally) reprints, at the set’s rarity', () => {
    const choice = { boosterCards: true, products: new Set(['promo_pack']), reprints: false }
    expect(selectSetCards(blc, choice)).toEqual([
      { name: 'Hidden Grotto', count: 1, rarity: 'RARE' },
      { name: 'Zinnia, Valley’s Voice', count: 1, rarity: 'MYTHIC' },
      { name: 'Promo Thing', count: 1, rarity: 'RARE' },
    ])
    expect(selectSetCards(blc, { boosterCards: false, products: new Set(), reprints: true })).toEqual([])
  })

  it('merges without touching cards already in the cube', () => {
    const { entries, added, skipped } = mergeEntries(
      [{ name: 'Sol Ring', count: 2, rarity: 'MYTHIC' }],
      [
        { name: 'sol ring', count: 1, rarity: 'UNCOMMON' },
        { name: 'Hidden Grotto', count: 1, rarity: 'RARE' },
        { name: 'Hidden Grotto', count: 1, rarity: 'RARE' },
      ],
    )
    expect(entries).toEqual([
      { name: 'Sol Ring', count: 2, rarity: 'MYTHIC' },
      { name: 'Hidden Grotto', count: 1, rarity: 'RARE' },
    ])
    expect(added).toBe(1)
    expect(skipped).toBe(2)
  })
})

describe('leaving the client', () => {
  const cube = {
    name: 'Bloomburrow Extended',
    cards: [
      { name: 'Lightning Bolt', count: 2 },
      { name: 'Hidden Grotto', count: 1, rarity: 'RARE' as const },
    ],
    basicLandSetCode: 'BLB',
    packSize: 15,
    boosterStyle: 'PLAY_BOOSTER' as const,
  }

  it('sends one name per physical card, the style and only the overridden rarities', () => {
    expect(cubeLobbyUpdate(cube)).toEqual({
      cubeCards: ['Lightning Bolt', 'Lightning Bolt', 'Hidden Grotto'],
      cubeName: 'Bloomburrow Extended',
      packSize: 15,
      cubeBasicLandSetCode: 'BLB',
      cubeBoosterStyle: 'PLAY_BOOSTER',
      cubeRarities: { 'Hidden Grotto': 'RARE' },
    })
    expect(rarityOverrides([{ name: 'X', count: 1 }])).toEqual({})
  })

  it('carries style and rarities on a recipe, and leaves them off a plain cube', () => {
    expect(recipeCubeOf(cube, false)).toMatchObject({ boosterStyle: 'PLAY_BOOSTER', rarities: { 'Hidden Grotto': 'RARE' } })
    const plain = recipeCubeOf({ ...cube, cards: [{ name: 'A', count: 1 }], boosterStyle: 'SHUFFLED' }, true)
    expect(plain).toEqual({ name: 'Bloomburrow Extended', cards: ['A'], basicLandSetCode: 'BLB', packSize: 15, poolPlay: true })
  })

  it('writes a text list the paste importer reads back, name included', () => {
    const parsed = parseArenaDeckList(cubeToText(cube))
    expect(parsed.deckName).toBe('Bloomburrow Extended')
    expect(parsed.entries.map((e) => [e.count, e.name])).toEqual([
      [1, 'Hidden Grotto'],
      [2, 'Lightning Bolt'],
    ])
  })

  it('finds a name no saved cube has', () => {
    expect(uniqueCubeName('Cube', ['Other'])).toBe('Cube')
    expect(uniqueCubeName('Cube', ['cube', 'Cube (2)'])).toBe('Cube (3)')
    expect(uniqueCubeName('  ', [])).toBe('Untitled cube')
  })
})
