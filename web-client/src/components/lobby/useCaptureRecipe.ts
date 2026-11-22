/**
 * Read the current lobby back out as a {@link LobbyRecipe}.
 *
 * The store half of `recipeFromLobby`, which is pure and therefore can't go and find the two things
 * the lobby broadcast doesn't carry:
 *
 * - **The cube's cards.** `LobbySettings` carries only `cubeName` and `cubeCardCount` — a cube
 *   reaches a lobby as a full name list over `UpdateLobbySettings.cubeCards` and is never sent back
 *   (`V12__cubes.sql` says so, and it is what keeps guests and unsaved cubes working). So the card
 *   list has to come from the local library, matched by name.
 * - **The deck's identity.** A decklist has no name on the wire; the picker knows which saved deck
 *   is loaded and hands the name in.
 *
 * Returns a thunk rather than a value: capture happens at the moment Start is pressed, and computing
 * it on every render would mean rebuilding a whole cube card list per keystroke in the deck picker.
 */
import { useCallback } from 'react'
import type { LobbyState } from '@/store/slices/types'
import type { QuickGameLobbyStateMessage } from '@/types'
import type { SharedCube } from '@/api/account'
import { recipeCubeOf } from '@/components/cube/cubeModel'
import { useUnifiedCubes } from '@/store/useUnifiedCubes'
import { recipeFromLobby, type LobbyRecipe, type RecipeCube, type RecipeDeck } from './lobbyRecipe'
import type { UnifiedLobbyView } from './lobbyViewModel'
import type { DeckPickerTab } from '../ui/DeckPicker'

export function useCaptureRecipe(
  view: UnifiedLobbyView | null,
  lobbyState: LobbyState | null,
  quickLobby: QuickGameLobbyStateMessage | null,
  deckTab: DeckPickerTab | undefined,
  savedDeckName: string | null,
  exampleName: string | null = null,
): () => { recipe: LobbyRecipe; notes: string[] } {
  const { cubes } = useUnifiedCubes()

  return useCallback(() => {
    if (!view) throw new Error('captureRecipe called with no lobby')
    return recipeFromLobby(
      view,
      lobbyState,
      quickLobby,
      deckRefFor(
        deckTab,
        savedDeckName,
        exampleName,
        quickLobby?.players.find((p) => p.playerId === quickLobby.youPlayerId)?.setCodes ?? [],
      ),
      cubeByName(
        cubes,
        lobbyState?.settings.cubeName ?? null,
        lobbyState?.settings.cubePoolPlay ?? false,
      ),
    )
  }, [view, lobbyState, quickLobby, deckTab, savedDeckName, exampleName, cubes])
}

/**
 * What this seat is bringing, as a reference.
 *
 * Random pool is the picker's tab rather than a lobby setting, so the tab *is* the answer for it;
 * everything else is either a named saved deck or nothing worth recording (a pasted list has no
 * identity to bring back, and a pool built inside the event isn't chosen at all).
 */
function deckRefFor(
  tab: DeckPickerTab | undefined,
  savedDeckName: string | null,
  exampleName: string | null,
  /** The sets your Random tab is rolling from; empty = any set. */
  randomSetCodes: readonly string[],
): RecipeDeck {
  if (tab === 'random') {
    return randomSetCodes.length > 0 ? { kind: 'RANDOM', setCodes: [...randomSetCodes] } : { kind: 'RANDOM' }
  }
  if (tab === 'examples' && exampleName) return { kind: 'EXAMPLE', name: exampleName }
  if (savedDeckName) return { kind: 'SAVED', name: savedDeckName }
  return { kind: 'NONE' }
}

function cubeByName(
  cubes: readonly SharedCube[],
  cubeName: string | null,
  /** Pool Play is a lobby setting, not a property of the cube — so it comes from the lobby. */
  poolPlay: boolean,
): RecipeCube | undefined {
  if (!cubeName) return undefined
  const match = cubes.find((c) => c.name.trim().toLowerCase() === cubeName.trim().toLowerCase())
  if (!match) return undefined
  return recipeCubeOf(match, poolPlay)
}
