import { useMemo } from 'react'
import { useGameStore } from '@/store/gameStore.ts'
import {
  groupCards,
  useBattlefieldCards,
  useSplitOutTargetIds,
  type GroupedCard,
} from '@/store/selectors.ts'
import type { EntityId } from '@/types'
import type { BoardStats } from './battlefieldLayout'
import { rowStats } from './rowStats'

/** One battlefield's permanents grouped into rendered stacks, plus the footprint stats the sizing solver needs. */
export interface BoardGroups {
  lands: readonly GroupedCard[]
  creatures: readonly GroupedCard[]
  planeswalkers: readonly GroupedCard[]
  other: readonly GroupedCard[]
  stats: BoardStats
}

/**
 * Groups one side's permanents into stacks (see `groupCards`) and derives the
 * row stats. Used by `Battlefield` to render, and by `GameBoard`'s pooled
 * two-player solve, which needs both sides' stats before either battlefield
 * renders — grouping twice per update is far cheaper than plumbing the groups
 * up through the board.
 *
 * `isOpponent` + `playerId` follow `useBattlefieldCards`: an opponent board
 * scoped to one seat (multiplayer strip cells) or, omitted, every non-viewing
 * seat; a player-side board optionally showing another seat's permanents.
 *
 * Memoized so the arrays keep stable identity across unrelated store updates —
 * otherwise every re-render allocates fresh arrays that cascade into child
 * re-renders and invalidate downstream useMemos. Permanents that are chosen
 * targets / triggering sources keep their own card so their targeting arrows
 * can anchor (a member hidden behind the stack render cap would drop its
 * arrow) — see `useSplitOutTargetIds` / `groupCards`.
 */
export function useBoardGroups(isOpponent: boolean, playerId?: EntityId): BoardGroups {
  const cards = useBattlefieldCards(isOpponent ? playerId : undefined, isOpponent ? undefined : playerId)
  const lands = isOpponent ? cards.opponentLands : cards.playerLands
  const creatures = isOpponent ? cards.opponentCreatures : cards.playerCreatures
  const planeswalkers = isOpponent ? cards.opponentPlaneswalkers : cards.playerPlaneswalkers
  const other = isOpponent ? cards.opponentOther : cards.playerOther

  const splitOutIds = useSplitOutTargetIds()
  const expanded = useGameStore((state) => state.expandedStackCardIds)
  const groupedLands = useMemo(() => groupCards(lands, splitOutIds), [lands, splitOutIds])
  const groupedCreatures = useMemo(() => groupCards(creatures, splitOutIds), [creatures, splitOutIds])
  const groupedPlaneswalkers = useMemo(() => groupCards(planeswalkers, splitOutIds), [planeswalkers, splitOutIds])
  const groupedOther = useMemo(() => groupCards(other, splitOutIds), [other, splitOutIds])

  const stats = useMemo<BoardStats>(
    () => ({
      front: rowStats(expanded, groupedCreatures, groupedPlaneswalkers),
      back: rowStats(expanded, groupedLands, groupedOther),
    }),
    [expanded, groupedCreatures, groupedPlaneswalkers, groupedLands, groupedOther],
  )

  return useMemo(
    () => ({
      lands: groupedLands,
      creatures: groupedCreatures,
      planeswalkers: groupedPlaneswalkers,
      other: groupedOther,
      stats,
    }),
    [groupedLands, groupedCreatures, groupedPlaneswalkers, groupedOther, stats],
  )
}
