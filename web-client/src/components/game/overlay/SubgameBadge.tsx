import React from 'react'
import { createPortal } from 'react-dom'
import type { ClientGameState, ClientSubgame, ClientWaitingGame, EntityId } from '@/types'
import styles from './SubgameBadge.module.css'

/**
 * A **Magic subgame** in progress (Shahrazad, CR 729) — a whole separate game played inside the
 * main one. Everything on the board is the subgame's, so the player needs one unmistakable cue that
 * they're in it and a place to see what's waiting outside it.
 *
 * The badge sits in the center HUD beside the step strip, like the day/night badge: a single fact
 * about the whole table. Hovering — or tapping, on touch screens — opens a panel with the spell that
 * started it, its rules text (what happens when the subgame is over), and every waiting game's life
 * totals. Purely presentational: it renders `ClientGameState.subgame`, which the server sends.
 */
export function SubgameBadge({ gameState }: { gameState: ClientGameState }) {
  const subgame = gameState.subgame
  const badgeRef = React.useRef<HTMLButtonElement>(null)
  const [hoverAnchor, setHoverAnchor] = React.useState<DOMRect | null>(null)
  const [pinned, setPinned] = React.useState(false)

  React.useEffect(() => {
    if (!pinned) return
    const close = (e: PointerEvent) => {
      if (!badgeRef.current?.contains(e.target as Node)) setPinned(false)
    }
    window.addEventListener('pointerdown', close)
    return () => window.removeEventListener('pointerdown', close)
  }, [pinned])

  if (!subgame) return null
  const source = subgame.waitingGames[subgame.waitingGames.length - 1]
  const anchor = hoverAnchor ?? (pinned ? badgeRef.current?.getBoundingClientRect() ?? null : null)

  return (
    <button
      ref={badgeRef}
      type="button"
      className={styles.badge}
      aria-label={`Subgame${source?.sourceName ? ` of ${source.sourceName}` : ''} in progress`}
      aria-expanded={anchor != null}
      data-testid="subgame-badge"
      onMouseEnter={() => setHoverAnchor(badgeRef.current?.getBoundingClientRect() ?? null)}
      onMouseLeave={() => setHoverAnchor(null)}
      onClick={() => setPinned((p) => !p)}
    >
      <NestedFramesGlyph depth={subgame.depth} />
      <span>Subgame</span>
      {subgame.depth > 1 && <span className={styles.depth}>{subgame.depth} deep</span>}
      {anchor && <SubgamePanel anchor={anchor} subgame={subgame} gameState={gameState} />}
    </button>
  )
}

/** Frames within a frame — one ring per level of nesting. */
function NestedFramesGlyph({ depth }: { depth: number }) {
  const rings = Math.min(depth, 3) + 1
  return (
    <svg className={styles.glyph} viewBox="0 0 16 16" aria-hidden>
      {Array.from({ length: rings }, (_, i) => {
        const inset = 1 + i * (6 / rings)
        return (
          <rect
            key={i}
            x={inset}
            y={inset}
            width={16 - inset * 2}
            height={16 - inset * 2}
            rx={1.5}
            fill={i === rings - 1 ? 'currentColor' : 'none'}
            stroke="currentColor"
            strokeWidth={1.2}
            opacity={i === rings - 1 ? 0.9 : 0.55 + i * 0.15}
          />
        )
      })}
    </svg>
  )
}

const PANEL_WIDTH = 320
const VIEWPORT_PADDING = 8

function SubgamePanel({ anchor, subgame, gameState }: {
  anchor: DOMRect
  subgame: ClientSubgame
  gameState: ClientGameState
}) {
  const vw = window.innerWidth
  const rawLeft = anchor.left + anchor.width / 2 - PANEL_WIDTH / 2
  const left = Math.max(VIEWPORT_PADDING, Math.min(rawLeft, vw - PANEL_WIDTH - VIEWPORT_PADDING))
  const openAbove = anchor.top > window.innerHeight / 2
  const creator = subgame.waitingGames[subgame.waitingGames.length - 1]
  const nameOf = (id: EntityId) =>
    id === gameState.viewingPlayerId
      ? 'You'
      : gameState.players.find((p) => p.playerId === id)?.name ?? 'Opponent'

  return createPortal(
    <div
      role="dialog"
      aria-label="Subgame details"
      className={styles.panel}
      style={{
        left,
        width: PANEL_WIDTH,
        ...(openAbove ? { bottom: window.innerHeight - anchor.top + 8 } : { top: anchor.bottom + 8 }),
      }}
      onPointerDown={(e) => e.stopPropagation()}
    >
      <div className={styles.header}>
        {creator?.sourceImageUri && (
          <img className={styles.art} src={creator.sourceImageUri} alt={creator.sourceName ?? ''} />
        )}
        <div className={styles.headerText}>
          <span className={styles.eyebrow}>
            {subgame.depth > 1 ? `Subgame within a subgame · ${subgame.depth} deep` : 'Subgame'}
          </span>
          {creator?.sourceName && <span className={styles.title}>{creator.sourceName}</span>}
          <span className={styles.lede}>
            A separate game of Magic. Each player's library became their deck; nothing else came
            along, and nothing here affects the game{subgame.depth > 1 ? 's' : ''} waiting outside.
          </span>
        </div>
      </div>

      {creator?.sourceOracleText && <p className={styles.oracle}>{creator.sourceOracleText}</p>}

      {/* Innermost first: the game this one returns to matters most. */}
      {subgame.waitingGames.map((game, level) => ({ game, level })).reverse().map(({ game, level }) => (
        <WaitingGame
          key={level}
          game={game}
          label={level === 0 ? 'Main game — waiting' : `Subgame ${level} — waiting`}
          nameOf={nameOf}
        />
      ))}

      <p className={styles.footnote}>
        When this game ends, every card in it goes back into its owner's library in the game that
        created it, and that library is shuffled.
      </p>
    </div>,
    document.body,
  )
}

function WaitingGame({ game, label, nameOf }: {
  game: ClientWaitingGame
  label: string
  nameOf: (id: EntityId) => string
}) {
  return (
    <div className={styles.waiting}>
      <span className={styles.waitingLabel}>{label}</span>
      <ul className={styles.lifeList}>
        {game.players.map((p) => (
          <li key={p.playerId} className={p.hasLost ? styles.lost : undefined}>
            <span className={styles.playerName}>{nameOf(p.playerId)}</span>
            <span className={styles.life} aria-label={`${p.life} life`}>
              <span aria-hidden className={styles.heart}>♥</span>
              {p.life}
            </span>
          </li>
        ))}
      </ul>
    </div>
  )
}

/**
 * A faint gilded frame around the whole table while a subgame is played — the board itself reads
 * as "a game inside the game" at a glance, without covering anything. Pointer-transparent.
 */
export function SubgameFrame({ depth }: { depth: number | null | undefined }) {
  if (!depth) return null
  return <div className={styles.frame} data-depth={Math.min(depth, 3)} aria-hidden />
}
