import React, { useEffect, useRef, useState } from 'react'
import { useGameStore } from '@/store/gameStore.ts'
import { useStackCards } from '@/store/selectors.ts'
import type { AnnouncementMode } from '@/store/slices/ui/playerPrefsSlice.ts'
import type { ClientCard, ClientChosenTarget, ClientGameState } from '@/types/gameState'
import type { EntityId } from '@/types'
import { getCardImageUrl } from '@/utils/cardImages.ts'
import { AbilityText } from '../ui/ManaSymbols'

/**
 * Announcement feed: a toast for each new spell or ability put on the stack — who, what, which
 * modes, which targets. Built from the stack objects themselves, which carry the chosen modes and
 * targets only while they are on the stack; the toast outlives them, so a spell that resolves the
 * moment it is cast (you had nothing to respond with) can still be read. Filtered by the
 * `announcementMode` preference (off / opponent's only / everything).
 */

interface Announcement {
  key: string
  headline: string
  name: string
  imageUrl: string
  isYours: boolean
  lines: { text: string; targets: string | null }[]
  /** Rules text when there are no chosen modes to show. */
  body: string | null
  createdAt: number
  ttl: number
}

const MAX_VISIBLE = 4

function targetName(t: ClientChosenTarget, gs: ClientGameState, viewerId: EntityId | null): string {
  switch (t.type) {
    case 'Player':
      return t.playerId === viewerId ? 'you' : gs.players.find((p) => p.playerId === t.playerId)?.name ?? 'a player'
    case 'Permanent':
      return gs.cards[t.entityId]?.name ?? 'a permanent'
    case 'Spell':
      return gs.cards[t.spellEntityId]?.name ?? 'a spell'
    case 'Card':
      return gs.cards[t.cardId]?.name ?? 'a card'
  }
}

function describe(card: ClientCard, gs: ClientGameState, viewerId: EntityId | null): Announcement {
  const isYours = card.controllerId === viewerId
  const who = isYours ? 'You' : gs.players.length > 2
    ? gs.players.find((p) => p.playerId === card.controllerId)?.name ?? 'Opponent'
    : 'Opponent'
  const isTrigger = card.typeLine === 'Triggered Ability'
  const isAbility = isTrigger || card.typeLine === 'Ability'
  const copy = card.copyIndex != null ? ' a copy of' : ''
  const headline = isTrigger
    ? `${isYours ? 'Your' : `${who}'s`} trigger`
    : isAbility
      ? `${who} activated`
      : `${who} cast${copy}`

  const groups = card.perModeTargets ?? []
  const lines = groups.length > 0
    ? groups.map((g) => ({ text: g.modeDescription, targets: g.targetNames.length > 0 ? g.targetNames.join(', ') : null }))
    : (card.chosenModeDescriptions ?? []).map((text) => ({ text, targets: null }))

  const targetNames = card.targets.map((t) => targetName(t, gs, viewerId))
  const xText = card.chosenX != null ? `X = ${card.chosenX}` : null
  let body: string | null = null
  if (lines.length === 0) {
    const text = isAbility ? card.oracleText : card.stackText
    body = text ? text : null
  }
  if (lines.length === 0 && (targetNames.length > 0 || xText)) {
    lines.push({ text: xText ?? '', targets: targetNames.length > 0 ? targetNames.join(', ') : null })
  }

  const detailed = lines.length > 0
  return {
    key: `${card.id}`,
    headline,
    name: card.name,
    imageUrl: getCardImageUrl(card.name, card.imageUri, 'small'),
    isYours,
    lines,
    body,
    createdAt: Date.now(),
    ttl: detailed ? 8000 : 6000,
  }
}

function shouldAnnounce(card: ClientCard, mode: AnnouncementMode, viewerId: EntityId | null): boolean {
  if (mode === 'off') return false
  if (mode === 'opponent') return card.controllerId !== viewerId
  return true
}

export function StackAnnouncements() {
  const stackCards = useStackCards()
  const mode = useGameStore((s) => s.announcementMode)
  const viewerId = useGameStore((s) => s.playerId)
  const gameState = useGameStore((s) => s.gameState)
  const [items, setItems] = useState<Announcement[]>([])
  const [hovered, setHovered] = useState(false)
  const seen = useRef<Set<EntityId> | null>(null)

  // Toast each stack object the first time it shows up. The first render only records what is
  // already there — joining mid-game (or a reconnect) shouldn't replay the whole stack.
  useEffect(() => {
    if (!gameState) return
    const ids = new Set(stackCards.map((c) => c.id))
    if (seen.current === null) {
      seen.current = ids
      return
    }
    const fresh = stackCards.filter((c) => !seen.current!.has(c.id) && shouldAnnounce(c, mode, viewerId))
    seen.current = ids
    if (fresh.length === 0) return
    setItems((prev) => [...prev, ...fresh.map((c) => describe(c, gameState, viewerId))].slice(-MAX_VISIBLE * 2))
  }, [stackCards, gameState, mode, viewerId])

  // Expire toasts; a pointer resting on the feed holds it.
  useEffect(() => {
    if (items.length === 0 || hovered) return
    const now = Date.now()
    const next = Math.min(...items.map((i) => i.createdAt + i.ttl)) - now
    const t = window.setTimeout(() => {
      const at = Date.now()
      setItems((prev) => prev.filter((i) => i.createdAt + i.ttl > at))
    }, Math.max(50, next))
    return () => window.clearTimeout(t)
  }, [items, hovered])

  // Resuming after a hover restarts the clocks, so nothing vanishes the instant the pointer leaves.
  const onLeave = () => {
    setHovered(false)
    const now = Date.now()
    setItems((prev) => prev.map((i) => ({ ...i, createdAt: now - Math.min(i.ttl - 2500, now - i.createdAt) })))
  }

  if (mode === 'off' || items.length === 0) return null
  const visible = items.slice(-MAX_VISIBLE)

  return (
    <div
      aria-live="polite"
      onMouseEnter={() => setHovered(true)}
      onMouseLeave={onLeave}
      style={styles.feed}
    >
      {visible.map((a) => (
        <div
          key={a.key}
          onClick={() => setItems((prev) => prev.filter((i) => i.key !== a.key))}
          title="Click to dismiss"
          style={{ ...styles.toast, borderLeftColor: a.isYours ? '#5bc0de' : '#e07050' }}
        >
          <img src={a.imageUrl} alt="" style={styles.thumb} />
          <div style={styles.textCol}>
            <div style={{ ...styles.headline, color: a.isYours ? '#8fd3ea' : '#f0a08a' }}>{a.headline}</div>
            <div style={styles.name}>{a.name}</div>
            {a.lines.map((l, i) => (
              <div key={i} style={styles.line}>
                {l.text && (
                  <div style={styles.mode}>
                    <span style={{ color: '#b8a8cc' }}>• </span>
                    <AbilityText text={l.text} size={11} />
                  </div>
                )}
                {l.targets && <div style={styles.targets}>→ {l.targets}</div>}
              </div>
            ))}
            {a.body && (
              <div style={styles.body}>
                <AbilityText text={a.body} size={11} />
              </div>
            )}
          </div>
        </div>
      ))}
    </div>
  )
}

const styles: Record<string, React.CSSProperties> = {
  // Right side, inside the pile column: the stack itself sits at the left-middle (StackZone),
  // and the toast shouldn't cover the object it describes or the library/graveyard piles.
  feed: {
    position: 'fixed',
    right: 'max(12px, min(100px, 7vw))',
    top: '50%',
    transform: 'translateY(-50%)',
    zIndex: 450,
    display: 'flex',
    flexDirection: 'column',
    gap: 6,
    width: 'min(290px, calc(100vw - 24px))',
  },
  toast: {
    display: 'flex',
    gap: 8,
    padding: '7px 9px',
    background: 'rgba(12, 12, 28, 0.94)',
    border: '1px solid rgba(255, 255, 255, 0.12)',
    borderLeft: '3px solid',
    borderRadius: 8,
    boxShadow: '0 4px 14px rgba(0, 0, 0, 0.5)',
    cursor: 'pointer',
    animation: 'argentumToastIn 0.18s ease-out',
  },
  thumb: {
    width: 40,
    height: 56,
    objectFit: 'cover',
    borderRadius: 3,
    flexShrink: 0,
  },
  textCol: { display: 'flex', flexDirection: 'column', gap: 2, minWidth: 0, flex: 1 },
  headline: { fontSize: 10, fontWeight: 700, textTransform: 'uppercase', letterSpacing: 0.6 },
  name: { fontSize: 13, fontWeight: 700, color: '#f2f2f2', lineHeight: 1.2 },
  line: { display: 'flex', flexDirection: 'column', gap: 1 },
  mode: { fontSize: 11, color: '#e0d4f0', lineHeight: 1.35 },
  targets: { fontSize: 11, color: '#ffcc66', fontStyle: 'italic', lineHeight: 1.3 },
  body: {
    fontSize: 11,
    color: '#b8b8c8',
    lineHeight: 1.35,
    overflow: 'hidden',
    display: '-webkit-box',
    WebkitLineClamp: 3,
    WebkitBoxOrient: 'vertical',
  },
}

const ANNOUNCEMENT_LABELS: Record<AnnouncementMode, string> = {
  off: 'Off',
  opponent: "Opponent's",
  all: 'Everything',
}

/**
 * Gear button in the in-game control row: a small popover of client-side gameplay preferences.
 */
export function GameplaySettingsButton({ buttonStyle }: { buttonStyle?: React.CSSProperties | undefined }) {
  const [open, setOpen] = useState(false)
  const mode = useGameStore((s) => s.announcementMode)
  const setMode = useGameStore((s) => s.setAnnouncementMode)
  const ref = useRef<HTMLDivElement>(null)

  useEffect(() => {
    if (!open) return
    const close = (e: MouseEvent) => {
      if (ref.current && !ref.current.contains(e.target as Node)) setOpen(false)
    }
    const esc = (e: KeyboardEvent) => { if (e.key === 'Escape') setOpen(false) }
    window.addEventListener('mousedown', close)
    window.addEventListener('keydown', esc)
    return () => {
      window.removeEventListener('mousedown', close)
      window.removeEventListener('keydown', esc)
    }
  }, [open])

  return (
    <div ref={ref} style={{ position: 'relative', display: 'flex' }}>
      <button
        onClick={() => setOpen((o) => !o)}
        title="Gameplay settings"
        aria-expanded={open}
        style={{ ...buttonStyle, cursor: 'pointer', color: open ? '#fff' : '#bbb', fontSize: 17, lineHeight: 1 }}
      >
        ⚙
      </button>
      {open && (
        <div role="dialog" aria-label="Gameplay settings" style={popover.panel}>
          <div style={popover.title}>Gameplay settings</div>
          <div style={popover.label}>Announce spells &amp; triggers</div>
          <div style={popover.segment}>
            {(Object.keys(ANNOUNCEMENT_LABELS) as AnnouncementMode[]).map((m) => (
              <button
                key={m}
                onClick={() => setMode(m)}
                style={{
                  ...popover.segButton,
                  ...(m === mode ? popover.segActive : {}),
                }}
              >
                {ANNOUNCEMENT_LABELS[m]}
              </button>
            ))}
          </div>
          <div style={popover.hint}>
            A toast on the right for each spell or ability put on the stack, with the modes and
            targets chosen. Hover to hold it, click to dismiss.
          </div>
          <div style={popover.divider} />
          <div style={popover.hint}>
            <b>Hand:</b> drag a card sideways to rearrange it.<br />
            <b>Blocking:</b> drag a creature onto an attacker, or click your creatures and then
            the attacker.
          </div>
        </div>
      )}
    </div>
  )
}

const popover: Record<string, React.CSSProperties> = {
  panel: {
    position: 'absolute',
    bottom: 'calc(100% + 8px)',
    right: 0,
    width: 260,
    padding: 12,
    background: 'rgba(14, 14, 30, 0.97)',
    border: '1px solid #444',
    borderRadius: 8,
    boxShadow: '0 6px 20px rgba(0, 0, 0, 0.6)',
    color: '#ddd',
    fontSize: 12,
    zIndex: 200,
    display: 'flex',
    flexDirection: 'column',
    gap: 8,
  },
  title: { fontSize: 11, fontWeight: 700, textTransform: 'uppercase', letterSpacing: 1, color: '#aaa' },
  label: { fontSize: 12, fontWeight: 600, color: '#eee' },
  segment: { display: 'flex', border: '1px solid #555', borderRadius: 6, overflow: 'hidden' },
  segButton: {
    flex: 1,
    padding: '5px 4px',
    background: 'transparent',
    color: '#aaa',
    border: 'none',
    fontSize: 11,
    cursor: 'pointer',
  },
  segActive: { background: 'rgba(79, 195, 247, 0.9)', color: '#000', fontWeight: 700 },
  hint: { fontSize: 11, color: '#999', lineHeight: 1.4 },
  divider: { height: 1, background: '#333' },
}
