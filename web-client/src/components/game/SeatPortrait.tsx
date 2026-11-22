/**
 * A seat's portrait at badge size — the face that rides beside a player's name wherever the table
 * names them (spectator name labels, the spectate/replay header, the multiplayer rail, the
 * "is choosing…" indicator). The HUD life medallion is the big version; this is the small one.
 *
 * Reads the seat's avatar from the store (`avatarByPlayerId`, stamped from the seat roster in live
 * play, spectating and replays alike) and renders nothing when the seat has no picture, so a guest
 * keeps the plain label it always had rather than a letter in a circle.
 */
import type React from 'react'
import { useGameStore } from '@/store/gameStore.ts'
import type { EntityId } from '@/types'
import { AvatarArt, hasAvatarArt } from '../profile/AvatarArt'

/** True when this seat has a portrait to show. */
export function useHasSeatPortrait(playerId: EntityId | string | null | undefined): boolean {
  return useGameStore((s) => (playerId ? hasAvatarArt(s.avatarByPlayerId[playerId as EntityId]) : false))
}

export function SeatPortrait({
  playerId,
  size,
  ringColor = 'rgba(255, 255, 255, 0.22)',
  ringWidth = 1.5,
  style,
}: {
  playerId: EntityId | string | null | undefined
  size: number
  /** The ring around the face — pass the seat's identity colour where the label carries one. */
  ringColor?: string
  ringWidth?: number
  style?: React.CSSProperties
}) {
  const avatar = useGameStore((s) => (playerId ? s.avatarByPlayerId[playerId as EntityId] : undefined))
  if (!hasAvatarArt(avatar)) return null
  return (
    <span
      aria-hidden
      data-seat-portrait={playerId}
      style={{
        position: 'relative',
        display: 'inline-block',
        flexShrink: 0,
        width: size,
        height: size,
        borderRadius: '50%',
        boxShadow: `0 0 0 ${ringWidth}px ${ringColor}, 0 2px 6px rgba(0, 0, 0, 0.5)`,
        ...style,
      }}
    >
      <AvatarArt avatar={avatar} />
    </span>
  )
}
