import { describe, expect, it } from 'vitest'
import { deckCover, deckTileModel, type DeckTileCard } from '../DeckTile'
import { applyDeckDetails, DECK_NOTE_MAX_LENGTH, type SavedDeck } from '@/store/deckLibrary'

const card = (name: string, rarity: string, extra: Partial<DeckTileCard> = {}): DeckTileCard => ({
  name, cmc: 3, colors: ['RED'], cardTypes: ['CREATURE'], basicLand: false, rarity,
  imageUri: `https://cards.scryfall.io/normal/front/a/b/${name}.jpg`, ...extra,
})

const catalog: Record<string, DeckTileCard> = {
  Bolt: card('Bolt', 'COMMON', { colors: ['RED'] }),
  Dragon: card('Dragon', 'MYTHIC'),
  Elf: card('Elf', 'UNCOMMON', { colors: ['GREEN'] }),
  Mountain: card('Mountain', 'COMMON', { basicLand: true, colors: [], cardTypes: ['LAND'] }),
}

describe('deckCover', () => {
  it('falls back to the rarest card when no cover is chosen', () => {
    expect(deckCover({ cards: { Bolt: 4, Dragon: 1 } }, catalog)?.name).toBe('Dragon')
  })

  it('uses the chosen cover while it is still in the deck', () => {
    expect(deckCover({ cards: { Bolt: 4, Dragon: 1 }, coverCard: 'Bolt' }, catalog)?.name).toBe('Bolt')
  })

  it('ignores a chosen cover that left the deck', () => {
    expect(deckCover({ cards: { Elf: 4, Dragon: 1 }, coverCard: 'Bolt' }, catalog)?.name).toBe('Dragon')
  })

  it("paints the chosen printing's stored art over the catalogue default", () => {
    const art = 'https://cards.scryfall.io/normal/front/c/d/alt.jpg'
    expect(deckCover({ cards: { Bolt: 4 }, coverCard: 'Bolt', coverImageUri: art }, catalog)?.imageUri).toBe(art)
  })

  it('paints a chosen cover from its stored art before the catalogue has loaded', () => {
    const art = 'https://cards.scryfall.io/normal/front/c/d/alt.jpg'
    const hero = deckCover({ cards: { Bolt: 4 }, coverCard: 'Bolt', coverImageUri: art }, {})
    expect(hero?.name).toBe('Bolt')
    expect(hero?.imageUri).toBe(art)
  })

  it('can choose the commander, which saved decks keep out of cards', () => {
    expect(deckCover({ cards: { Dragon: 1 }, commander: 'Elf', coverCard: 'Elf' }, catalog)?.name).toBe('Elf')
  })
})

describe('deckTileModel', () => {
  it('counts the commander and orders colours by weight', () => {
    const model = deckTileModel({ cards: { Bolt: 4, Mountain: 20 }, commander: 'Elf' }, catalog)
    expect(model.total).toBe(25)
    expect(model.colors).toEqual(['RED', 'GREEN'])
  })

  it('counts both commanders of a partner pair, and keeps a cover chosen from either', () => {
    const deck = { cards: { Bolt: 4, Mountain: 20 }, commander: 'Elf', secondCommander: 'Dragon', coverCard: 'Dragon' }
    expect(deckTileModel(deck, catalog).total).toBe(26)
    expect(deckCover({ ...deck, coverCard: 'Elf' }, catalog)?.name).toBe('Elf')
  })
})

describe('applyDeckDetails', () => {
  const deck: SavedDeck = { id: 'd', name: 'Burn', cards: { Bolt: 4 }, note: 'old', coverCard: 'Bolt', coverImageUri: 'x', updatedAt: 0 }

  it('trims, caps and clears the note', () => {
    expect(applyDeckDetails(deck, { note: '  go fast  ' }).note).toBe('go fast')
    expect(applyDeckDetails(deck, { note: 'x'.repeat(500) }).note).toHaveLength(DECK_NOTE_MAX_LENGTH)
    expect('note' in applyDeckDetails(deck, { note: '' })).toBe(false)
  })

  it('keeps the old name when the new one is blank', () => {
    expect(applyDeckDetails(deck, { name: '   ' }).name).toBe('Burn')
  })

  it('drops the stored art when the cover goes back to automatic', () => {
    const next = applyDeckDetails(deck, { coverCard: null })
    expect(next.coverCard).toBeUndefined()
    expect(next.coverImageUri).toBeUndefined()
  })
})
