/**
 * What the player sees while the server can't be reached, on every route (mounted beside the
 * router, like the matchmaking layer).
 *
 * - **Server restarting** (the server announced it, or closed with 1012 — a deploy): on a screen
 *   that can't work without the server — a game, a mulligan, a draft, a spectated game — a card
 *   over the dimmed table says the game is saved and will pick up where it left off. The table
 *   stays visible behind it, so it's clear nothing was lost. Anywhere else (home, deckbuilder,
 *   library…) a slim banner at the bottom says the same without blocking what still works offline.
 * - **Connection lost mid-game** (no restart announced): after a short grace, the same card in a
 *   "reconnecting" voice, with a "Try now" once the backoff has grown.
 * - **Back**: when the server answers after a restart, a brief "update complete" note confirms it.
 */
import { useEffect, useState } from 'react'
import { useLocation } from 'react-router-dom'
import { isAppRoute } from '@/hooks/useHomeVisible'
import { useGameStore } from '@/store/gameStore.ts'
import styles from './ConnectionStatusLayer.module.css'

/** A blip shorter than this (a socket swap, a tab waking up) doesn't deserve a takeover. */
const LOST_GRACE_MS = 2_500
/** When "Try now" appears on a lost connection: by then the backoff waits are getting long. */
const LOST_RETRY_OFFER_MS = 10_000
/** How long the "update complete" note stays up. */
const BACK_NOTICE_MS = 4_000

export default function ConnectionStatusLayer() {
  const { pathname } = useLocation()
  const serverRestart = useGameStore((s) => s.serverRestart)
  const serverBackAt = useGameStore((s) => s.serverBackAt)
  const connectionLostAt = useGameStore((s) => s.connectionLostAt)
  const sessionReplaced = useGameStore((s) => s.sessionReplaced)
  const retryConnection = useGameStore((s) => s.retryConnection)
  // Screens that are dead without the server: the table (or a draft) is frozen until it's back.
  const liveTable = useGameStore((s) =>
    s.gameOverState == null && (
      s.gameState != null ||
      s.mulliganState != null ||
      s.waitingForOpponentMulligan ||
      s.spectatingState != null ||
      s.lobbyState?.state === 'DRAFTING'
    ),
  )
  const blocking = liveTable && isAppRoute(pathname)

  const ticking = serverRestart != null || connectionLostAt != null ||
    (serverBackAt != null && Date.now() - serverBackAt < BACK_NOTICE_MS + 1_000)
  const now = useNow(ticking)

  // The takeover overlay owns the screen when another tab has the session.
  if (sessionReplaced) return null

  if (serverRestart) {
    const elapsedMs = Math.max(0, now - serverRestart.startedAt)
    return blocking
      ? <RestartCard elapsedMs={elapsedMs} expectedSeconds={serverRestart.expectedSeconds} />
      : <RestartBanner elapsedMs={elapsedMs} expectedSeconds={serverRestart.expectedSeconds} />
  }

  if (connectionLostAt != null && blocking) {
    const lostFor = now - connectionLostAt
    if (lostFor < LOST_GRACE_MS) return null
    return <LostCard offerRetry={lostFor >= LOST_RETRY_OFFER_MS} onRetry={retryConnection} />
  }

  if (serverBackAt != null && now - serverBackAt < BACK_NOTICE_MS) {
    return (
      <div className={`${styles.banner} ${styles.bannerBack}`} role="status">
        <span className={styles.check} aria-hidden>✓</span>
        <span><strong>Update complete.</strong> You're back online.</span>
      </div>
    )
  }

  return null
}

function RestartCard({ elapsedMs, expectedSeconds }: { elapsedMs: number; expectedSeconds: number }) {
  const overdue = elapsedMs > expectedSeconds * 1000 * 1.5
  const progress = Math.min(elapsedMs / (expectedSeconds * 1000), 1)
  return (
    <div className={styles.scrim} role="alertdialog" aria-modal="true" aria-labelledby="restart-title" aria-describedby="restart-body">
      <div className={styles.card}>
        <p className={styles.eyebrow}>Server update</p>
        <h2 id="restart-title" className={styles.title}>Installing a new version</h2>
        <p id="restart-body" className={styles.body}>
          Your game is saved and will pick up right where you left off — usually within {quote(expectedSeconds)}.
        </p>
        <div
          className={`${styles.track} ${overdue ? styles.trackWaiting : ''}`}
          role="progressbar"
          aria-label="Time since the update started"
          aria-valuemin={0}
          aria-valuemax={expectedSeconds}
          aria-valuenow={Math.round(elapsedMs / 1000)}
          aria-valuetext={`${formatElapsed(elapsedMs)} elapsed`}
        >
          <div className={styles.fill} style={{ width: `${progress * 100}%` }} />
        </div>
        <div className={styles.statusRow} aria-live="polite">
          <span className={styles.statusText}>
            <span className={styles.pulse} aria-hidden />
            {overdue ? 'Taking a little longer than usual' : 'Reconnecting automatically'}
          </span>
          <span className={styles.elapsed}>{formatElapsed(elapsedMs)}</span>
        </div>
        <p className={styles.hint}>
          {overdue
            ? "We'll keep trying every few seconds. No need to refresh."
            : 'No need to refresh — this page reconnects on its own.'}
        </p>
      </div>
    </div>
  )
}

function RestartBanner({ elapsedMs, expectedSeconds }: { elapsedMs: number; expectedSeconds: number }) {
  return (
    <div className={styles.banner} role="status">
      <span className={styles.pulse} aria-hidden />
      <span className={styles.bannerText}>
        <strong>Server update in progress.</strong>{' '}
        Back in about {quote(expectedSeconds)} — games in progress are saved.
      </span>
      <span className={styles.elapsed}>{formatElapsed(elapsedMs)}</span>
    </div>
  )
}

function LostCard({ offerRetry, onRetry }: { offerRetry: boolean; onRetry: () => void }) {
  return (
    <div className={styles.scrim} role="alertdialog" aria-modal="true" aria-labelledby="lost-title" aria-describedby="lost-body">
      <div className={styles.card}>
        <p className={styles.eyebrow}>Connection lost</p>
        <h2 id="lost-title" className={styles.title}>Reconnecting…</h2>
        <p id="lost-body" className={styles.body}>
          Your game is still on the server. You'll be back at the table as soon as the connection returns.
        </p>
        <div className={`${styles.track} ${styles.trackWaiting}`} aria-hidden>
          <div className={styles.fill} />
        </div>
        {offerRetry && (
          <button type="button" className={styles.retry} onClick={onRetry}>
            Try now
          </button>
        )}
      </div>
    </div>
  )
}

/** "1–2 minutes" for the deploy-sized default; plain minutes or seconds otherwise. */
function quote(seconds: number): string {
  if (seconds <= 45) return 'a minute'
  if (seconds <= 120) return '1–2 minutes'
  return `${Math.ceil(seconds / 60)} minutes`
}

function formatElapsed(ms: number): string {
  const total = Math.floor(ms / 1000)
  return `${Math.floor(total / 60)}:${String(total % 60).padStart(2, '0')}`
}

/** Re-render once a second while [active]; returns the current time. */
function useNow(active: boolean): number {
  const [now, setNow] = useState(() => Date.now())
  useEffect(() => {
    if (!active) return
    setNow(Date.now())
    const id = setInterval(() => setNow(Date.now()), 1000)
    return () => clearInterval(id)
  }, [active])
  return now
}
