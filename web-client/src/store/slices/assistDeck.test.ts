import { describe, expect, it } from 'vitest'
import { deckKey, sameDeck } from './assistDeck'

describe('sameDeck', () => {
  it('ignores card order and zero land counts', () => {
    expect(
      sameDeck(
        { deck: ['Shock', 'Bear', 'Shock'], landCounts: { Mountain: 8, Forest: 0 } },
        { deck: ['Bear', 'Shock', 'Shock'], landCounts: { Mountain: 8 } },
      ),
    ).toBe(true)
  })

  it('sees a changed copy count or land count', () => {
    const base = { deck: ['Shock', 'Bear'], landCounts: { Mountain: 8 } }
    expect(sameDeck(base, { deck: ['Shock', 'Bear', 'Bear'], landCounts: { Mountain: 8 } })).toBe(false)
    expect(sameDeck(base, { deck: ['Shock', 'Bear'], landCounts: { Mountain: 9 } })).toBe(false)
  })

  it('keeps a card name from colliding with a land entry', () => {
    expect(deckKey({ deck: ['Mountain=8'], landCounts: {} })).not.toBe(
      deckKey({ deck: [], landCounts: { Mountain: 8 } }),
    )
  })
})
