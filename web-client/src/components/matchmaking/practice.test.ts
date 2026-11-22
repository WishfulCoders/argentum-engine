import { describe, expect, it } from 'vitest'
import { lobbyKindFor } from '../lobby/modeMatrix'
import { practiceRecipe } from './practice'

describe('practiceRecipe', () => {
  it('warms up a Random deck search with a random deck against the AI, started at once', () => {
    const recipe = practiceRecipe('RANDOM_DECK', null)
    expect(recipe.selection.roster).toBe('SOLO')
    expect(recipe.deck).toEqual({ kind: 'RANDOM' })
    expect(recipe.autoStart).toBe(true)
    expect(lobbyKindFor(recipe.selection)).toBe('QUICK')
  })

  it('warms up Momir with Momir', () => {
    const recipe = practiceRecipe('MOMIR_BASIC', null)
    expect(recipe.selection.cards.kind).toBe('MOMIR')
    expect(lobbyKindFor(recipe.selection)).toBe('QUICK')
  })

  it("warms up a Constructed search under that format's legality", () => {
    const recipe = practiceRecipe('CONSTRUCTED', 'PAUPER')
    expect(recipe.settings.deckFormat).toBe('PAUPER')
    expect(lobbyKindFor(recipe.selection)).toBe('QUICK')
  })

  it('keeps every warm-up in a quick lobby, which is what keeps the player queued', () => {
    for (const recipe of [practiceRecipe('JUMP_IN', null), practiceRecipe('CONSTRUCTED', 'COMMANDER')]) {
      expect(recipe.deck).toEqual({ kind: 'RANDOM' })
      expect(lobbyKindFor(recipe.selection)).toBe('QUICK')
    }
  })
})
