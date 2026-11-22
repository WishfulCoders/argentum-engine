/**
 * "Play this cube" from outside a lobby — the cube library or someone's share link.
 *
 * Those pages live on their own routes, where there may be no game-server connection yet (a share
 * link opened cold has never connected). So the launch is a {@link LobbyRecipe} parked in a module
 * slot, the page navigates home, and the home screen hands it to `useApplyRecipe` once it is
 * connected — the same one-way handoff `pendingLobbyIntent.ts` uses, one step earlier.
 *
 * The recipe is built by the play panel's own {@link recipeForOptions}, so a cube draft against AI
 * opens with the same seats, shape and auto-start a set draft from the panel would; only the cube
 * rides on top.
 */
import type { SharedCube } from '@/api/account'
import type { LobbyRecipe } from '@/components/lobby/lobbyRecipe'
import { defaultOptions, opponentRange, recipeForOptions } from '@/components/lobby/playModes'
import { recipeCubeOf } from './cubeModel'

export type CubePlayFormat = 'DRAFT' | 'SEALED'
export type CubePlayWith = 'AI' | 'FRIENDS'

export interface CubePlayChoice {
  readonly format: CubePlayFormat
  readonly playWith: CubePlayWith
  /** AI opponents; only read when playing against AI. */
  readonly opponents: number
}

/** The AI-opponent range the play panel offers for this format. */
export function cubeOpponentRange(format: CubePlayFormat): { min: number; max: number; fallback: number } {
  const range = opponentRange(defaultOptions(format, true))
  return range ?? { min: 1, max: 1, fallback: 1 }
}

export function cubePlayRecipe(cube: SharedCube, choice: CubePlayChoice): LobbyRecipe {
  const base = recipeForOptions({
    ...defaultOptions(choice.format, true),
    playWith: choice.playWith,
    opponents: choice.opponents,
  })
  return { ...base, settings: { ...base.settings, cube: recipeCubeOf(cube, false) } }
}

let queued: LobbyRecipe | null = null

/** Park a launch for the home screen. A second call replaces the first — the latest click wins. */
export function queueCubePlay(recipe: LobbyRecipe): void {
  queued = recipe
}

/** Read the parked launch, at most once. */
export function takeQueuedCubePlay(): LobbyRecipe | null {
  const recipe = queued
  queued = null
  return recipe
}
