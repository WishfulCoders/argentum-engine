import { useMemo, useState } from 'react'
import type { ClientDungeon } from '@/types'

/**
 * Lays a dungeon's rooms out in rows by depth: the topmost room is row 0, and a room sits one row
 * below the deepest room that leads to it. Rooms in one row are spread evenly across the width.
 * Pure display geometry — which rooms exist and where they lead comes from the server.
 */
export function layoutDungeon(dungeon: ClientDungeon): { depth: number[]; rows: number[][] } {
  const depth = dungeon.rooms.map(() => 0)
  dungeon.rooms.forEach((room, i) => {
    for (const next of room.leadsTo) depth[next] = Math.max(depth[next] ?? 0, (depth[i] ?? 0) + 1)
  })
  const rows: number[][] = []
  depth.forEach((d, i) => {
    ;(rows[d] ??= []).push(i)
  })
  return { depth, rows }
}

const ROOM_W = 168
const ROOM_H = 76
const ROW_GAP = 34
const COL_GAP = 18

/**
 * A dungeon drawn as its map of rooms (CR 309.4): boxes for rooms, arrows for the paths between
 * them, the venture marker on the current room. When [selectable] is given, those rooms light up
 * and clicking one calls [onSelect] with its index — the room-branch choice (CR 701.49b).
 */
export function DungeonMap({
  dungeon,
  selectable,
  selected,
  onSelect,
  scale = 1,
}: {
  dungeon: ClientDungeon
  selectable?: readonly number[]
  selected?: number | null
  onSelect?: (roomIndex: number) => void
  scale?: number
}) {
  const [hovered, setHovered] = useState<number | null>(null)
  const { rows } = useMemo(() => layoutDungeon(dungeon), [dungeon])
  const roomW = ROOM_W * scale
  const roomH = ROOM_H * scale
  const widest = Math.max(...rows.map((r) => r.length))
  const width = widest * roomW + (widest - 1) * COL_GAP * scale
  const height = rows.length * roomH + (rows.length - 1) * ROW_GAP * scale

  const positions = useMemo(() => {
    const pos: Record<number, { x: number; y: number }> = {}
    rows.forEach((row, r) => {
      const rowWidth = row.length * roomW + (row.length - 1) * COL_GAP * scale
      const start = (width - rowWidth) / 2
      row.forEach((room, c) => {
        pos[room] = { x: start + c * (roomW + COL_GAP * scale), y: r * (roomH + ROW_GAP * scale) }
      })
    })
    return pos
  }, [rows, roomW, roomH, width, scale])

  const selectableSet = new Set(selectable ?? [])

  return (
    <div style={{ position: 'relative', width, height }} data-testid="dungeon-map">
      <svg width={width} height={height} style={{ position: 'absolute', inset: 0, overflow: 'visible' }} aria-hidden>
        <defs>
          <marker id="dungeon-arrow" viewBox="0 0 10 10" refX="9" refY="5" markerWidth="6" markerHeight="6" orient="auto-start-reverse">
            <path d="M 0 0 L 10 5 L 0 10 z" fill="#8a7a5a" />
          </marker>
        </defs>
        {dungeon.rooms.flatMap((room, i) =>
          room.leadsTo.map((next) => {
            const from = positions[i]
            const to = positions[next]
            if (!from || !to) return null
            const lit = i === dungeon.currentRoom && selectableSet.has(next)
            return (
              <line
                key={`${i}-${next}`}
                x1={from.x + roomW / 2}
                y1={from.y + roomH}
                x2={to.x + roomW / 2}
                y2={to.y - 2}
                stroke={lit ? '#f0c869' : '#5e5340'}
                strokeWidth={lit ? 2.5 : 1.5}
                markerEnd="url(#dungeon-arrow)"
              />
            )
          }),
        )}
      </svg>
      {dungeon.rooms.map((room, i) => {
        const pos = positions[i]
        if (!pos) return null
        const isCurrent = i === dungeon.currentRoom
        const canPick = selectableSet.has(i)
        const isSelected = selected === i
        const isHovered = hovered === i
        return (
          <button
            key={i}
            type="button"
            data-room={room.name}
            disabled={!canPick}
            onClick={canPick && onSelect ? () => onSelect(i) : undefined}
            onMouseEnter={() => setHovered(i)}
            onMouseLeave={() => setHovered(null)}
            title={`${room.name} — ${room.text}`}
            style={{
              position: 'absolute',
              left: pos.x,
              top: pos.y,
              width: roomW,
              height: roomH,
              boxSizing: 'border-box',
              padding: `${4 * scale}px ${6 * scale}px`,
              borderRadius: 8 * scale,
              textAlign: 'left',
              cursor: canPick ? 'pointer' : 'default',
              color: '#efe6d2',
              background: isSelected
                ? 'linear-gradient(180deg, #6a5320, #3d2f12)'
                : isCurrent
                  ? 'linear-gradient(180deg, #4a3b1c, #2b2210)'
                  : canPick
                    ? 'linear-gradient(180deg, #2c3a2a, #1b2419)'
                    : 'linear-gradient(180deg, #2a2620, #1b1915)',
              border: isSelected
                ? '2px solid #ffd76a'
                : canPick
                  ? `2px solid ${isHovered ? '#b8f08a' : '#7fc35a'}`
                  : isCurrent
                    ? '2px solid #d4af37'
                    : '1px solid #4a4234',
              boxShadow: canPick
                ? '0 0 12px rgba(127, 195, 90, 0.45)'
                : isCurrent
                  ? '0 0 10px rgba(212, 175, 55, 0.5)'
                  : 'none',
              opacity: !canPick && selectable && !isCurrent ? 0.6 : 1,
              overflow: 'hidden',
              fontFamily: 'inherit',
            }}
          >
            <div style={{ display: 'flex', alignItems: 'center', gap: 4, fontWeight: 700, fontSize: 12 * scale, lineHeight: 1.15 }}>
              {isCurrent && <span aria-label="Venture marker">🧭</span>}
              <span style={{ whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>{room.name}</span>
            </div>
            <div style={{ fontSize: 10 * scale, lineHeight: 1.2, color: '#c9bfa6', marginTop: 2 }}>{room.text}</div>
          </button>
        )
      })}
    </div>
  )
}
