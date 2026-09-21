/**
 * The playtest launcher: a curated list of draft-deck matchups, and one click to play one.
 *
 * Deliberately not the normal front door. Collecting gameplay labels means playing *chosen* decks
 * — this 17Lands draft deck against that one, with a reason on record for why the pairing is worth
 * an hour — where the usual vs-AI flow rolls a fresh sealed pool and tells you nothing about what
 * you just played (mtg-draft-ai `docs/44`).
 *
 * The rows come from `/api/dev/playtest/matchups`, written by that repo's
 * `scripts/playtest/build_matchups.py`. Everything the page renders is a field of the row, so a new
 * bucket or statistic shows up here without a change to this file or the server.
 *
 * Playing hands off to the normal board: the matchup is parked in sessionStorage and App fires it
 * once connected. The board is Argentum's own — reimplementing it would be absurd, and playing on
 * a different one would measure a different thing.
 */
import { useCallback, useEffect, useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { parkMatchup, type PlaytestMatchup } from '@/components/playtest/playtest'
import styles from './PlaytestPage.module.css'

const API = import.meta.env.VITE_API_URL ?? ''

function deckSize(deckList: Record<string, number>): number {
  return Object.values(deckList).reduce((a, b) => a + b, 0)
}

function record(deck: PlaytestMatchup['you']): string {
  if (!deck.games) return 'no recorded games'
  const pct = deck.winRate === null ? '' : ` (${Math.round(deck.winRate * 100)}%)`
  return `${deck.wins}–${deck.games - (deck.wins ?? 0)}${pct}`
}

export default function PlaytestPage(): React.JSX.Element {
  const navigate = useNavigate()
  const [matchups, setMatchups] = useState<PlaytestMatchup[] | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    let cancelled = false
    fetch(`${API}/api/dev/playtest/matchups`)
      .then(async (res) => {
        if (!res.ok) throw new Error(`HTTP ${res.status}`)
        return (await res.json()) as PlaytestMatchup[]
      })
      .then((rows) => { if (!cancelled) setMatchups(rows) })
      .catch((e: unknown) => {
        if (!cancelled) setError(e instanceof Error ? e.message : 'Could not load matchups')
      })
    return () => { cancelled = true }
  }, [])

  const play = useCallback((matchup: PlaytestMatchup) => {
    parkMatchup(matchup)
    navigate(`/?playtest=${encodeURIComponent(matchup.id)}`)
  }, [navigate])

  /** Grouped by bucket, preserving the order the curation script wrote them in. */
  const byBucket = useMemo(() => {
    const groups = new Map<string, PlaytestMatchup[]>()
    for (const m of matchups ?? []) {
      const list = groups.get(m.bucket)
      if (list) list.push(m)
      else groups.set(m.bucket, [m])
    }
    return [...groups.entries()]
  }, [matchups])

  return (
    <div className={styles.page}>
      <div className={styles.inner}>
        <div className={styles.head}>
          <h1>Playtest</h1>
          <button className={styles.back} onClick={() => navigate('/')}>← Normal play</button>
        </div>
        <p className={styles.sub}>
          Real 17Lands draft decks against the gameplay pilot. Each deck’s record is what it
          actually did for the human who drafted it. Every finished game is exported as a full
          input log, so it can be turned into training labels later.
        </p>

        {error && (
          <p className={styles.empty}>
            Could not load matchups: <span className={styles.warn}>{error}</span>. The endpoint is
            dev-gated — the server needs <code>game.dev-endpoints.enabled=true</code>, which{' '}
            <code>just pilot</code> sets.
          </p>
        )}

        {matchups !== null && matchups.length === 0 && !error && (
          <p className={styles.empty}>
            No matchups curated yet. Run{' '}
            <code>uv run python scripts/playtest/build_matchups.py</code> in mtg-draft-ai, then
            reload.
          </p>
        )}

        {byBucket.map(([bucket, rows]) => (
          <section key={bucket}>
            <h2 className={styles.bucket}>{bucket}</h2>
            <p className={styles.reason}>{rows[0]?.reason}</p>
            <div className={styles.grid}>
              {rows.map((m) => (
                <div key={m.id} className={styles.card}>
                  <div className={styles.seat}>
                    <div>
                      <div className={styles.seatLabel}>You</div>
                      <div className={styles.colors}>{m.you.colors}</div>
                    </div>
                    <div className={styles.meta}>
                      {m.you.rank ?? 'unranked'} · {record(m.you)}
                    </div>
                  </div>
                  <div className={styles.vs}>VS</div>
                  <div className={styles.seat}>
                    <div>
                      <div className={styles.seatLabel}>Pilot</div>
                      <div className={styles.colors}>{m.opponent.colors}</div>
                    </div>
                    <div className={styles.meta}>
                      {m.opponent.rank ?? 'unranked'} · {record(m.opponent)}
                    </div>
                  </div>
                  <div className={styles.tags}>
                    <span className={styles.tag}>{m.set}</span>
                    <span className={styles.tag}>{m.you.instants} instants</span>
                    <span className={styles.tag}>{deckSize(m.you.deckList)} cards</span>
                  </div>
                  <button className={styles.play} onClick={() => play(m)}>
                    Play this matchup
                  </button>
                </div>
              ))}
            </div>
          </section>
        ))}
      </div>
    </div>
  )
}
