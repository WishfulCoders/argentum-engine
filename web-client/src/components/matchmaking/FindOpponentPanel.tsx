/**
 * "Find an opponent" — the home hub's way to play someone you don't know. A row of game modes, one
 * line on what the chosen mode is, and a Search button (with casual / ranked and, for Constructed, a
 * format beside it); while searching, the whole picker becomes the status line. The server pairs you
 * with another searcher and {@link MatchFoundDialog} asks both of you to accept. The panel only
 * captures intent and mirrors the server's queue state.
 */
import { useEffect, useRef, useState, type KeyboardEvent } from 'react'
import { useGameStore } from '@/store/gameStore.ts'
import { useAuthStore } from '@/store/authStore'
import type { MatchmakingMode, MatchmakingQueueCount } from '@/types'
import styles from './Matchmaking.module.css'
import { useApplyRecipe } from '../lobby/useApplyRecipe'
import { practiceCaption, practiceRecipe } from './practice'
import {
  QUEUE_FORMATS,
  QUEUE_MODES,
  activeQueues,
  formatWait,
  normaliseQueue,
  queueLabel,
  queueMode,
  searchingIn,
  searchingInMode,
  type QueueChoice,
} from './queues'

const STORAGE_KEY = 'argentum-matchmaking-choice'

/** Enough to show where the players are without turning the panel into a lobby list. */
const MAX_WAITING_ROWS = 3

function loadChoice(): QueueChoice {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    if (raw) {
      const parsed = JSON.parse(raw) as Partial<QueueChoice>
      // A choice saved before modes existed is a format (Constructed) or nothing (Random deck).
      const mode = parsed.mode ?? (parsed.format ? 'CONSTRUCTED' : undefined)
      return normaliseQueue({ ...parsed, mode })
    }
  } catch { /* storage unavailable — fall back to the default */ }
  return normaliseQueue({})
}

function saveChoice(choice: QueueChoice) {
  try { localStorage.setItem(STORAGE_KEY, JSON.stringify(choice)) } catch { /* not essential */ }
}

export function FindOpponentPanel({ onSignIn }: { onSignIn: () => void }) {
  const status = useGameStore((s) => s.matchmaking)
  const counts = useGameStore((s) => s.matchmakingQueues)
  const joinMatchmaking = useGameStore((s) => s.joinMatchmaking)
  const leaveMatchmaking = useGameStore((s) => s.leaveMatchmaking)
  const dismissNotice = useGameStore((s) => s.dismissMatchmakingNotice)
  const accountsEnabled = useAuthStore((s) => s.accountsEnabled)
  const signedIn = useAuthStore((s) => s.status === 'authenticated')
  const aiEnabled = useGameStore((s) => s.aiEnabled)
  const applyRecipe = useApplyRecipe()
  const [choice, setChoice] = useState<QueueChoice>(loadChoice)

  // First paint; after this the server pushes every change.
  useEffect(() => {
    if (counts !== null) return
    let cancelled = false
    fetch('/api/matchmaking/queues')
      .then((res) => (res.ok ? res.json() as Promise<MatchmakingQueueCount[]> : null))
      .then((data) => { if (!cancelled && data) useGameStore.setState({ matchmakingQueues: data }) })
      .catch(() => { /* the socket push fills it in */ })
    return () => { cancelled = true }
  }, [counts])

  const rankedAvailable = accountsEnabled && signedIn
  // The stored choice keeps "ranked" while a guest, so signing in brings it back.
  const effective: QueueChoice = { ...choice, ranked: choice.ranked && rankedAvailable && queueMode(choice.mode).rankable }
  const mode = queueMode(choice.mode)
  const choose = (next: Partial<QueueChoice>) => {
    // Keep the ranked wish across a mode that has no ladder, so switching back restores it.
    const merged = { ...choice, ...next }
    const normalised = { ...normaliseQueue(merged), ranked: merged.ranked }
    setChoice(normalised)
    saveChoice(normalised)
  }

  const searching = status?.searching === true
  // Before you search, everyone counted is someone else — a game you can have right now. Shown
  // first and in green, because joining their queue pairs you at once.
  const waiting = searching ? [] : activeQueues(counts).slice(0, MAX_WAITING_ROWS)
  const play = (queue: QueueChoice) => {
    choose(queue)
    joinMatchmaking(queue.mode, queue.format, queue.ranked)
  }

  return (
    <section className={styles.panel} aria-labelledby="find-opponent-title" data-testid="find-opponent">
      <span id="find-opponent-title" className={styles.title}>Find an opponent</span>

      {waiting.length > 0 && (
        <ul className={styles.waitingList} aria-label="Players waiting for a game">
          {waiting.map((q) => {
            const needsSignIn = q.ranked && !rankedAvailable
            const label = queueLabel(q.mode, q.format, q.ranked)
            return (
              <li key={`${q.mode}-${q.format ?? ''}-${q.ranked}`} className={styles.waitingRow} data-testid="waiting-queue">
                <span className={styles.waitingDot} aria-hidden />
                <span className={styles.waitingText}>
                  <span className={styles.waitingQueue}>{label}</span>
                  <span className={styles.waitingCount}>
                    {q.searching === 1 ? '1 player waiting' : `${q.searching} players waiting`}
                  </span>
                </span>
                <button
                  type="button"
                  className={styles.play}
                  onClick={() => (needsSignIn
                    ? onSignIn()
                    : play({ mode: q.mode, format: q.format ?? null, ranked: q.ranked }))}
                  title={needsSignIn ? 'Sign in to play ranked' : `Play ${label} now`}
                >
                  {needsSignIn ? 'Sign in' : 'Play'}
                </button>
              </li>
            )
          })}
        </ul>
      )}

      {searching ? (
        <>
          <SearchingRow
            label={queueLabel(status.mode, status.format, status.ranked)}
            since={status.searchingSince ?? null}
            others={Math.max(0, searchingIn(counts, normaliseQueue({
              mode: status.mode ?? undefined,
              format: status.format ?? null,
              ranked: status.ranked ?? false,
            })) - 1)}
            onCancel={leaveMatchmaking}
          />
          {aiEnabled && (
            <button
              type="button"
              className={styles.practice}
              onClick={() => applyRecipe(practiceRecipe(status.mode ?? null, status.format ?? null))}
              data-testid="find-opponent-practice"
            >
              <span className={styles.practiceIcon} aria-hidden>
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                  <rect x="4" y="7" width="16" height="12" rx="3" />
                  <path d="M12 3v4M9 12h.01M15 12h.01M9.5 16h5" />
                </svg>
              </span>
              <span className={styles.practiceText}>
                <span className={styles.practiceLabel}>Play the AI while you wait</span>
                <span className={styles.practiceCaption}>{practiceCaption(status.mode ?? null, status.format ?? null)}</span>
              </span>
              <span className={styles.practiceArrow} aria-hidden>→</span>
            </button>
          )}
        </>
      ) : (
        <>
          <ModePicker
            value={choice.mode}
            counts={counts}
            onChange={(next) => choose({ mode: next })}
          />
          <p className={styles.blurb} id="find-opponent-blurb">{mode.blurb}</p>
          <div className={styles.controls}>
            {accountsEnabled && mode.rankable && (
              <select
                className={`${styles.pick} ${styles.pickMode}`}
                aria-label="Casual or ranked"
                value={effective.ranked ? 'ranked' : 'casual'}
                onChange={(e) => {
                  if (e.target.value === 'signin') { onSignIn(); return }
                  choose({ ranked: e.target.value === 'ranked' })
                }}
              >
                <option value="casual">Casual</option>
                {rankedAvailable
                  ? <option value="ranked">Ranked</option>
                  : <option value="signin">Ranked — sign in</option>}
              </select>
            )}
            {choice.mode === 'CONSTRUCTED' && (
              <select
                className={`${styles.pick} ${styles.pickFormat}`}
                aria-label="Format"
                value={choice.format ?? ''}
                onChange={(e) => choose({ format: e.target.value as QueueChoice['format'] })}
              >
                {QUEUE_FORMATS.map((o) => {
                  const n = searchingIn(counts, { mode: 'CONSTRUCTED', format: o.format, ranked: effective.ranked })
                  return (
                    <option key={o.format} value={o.format}>
                      {o.label}{n > 0 ? ` · ${n} waiting` : ''}
                    </option>
                  )
                })}
              </select>
            )}
            <button
              type="button"
              className={styles.search}
              data-testid="find-opponent-search"
              aria-describedby="find-opponent-blurb"
              onClick={() => joinMatchmaking(effective.mode, effective.format, effective.ranked)}
            >
              Search
            </button>
          </div>
        </>
      )}

      {status?.notice ? (
        <p className={styles.footnote} role="status">
          {status.notice}
          <button type="button" className={styles.footnoteDismiss} aria-label="Dismiss" onClick={dismissNotice}>×</button>
        </p>
      ) : null}
    </section>
  )
}

/**
 * The four modes as one segmented control — a radio group, so arrow keys move between them. A green
 * dot marks a mode someone is already searching in.
 */
function ModePicker({
  value,
  counts,
  onChange,
}: {
  value: MatchmakingMode
  counts: readonly MatchmakingQueueCount[] | null
  onChange: (mode: MatchmakingMode) => void
}) {
  const refs = useRef<(HTMLButtonElement | null)[]>([])
  const onKeyDown = (e: KeyboardEvent<HTMLDivElement>) => {
    const step = e.key === 'ArrowRight' || e.key === 'ArrowDown' ? 1 : e.key === 'ArrowLeft' || e.key === 'ArrowUp' ? -1 : 0
    if (step === 0) return
    e.preventDefault()
    const index = QUEUE_MODES.findIndex((o) => o.mode === value)
    const next = (index + step + QUEUE_MODES.length) % QUEUE_MODES.length
    onChange(QUEUE_MODES[next]!.mode)
    refs.current[next]?.focus()
  }

  return (
    <div className={styles.modes} role="radiogroup" aria-label="Game mode" onKeyDown={onKeyDown}>
      {QUEUE_MODES.map((o, i) => {
        const selected = o.mode === value
        const waiting = searchingInMode(counts, o.mode)
        return (
          <button
            key={o.mode}
            ref={(el) => { refs.current[i] = el }}
            type="button"
            role="radio"
            aria-checked={selected}
            tabIndex={selected ? 0 : -1}
            className={`${styles.mode} ${selected ? styles.modeSelected : ''}`}
            onClick={() => onChange(o.mode)}
            title={waiting > 0 ? `${o.name} · ${waiting} waiting` : o.name}
            data-testid={`find-opponent-mode-${o.mode}`}
          >
            {o.label}
            {waiting > 0 && <span className={styles.modeDot} aria-label={`${waiting} waiting`} />}
          </button>
        )
      })}
    </div>
  )
}

function SearchingRow({
  label,
  since,
  others,
  onCancel,
}: {
  label: string
  since: number | null
  others: number
  onCancel: () => void
}) {
  const [now, setNow] = useState(() => Date.now())
  useEffect(() => {
    const id = window.setInterval(() => setNow(Date.now()), 1000)
    return () => window.clearInterval(id)
  }, [])

  return (
    <div
      className={styles.searchingRow}
      role="status"
      aria-live="polite"
      title={others > 0 ? `${others} other ${others === 1 ? 'player' : 'players'} in this queue` : 'You’ll be paired as soon as someone joins'}
    >
      <span className={styles.spinner} aria-hidden />
      <span className={styles.searchingText}>{label}</span>
      {since !== null && <span className={styles.wait}>{formatWait(now - since)}</span>}
      <button type="button" className={styles.cancel} onClick={onCancel} data-testid="find-opponent-cancel">
        Cancel
      </button>
    </div>
  )
}
