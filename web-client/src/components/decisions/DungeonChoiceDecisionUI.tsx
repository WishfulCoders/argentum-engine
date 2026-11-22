import { useState } from 'react'
import { useGameStore } from '@/store/gameStore.ts'
import type { ChooseOptionDecision } from '@/types'
import { DungeonMap } from '../game/dungeon/DungeonMap'
import styles from './DecisionUI.module.css'

/** Option-metadata id prefixes the server stamps on venture choices (DungeonService). */
const DUNGEON_PREFIX = 'dungeon:'
const ROOM_PREFIX = 'dungeon-room:'

/** Which venture choice, if any, a [ChooseOptionDecision] is — read off the server's option ids. */
export function dungeonChoiceKind(decision: ChooseOptionDecision): 'dungeon' | 'room' | null {
  const meta = decision.optionMetadata
  if (!meta || meta.length !== decision.options.length || meta.length === 0) return null
  if (meta.every((m) => m.id?.startsWith(DUNGEON_PREFIX))) return 'dungeon'
  if (meta.every((m) => m.id?.startsWith(ROOM_PREFIX))) return 'room'
  return null
}

/**
 * The two choices venturing into the dungeon asks for (CR 701.49):
 *
 * - **which dungeon** to enter — the dungeon cards shown side by side, pick one;
 * - **which room** to move to at a fork — the dungeon's map, with the reachable rooms lit up to click.
 *
 * The options and their order are the server's; this only draws them and answers with the index.
 */
export function DungeonChoiceDecisionUI({ decision }: { decision: ChooseOptionDecision }) {
  const kind = dungeonChoiceKind(decision)
  const submitOptionDecision = useGameStore((s) => s.submitOptionDecision)
  const players = useGameStore((s) => s.gameState?.players)
  const [selected, setSelected] = useState<number | null>(null)
  const [minimized, setMinimized] = useState(false)

  if (minimized) {
    return (
      <button className={styles.floatingReturnButton} onClick={() => setMinimized(false)}>
        Return to {decision.prompt}
      </button>
    )
  }

  const confirm = () => {
    if (selected !== null) submitOptionDecision(decision.id, selected)
  }

  const dungeon = kind === 'room' ? players?.find((p) => p.playerId === decision.playerId)?.dungeon : undefined
  const roomIndices = (decision.optionMetadata ?? []).map((m) => Number(m.id?.slice(ROOM_PREFIX.length)))

  return (
    <div className={styles.overlay} data-testid={kind === 'room' ? 'dungeon-room-choice' : 'dungeon-choice'}>
      <h2 className={styles.title}>{decision.prompt}</h2>
      {decision.context.sourceName && <p className={styles.sourceLabel}>{decision.context.sourceName}</p>}

      {kind === 'dungeon' && (
        <div style={{ display: 'flex', gap: 18, flexWrap: 'wrap', justifyContent: 'center', margin: '16px 0' }}>
          {decision.options.map((name, i) => {
            const meta = decision.optionMetadata?.[i]
            const isSelected = selected === i
            return (
              <button
                key={name}
                type="button"
                data-dungeon={name}
                onClick={() => setSelected(i)}
                onDoubleClick={() => submitOptionDecision(decision.id, i)}
                style={{
                  display: 'flex',
                  flexDirection: 'column',
                  alignItems: 'center',
                  gap: 6,
                  width: 230,
                  padding: 8,
                  borderRadius: 14,
                  cursor: 'pointer',
                  background: isSelected ? 'rgba(212, 175, 55, 0.18)' : 'rgba(20, 18, 14, 0.85)',
                  border: isSelected ? '2px solid #ffd76a' : '1px solid rgba(212, 175, 55, 0.35)',
                  boxShadow: isSelected ? '0 0 18px rgba(255, 215, 106, 0.45)' : 'none',
                  color: '#efe6d2',
                  fontFamily: 'inherit',
                }}
              >
                {meta?.imageUri ? (
                  <img src={meta.imageUri} alt={name} style={{ width: 210, borderRadius: 10 }} />
                ) : (
                  <span style={{ fontWeight: 700 }}>{name}</span>
                )}
                {meta?.description && (
                  <span style={{ fontSize: 11, color: '#c9bfa6', lineHeight: 1.3 }}>{meta.description}</span>
                )}
              </button>
            )
          })}
        </div>
      )}

      {kind === 'room' && dungeon && (
        <div
          style={{
            margin: '16px 0',
            padding: 20,
            borderRadius: 14,
            background: 'linear-gradient(180deg, rgba(31, 26, 18, 0.97), rgba(18, 15, 10, 0.97))',
            border: '1px solid rgba(212, 175, 55, 0.45)',
          }}
        >
          <DungeonMap
            dungeon={dungeon}
            selectable={roomIndices}
            selected={selected === null ? null : roomIndices[selected] ?? null}
            onSelect={(room) => {
              const idx = roomIndices.indexOf(room)
              if (idx >= 0) setSelected(idx)
            }}
          />
        </div>
      )}

      {kind === 'room' && !dungeon && (
        <div style={{ display: 'flex', gap: 10, margin: '16px 0' }}>
          {decision.options.map((name, i) => (
            <button key={name} type="button" className={styles.confirmButton} onClick={() => setSelected(i)}>
              {name}
            </button>
          ))}
        </div>
      )}

      <div className={styles.optionButtonRow}>
        <button onClick={() => setMinimized(true)} className={styles.viewBattlefieldButton}>
          View Battlefield
        </button>
        <button onClick={confirm} disabled={selected === null} className={styles.confirmButton}>
          {selected === null
            ? kind === 'room' ? 'Choose a room' : 'Choose a dungeon'
            : `Venture into ${decision.options[selected]}`}
        </button>
      </div>
    </div>
  )
}
