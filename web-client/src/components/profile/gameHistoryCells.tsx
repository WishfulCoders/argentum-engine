/**
 * Shared cell renderers for the recent-games tables used by both the signed-in profile and a public
 * profile, so the two tables read identically: the game mode as a "Tournament › Draft" hierarchy, the
 * opponents linked to their public profiles, and each player's ELO at the time of a ranked game.
 */
import type React from 'react'
import { useNavigate } from 'react-router-dom'
import type { GameHistoryEntry } from '@/api/account'
import { gameModeLabel } from '@/components/admin/statFormat'
import a from './account.module.css'

/** The game mode as a top-level category with an optional dimmer variant ("Tournament › Draft"). */
export function GameModeCell({ gameMode, format }: { gameMode: string | null; format: string | null }) {
  const { primary, variant } = gameModeLabel(gameMode, format)
  return (
    <span>
      {primary}
      {variant ? <span style={styles.variant}> › {variant}</span> : null}
    </span>
  )
}

/** Opponent names, each linking to their public profile when the opponent is a signed-in account. */
export function OpponentCell({ entry }: { entry: GameHistoryEntry }) {
  const navigate = useNavigate()
  if (entry.opponentList.length === 0) return <>{entry.opponents ?? '—'}</>
  return (
    <span style={styles.opponents}>
      {entry.opponentList.map((o, i) => (
        <span key={`${o.name}-${i}`}>
          {i > 0 && <span style={styles.sep}>, </span>}
          {o.userId ? (
            <button type="button" className={a.link} onClick={() => navigate(`/u/${o.userId}`)}>
              {o.name}
            </button>
          ) : (
            <span>{o.name}</span>
          )}
        </span>
      ))}
    </span>
  )
}

/**
 * Both players' ELO at the time of a ranked game (you → opponent), plus how the game moved your
 * rating (green +N for a gain, red −N for a loss); a dash for non-ranked games.
 */
export function EloCell({ entry }: { entry: GameHistoryEntry }) {
  if (entry.selfRating == null) return <span style={styles.dim}>—</span>
  const delta = entry.ratingDelta
  return (
    <span style={styles.elo}>
      <span style={styles.eloSelf}>{entry.selfRating}</span>
      {delta != null && delta !== 0 && (
        <span style={delta > 0 ? styles.eloDeltaUp : styles.eloDeltaDown}>
          {delta > 0 ? `+${delta}` : `−${Math.abs(delta)}`}
        </span>
      )}
      {entry.opponentRating != null && (
        <>
          <span style={styles.eloVs}> vs </span>
          <span style={styles.eloOpp}>{entry.opponentRating}</span>
        </>
      )}
    </span>
  )
}

const styles: Record<string, React.CSSProperties> = {
  variant: { color: '#8f98ac' },
  opponents: { color: 'inherit' },
  sep: { color: '#666' },
  dim: { color: '#666' },
  elo: { fontVariantNumeric: 'tabular-nums' },
  eloSelf: { color: '#d5dae6' },
  eloDeltaUp: { color: '#5fd08a', marginLeft: 4, fontWeight: 600 },
  eloDeltaDown: { color: '#e2686b', marginLeft: 4, fontWeight: 600 },
  eloVs: { color: '#666' },
  eloOpp: { color: '#9aa3b8' },
}
