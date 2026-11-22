import { useState } from 'react'
import { createPortal } from 'react-dom'
import type { ClientPlayer } from '@/types'
import { DungeonMap, layoutDungeon } from './DungeonMap'

/**
 * A player's dungeon in the command zone (CR 309): a small tile naming the dungeon and the room
 * their venture marker is on, with a depth track. Clicking it opens the full map. Renders nothing
 * when the player isn't in a dungeon and has completed none.
 */
export function DungeonPanel({ player, compact = false }: { player: ClientPlayer; compact?: boolean }) {
  const [open, setOpen] = useState(false)
  const dungeon = player.dungeon
  const completed = player.completedDungeons ?? []
  if (!dungeon && completed.length === 0) return null

  const current = dungeon ? dungeon.rooms[dungeon.currentRoom] : undefined
  const { depth, rows } = dungeon ? layoutDungeon(dungeon) : { depth: [], rows: [] }
  const level = dungeon ? (depth[dungeon.currentRoom] ?? 0) : 0

  return (
    <>
      <button
        type="button"
        data-testid="dungeon-panel"
        onClick={() => dungeon && setOpen(true)}
        title={dungeon
          ? `${dungeon.name} — ${current?.name}: ${current?.text}\nClick to see the map.`
          : `Completed: ${completed.join(', ')}`}
        style={{
          display: 'flex',
          flexDirection: 'column',
          alignItems: 'stretch',
          gap: 2,
          minWidth: compact ? 92 : 132,
          maxWidth: compact ? 120 : 168,
          padding: compact ? '3px 6px' : '5px 8px',
          borderRadius: 8,
          border: '1px solid rgba(212, 175, 55, 0.55)',
          background: 'linear-gradient(180deg, rgba(58, 45, 20, 0.92), rgba(28, 22, 12, 0.92))',
          color: '#efe6d2',
          cursor: dungeon ? 'pointer' : 'default',
          textAlign: 'left',
          fontFamily: 'inherit',
          boxShadow: '0 2px 8px rgba(0,0,0,0.4)',
        }}
      >
        <span style={{ fontSize: compact ? 8 : 9, letterSpacing: 1, textTransform: 'uppercase', color: '#d4af37', fontWeight: 700 }}>
          {dungeon ? 'Dungeon' : 'Dungeons completed'}
        </span>
        {dungeon && (
          <>
            <span style={{ fontSize: compact ? 10 : 12, fontWeight: 700, lineHeight: 1.15 }}>{dungeon.name}</span>
            <span style={{ fontSize: compact ? 9 : 11, color: '#f0c869', lineHeight: 1.15 }}>🧭 {current?.name}</span>
            <span aria-hidden style={{ display: 'flex', gap: 3, marginTop: 2 }}>
              {rows.map((_, r) => (
                <span
                  key={r}
                  style={{
                    flex: 1,
                    height: 4,
                    borderRadius: 2,
                    background: r <= level ? '#d4af37' : 'rgba(255,255,255,0.15)',
                  }}
                />
              ))}
            </span>
          </>
        )}
        {completed.length > 0 && (
          <span style={{ fontSize: compact ? 8 : 10, color: '#b8f08a', lineHeight: 1.2 }}>
            ✓ {completed.length} completed
          </span>
        )}
      </button>
      {open && dungeon && createPortal(
        <DungeonMapOverlay player={player} onClose={() => setOpen(false)} />,
        document.body,
      )}
    </>
  )
}

/** Full-screen view of one player's dungeon map, opened from [DungeonPanel]. */
function DungeonMapOverlay({ player, onClose }: { player: ClientPlayer; onClose: () => void }) {
  const dungeon = player.dungeon
  if (!dungeon) return null
  return (
    <div
      role="dialog"
      aria-label={`${player.name}'s dungeon`}
      onClick={onClose}
      style={{
        position: 'fixed',
        inset: 0,
        zIndex: 2000,
        background: 'rgba(5, 6, 10, 0.78)',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        overflow: 'auto',
      }}
    >
      <div
        onClick={(e) => e.stopPropagation()}
        style={{
          display: 'flex',
          gap: 24,
          alignItems: 'flex-start',
          padding: 24,
          borderRadius: 14,
          background: 'linear-gradient(180deg, #1f1a12, #120f0a)',
          border: '1px solid rgba(212, 175, 55, 0.5)',
          maxWidth: '96vw',
          flexWrap: 'wrap',
          justifyContent: 'center',
        }}
      >
        <div style={{ display: 'flex', flexDirection: 'column', gap: 12, alignItems: 'center' }}>
          <div style={{ color: '#d4af37', fontWeight: 800, fontSize: 18 }}>
            {player.name} — {dungeon.name}
          </div>
          <DungeonMap dungeon={dungeon} />
          {(player.completedDungeons ?? []).length > 0 && (
            <div style={{ color: '#b8f08a', fontSize: 12 }}>
              Completed: {(player.completedDungeons ?? []).join(', ')}
            </div>
          )}
          <button
            type="button"
            onClick={onClose}
            style={{
              padding: '6px 18px',
              borderRadius: 8,
              border: '1px solid #6a5a3a',
              background: '#2a2216',
              color: '#efe6d2',
              cursor: 'pointer',
            }}
          >
            Close
          </button>
        </div>
        {dungeon.imageUri && (
          <img src={dungeon.imageUri} alt={dungeon.name} style={{ width: 260, borderRadius: 12, boxShadow: '0 4px 18px rgba(0,0,0,0.6)' }} />
        )}
      </div>
    </div>
  )
}
