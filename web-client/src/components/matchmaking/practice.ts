/**
 * "Play the AI while you wait": the warm-up game a searching player can start without leaving the
 * queue. The server keeps a player in a vs-AI lobby or game searching, and when a match is found and
 * accepted it ends the warm-up quietly and seats them in the matched game.
 *
 * The warm-up follows the queue where a 1v1 quick game against the AI can: Momir warms up with Momir,
 * Constructed opens an AI lobby under that format's legality, where the player picks the deck they're
 * about to queue with. Everything else — a Random deck search, Jump In (whose pack choice is a
 * tournament lobby, which would take the player out of the queue), and commander-shaped formats (no
 * 1v1 quick lobby) — warms up with a random deck, which starts at once.
 */
import type { DeckFormat, MatchmakingMode } from '@/types'
import type { LobbyRecipe } from '../lobby/lobbyRecipe'
import { defaultOptions, recipeForOptions } from '../lobby/playModes'

const COMMANDER_SHAPED: ReadonlySet<DeckFormat> = new Set<DeckFormat>(['COMMANDER', 'BRAWL', 'STANDARD_BRAWL'])

type Warmup = 'RANDOM' | 'MOMIR' | 'CONSTRUCTED'

function warmupFor(mode: MatchmakingMode | null, format: DeckFormat | null): Warmup {
  if (mode === 'MOMIR_BASIC') return 'MOMIR'
  const constructed = mode === 'CONSTRUCTED' || (mode === null && format !== null)
  if (constructed && format !== null && !COMMANDER_SHAPED.has(format)) return 'CONSTRUCTED'
  return 'RANDOM'
}

export function practiceRecipe(mode: MatchmakingMode | null, format: DeckFormat | null): LobbyRecipe {
  switch (warmupFor(mode, format)) {
    case 'MOMIR':
      return recipeForOptions(defaultOptions('MOMIR', true))
    case 'CONSTRUCTED': {
      const base = recipeForOptions(defaultOptions('CONSTRUCTED', true))
      return { ...base, settings: { ...base.settings, deckFormat: format } }
    }
    case 'RANDOM':
      return recipeForOptions({ ...defaultOptions('RANDOM', true), deck: { kind: 'RANDOM' } })
  }
}

/** What the warm-up button promises, so it never starts something the player didn't expect. */
export function practiceCaption(mode: MatchmakingMode | null, format: DeckFormat | null): string {
  switch (warmupFor(mode, format)) {
    case 'MOMIR': return 'Momir Basic · you stay in the queue'
    case 'CONSTRUCTED': return 'Pick a deck · you stay in the queue'
    case 'RANDOM': return 'Random deck · you stay in the queue'
  }
}
