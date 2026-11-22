import { describe, expect, it } from 'vitest'
import { cubePlayRecipe, queueCubePlay, takeQueuedCubePlay } from '../playCube'

const cube = {
  name: 'Set Cube',
  cards: [{ name: 'Hidden Grotto', count: 1, rarity: 'RARE' as const }],
  basicLandSetCode: 'BLB',
  packSize: 15,
  boosterStyle: 'PLAY_BOOSTER' as const,
}

describe('cubePlayRecipe', () => {
  it('opens a booster draft with friends as a group bracket with no AI', () => {
    const recipe = cubePlayRecipe(cube, { format: 'DRAFT', playWith: 'FRIENDS', opponents: 3 })
    expect(recipe.selection.cards).toEqual({ kind: 'DRAFT', shape: 'BOOSTER' })
    expect(recipe.selection.roster).toBe('GROUP')
    expect(recipe.aiSeats).toBe(0)
    expect(recipe.settings.cube).toMatchObject({
      name: 'Set Cube',
      cards: ['Hidden Grotto'],
      boosterStyle: 'PLAY_BOOSTER',
      rarities: { 'Hidden Grotto': 'RARE' },
      poolPlay: false,
    })
  })

  it('fills AI seats and starts by itself against AI', () => {
    const recipe = cubePlayRecipe(cube, { format: 'SEALED', playWith: 'AI', opponents: 2 })
    expect(recipe.selection.cards).toEqual({ kind: 'SEALED', shape: 'STANDARD' })
    expect(recipe.selection.roster).toBe('SOLO')
    expect(recipe.aiSeats).toBe(2)
    expect(recipe.autoStart).toBe(true)
  })
})

describe('the queued launch', () => {
  it('is read at most once', () => {
    const recipe = cubePlayRecipe(cube, { format: 'DRAFT', playWith: 'FRIENDS', opponents: 1 })
    queueCubePlay(recipe)
    expect(takeQueuedCubePlay()).toBe(recipe)
    expect(takeQueuedCubePlay()).toBeNull()
  })
})
