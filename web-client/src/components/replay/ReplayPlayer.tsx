/**
 * The replay playback surface: transport controls, scrubber, share/export actions and the
 * spectator board.
 *
 * **One surface, two entry points.** There used to be two near-identical copies of this — the
 * `/replay/:gameId` route and the overlay behind the home screen's "Game Replays" button — which
 * had already drifted: only the route knew about replay metadata, archived frames, multiplayer seat
 * labels and team stamping. This is the route's (better) version, extracted so the overlay gets all
 * of that for free and neither can drift again.
 *
 * The two *entry points* stay separate on purpose, because they are genuinely different things: the
 * route is a shareable URL that loads a public replay by id, while the overlay is an in-app screen
 * that lists games and must not navigate (navigating away would drop the WebSocket). What they
 * share is everything after "here are the frames" — which is all of this file.
 */
import { useCallback, useEffect, useRef, useState } from 'react'
import type React from 'react'
import { useGameStore } from '@/store/gameStore.ts'
import { SpectatorContext } from '../../contexts/SpectatorContext'
import { GameBoard } from '../game/GameBoard'
import { CombatArrows } from '../combat/CombatArrows'
import type { SpectatingState } from '@/store/slices'
import type { PublicReplayData, SpectatorStateUpdate } from '@/replay/reconstructSnapshots.ts'
import { buildReplayScenarioUrl } from '../scenario/shareScenario'
import { useViewportSize } from '@/hooks/useResponsive.ts'
import { replayExportUrl } from '@/replay/replayFile.ts'
import styles from './Replay.module.css'
import { BrandMark, PLAIN_BACKDROP } from '../ui/PageShell'
import { SeatPortrait } from '../game/SeatPortrait'

const HEADER_HEIGHT = 55
const AUTOPLAY_INTERVAL_MS = 1000

/** The HUD life orbs' 1v1 colours: player 1 (bottom, blue), player 2 (top, orange). */
const DUEL_RINGS = ['rgba(74, 154, 234, 0.85)', 'rgba(255, 158, 70, 0.85)'] as const

export type ReplayMetadata = PublicReplayData['metadata']

export function ReplayPlayer({
  snapshots,
  gameId,
  metadata,
  fromFile = false,
  onExit,
  onHome,
}: {
  snapshots: readonly SpectatorStateUpdate[]
  /** Replay id, used to build the share/scenario links and fetch a frame's full state. */
  gameId: string
  /** Only the public-replay route has this; the overlay passes nothing and loses only the extras. */
  metadata?: ReplayMetadata | null
  /**
   * Frames from an uploaded replay file rather than a stored game. The server never kept it, so
   * nothing here can address it by id — sharing, exporting and the scenario tools all go.
   */
  fromFile?: boolean
  /** Back button and Escape. The route navigates home; the overlay returns to its game list. */
  onExit: () => void
  /** The Argentum mark — straight back to the landing screen, as on every other page. */
  onHome: () => void
}) {
  const [currentStep, setCurrentStep] = useState(0)
  const [autoPlay, setAutoPlay] = useState(false)
  /**
   * Whether frame 0 has reached the store yet.
   *
   * `GameBoard` calls a different number of hooks depending on whether there is spectating state,
   * so mounting it against an empty store and populating it a tick later crashes React with
   * "Rendered more hooks than during the previous render". Both call sites used to avoid this by
   * accident, writing frame 0 in the same batch that revealed the board. Gating the mount here
   * makes that ordering explicit and keeps it in one place.
   */
  const [primed, setPrimed] = useState(false)
  const setSpectatingState = useGameStore((s) => s.setSpectatingState)
  const setSeatTeams = useGameStore((s) => s.setSeatTeams)
  const setSeatAvatars = useGameStore((s) => s.setSeatAvatars)

  const writeSnapshotToStore = useCallback(
    (snapshot: SpectatorStateUpdate) => {
      const state: SpectatingState = {
        gameSessionId: snapshot.gameSessionId,
        gameState: snapshot.gameState as SpectatingState['gameState'],
        player1Id: snapshot.player1Id,
        player2Id: snapshot.player2Id,
        player1Name: snapshot.player1Name ?? 'Player 1',
        player2Name: snapshot.player2Name ?? 'Player 2',
        player1: snapshot.player1 as SpectatingState['player1'],
        player2: snapshot.player2 as SpectatingState['player2'],
        currentPhase: snapshot.currentPhase,
        activePlayerId: snapshot.activePlayerId,
        priorityPlayerId: snapshot.priorityPlayerId,
        combat: snapshot.combat as SpectatingState['combat'],
        decisionStatus: snapshot.decisionStatus as SpectatingState['decisionStatus'],
        isReplay: true,
      }
      setSpectatingState(state)
    },
    [setSpectatingState],
  )

  // Show frame 0 as soon as frames arrive, and rewind whenever a different replay is loaded.
  // The store write and `setPrimed` land in one commit, so the board's first render already has
  // state to read.
  useEffect(() => {
    setCurrentStep(0)
    setAutoPlay(false)
    const hasFrames = snapshots.length > 0
    // Stamp the per-seat facts that only ride in the seat roster — team membership and each
    // seat's avatar. Live play and spectating stamp them in their message handlers; replay frames
    // go straight into the store and bypass those, so without this a replay showed every seat as
    // a bare life orb and a team game as a free-for-all.
    const roster = snapshots[0]?.players ?? []
    const teams: Record<string, number> = {}
    for (const p of roster) if (p.teamIndex != null) teams[p.playerId] = p.teamIndex
    setSeatTeams(teams, roster.some((p) => p.teamSharedLife), roster.some((p) => p.teamSharedTurns))
    setSeatAvatars(roster)
    if (hasFrames) writeSnapshotToStore(snapshots[0]!)
    setPrimed(hasFrames)
  }, [snapshots, writeSnapshotToStore, setSeatTeams, setSeatAvatars])

  useEffect(() => {
    return () => {
      setSpectatingState(null)
      setSeatTeams({})
      setSeatAvatars([])
    }
  }, [setSpectatingState, setSeatTeams, setSeatAvatars])

  const goToStep = useCallback(
    (step: number) => {
      if (step < 0 || step >= snapshots.length) return
      setCurrentStep(step)
      writeSnapshotToStore(snapshots[step]!)
    },
    [snapshots, writeSnapshotToStore],
  )

  useEffect(() => {
    if (!autoPlay) return
    const timer = setInterval(() => {
      setCurrentStep((prev) => {
        const next = prev + 1
        if (next >= snapshots.length) {
          setAutoPlay(false)
          return prev
        }
        writeSnapshotToStore(snapshots[next]!)
        return next
      })
    }, AUTOPLAY_INTERVAL_MS)
    return () => clearInterval(timer)
  }, [autoPlay, snapshots, writeSnapshotToStore])

  useEffect(() => {
    const handler = (e: KeyboardEvent) => {
      if (e.key === 'ArrowLeft') { e.preventDefault(); goToStep(currentStep - 1) }
      else if (e.key === 'ArrowRight') { e.preventDefault(); goToStep(currentStep + 1) }
      else if (e.key === ' ') { e.preventDefault(); setAutoPlay((p) => !p) }
      else if (e.key === 'Escape') { onExit() }
    }
    window.addEventListener('keydown', handler)
    return () => window.removeEventListener('keydown', handler)
  }, [goToStep, currentStep, onExit])

  // Measure the header so the board sits below it even when the controls wrap to a 2nd row
  // on narrow windows (otherwise the rightmost buttons overflow off-screen).
  const headerRef = useRef<HTMLDivElement>(null)
  const [headerHeight, setHeaderHeight] = useState(HEADER_HEIGHT)
  useEffect(() => {
    const el = headerRef.current
    if (!el) return
    const update = () => setHeaderHeight(el.offsetHeight)
    update()
    const ro = new ResizeObserver(update)
    ro.observe(el)
    return () => ro.disconnect()
  }, [])

  // Same breakpoint as useResponsive's isMobile. The scenario/snapshot/share buttons don't fit the
  // header on phones — hide them there (they're desktop-tooling features anyway).
  const isMobile = useViewportSize().width < 640

  const [scenarioCopied, setScenarioCopied] = useState(false)
  const [replayCopied, setReplayCopied] = useState(false)
  const [downloaded, setDownloaded] = useState(false)
  const [downloadError, setDownloadError] = useState(false)

  const copyToClipboard = async (url: string, prompt: string, flag: (v: boolean) => void) => {
    try {
      await navigator.clipboard.writeText(url)
      flag(true)
      setTimeout(() => flag(false), 2500)
    } catch {
      window.prompt(prompt, url)
    }
  }

  const handleShareAsScenario = () =>
    copyToClipboard(
      buildReplayScenarioUrl(window.location.origin, gameId, currentStep),
      'Copy this scenario link',
      setScenarioCopied,
    )

  const handleShareReplay = () =>
    copyToClipboard(`${window.location.origin}/replay/${gameId}`, 'Copy this replay link', setReplayCopied)

  const handleDownloadSnapshot = async () => {
    // Download the frame's full game state as a file you can reload from the Scenario Builder.
    const r = await fetch(`/api/public/replays/${gameId}/frames/${currentStep}/full-state`)
    if (!r.ok) {
      setDownloadError(true)
      setTimeout(() => setDownloadError(false), 2500)
      return
    }
    const blob = new Blob([await r.text()], { type: 'application/json' })
    const a = document.createElement('a')
    a.href = URL.createObjectURL(blob)
    a.download = `scenario-${gameId}-frame${currentStep}.json`
    a.click()
    URL.revokeObjectURL(a.href)
    setDownloaded(true)
    setTimeout(() => setDownloaded(false), 2500)
  }

  const snapshot = snapshots[currentStep]
  if (!primed || !snapshot) return null

  // Older replays predate the flag; they re-simulate, so treat a missing value as reproducible.
  const stateReproducible = !fromFile && metadata?.stateReproducible !== false

  // Replay metadata only carries the first two seat names (legacy 2-player shape), so a 3+ player
  // game would misleadingly read "Alice vs Bob". The reconstructed snapshot's gameState carries
  // every seat in turn order — use it to list all players for multiplayer.
  const allSeats = (snapshot.gameState as SpectatingState['gameState'] | null)?.players ?? []
  const isMultiplayerReplay = allSeats.length > 2
  const matchupLabel = isMultiplayerReplay
    ? allSeats.map((p) => p.name).join('  ·  ')
    : `${metadata?.player1Name ?? snapshot.player1Name} vs ${metadata?.player2Name ?? snapshot.player2Name}`

  return (
    <SpectatorContext.Provider
      value={{
        isSpectating: true,
        player1Id: snapshot.player1Id,
        player2Id: snapshot.player2Id,
        player1Name: snapshot.player1Name ?? 'Player 1',
        player2Name: snapshot.player2Name ?? 'Player 2',
      }}
    >
      <div className={styles.container} style={{ background: PLAIN_BACKDROP }}>
        <div ref={headerRef} className={styles.toolbar}>
          <button type="button" onClick={onHome} className={styles.brand} aria-label="Argentum — home" title="Home">
            <BrandMark size={28} />
          </button>
          <button type="button" onClick={onExit} className={styles.back} aria-label="Back">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden><path d="M15 18l-6-6 6-6" /></svg>
            <span className={styles.backLabel}>Back</span>
          </button>
          <div className={styles.transport}>
            <button type="button" onClick={() => goToStep(currentStep - 1)} disabled={currentStep === 0} className={styles.iconButton} title="Previous (Left Arrow)" aria-label="Previous step">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="currentColor" aria-hidden><path d="M6 6h2v12H6zM9.5 12l8.5 6V6z" /></svg>
            </button>
            <button type="button" onClick={() => setAutoPlay(!autoPlay)} className={styles.playButton} title="Play/Pause (Space)" aria-label={autoPlay ? 'Pause' : 'Play'}>
              {autoPlay
                ? <svg width="18" height="18" viewBox="0 0 24 24" fill="currentColor" aria-hidden><path d="M6 5h4v14H6zM14 5h4v14h-4z" /></svg>
                : <svg width="18" height="18" viewBox="0 0 24 24" fill="currentColor" aria-hidden><path d="M8 5v14l11-7z" /></svg>}
            </button>
            <button type="button" onClick={() => goToStep(currentStep + 1)} disabled={currentStep >= snapshots.length - 1} className={styles.iconButton} title="Next (Right Arrow)" aria-label="Next step">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="currentColor" aria-hidden><path d="M16 6h2v12h-2zM6 18l8.5-6L6 6z" /></svg>
            </button>
          </div>
          <div className={styles.scrubberWrap}>
            <input
              type="range"
              min={0}
              max={snapshots.length - 1}
              value={currentStep}
              onChange={(e) => goToStep(Number(e.target.value))}
              className={styles.scrubber}
              aria-label="Replay position"
              style={{ '--progress': `${snapshots.length > 1 ? (currentStep / (snapshots.length - 1)) * 100 : 100}%` } as React.CSSProperties}
            />
            <span className={styles.stepCounter}>
              {currentStep + 1}<span className={styles.stepTotal}> / {snapshots.length}</span>
            </span>
          </div>
          <div className={styles.identity}>
            <SeatFaces
              playerIds={isMultiplayerReplay
                ? allSeats.map((p) => p.playerId)
                : [snapshot.player1Id, snapshot.player2Id].filter((id): id is string => id != null)}
            />
          <div className={styles.info}>
            <span className={styles.kicker}>
              {isMultiplayerReplay ? `Replay · ${allSeats.length} players` : 'Replay'}
              {fromFile ? ' · from file' : ''}
              {metadata?.winnerName && <span className={styles.winner}> · {metadata.winnerName} won</span>}
            </span>
            <span className={styles.matchup} title={matchupLabel}>{matchupLabel}</span>
          </div>
          </div>
          {/*
            Something about these frames isn't the plain case, and the badge says which. DIVERGED:
            the recorded inputs no longer re-simulate on this build, so the server served the frames
            it stored when the game was played — everything on screen is the real game, but there is
            no live game state behind it, so the scenario buttons are gone rather than merely
            failing when clicked. Otherwise the frames are an exact re-simulation of a recording
            that stops before the game did (a game long enough that recording had to give up), so
            the scenario buttons keep working and only the ending is missing.
          */}
          {metadata?.degradedReason && (
            <span className={styles.badge} title={metadata.degradedReason}>
              {metadata.fidelity === 'DIVERGED' ? 'From archive' : 'Partial recording'}
            </span>
          )}
          {!isMobile && (stateReproducible || !fromFile) && (
            <div className={styles.actions}>
              {stateReproducible && (
                <>
                  <button
                    type="button"
                    onClick={() => void handleShareAsScenario()}
                    className={styles.action}
                    title="Copy a short link that drops you into this exact position — full board, hands, libraries, stack, targets and mana — to play it out yourself or against the AI."
                  >
                    {scenarioCopied ? 'Copied!' : 'Share as scenario'}
                  </button>
                  <button
                    type="button"
                    onClick={() => void handleDownloadSnapshot()}
                    className={styles.action}
                    title="Download this exact position as a snapshot file you can reload later from the Scenario Builder ('Load file')."
                  >
                    {downloadError ? 'Failed' : downloaded ? 'Saved!' : 'Save snapshot'}
                  </button>
                </>
              )}
              {!fromFile && (
                <>
                  {/* A plain link with `download`: the server sends the file with its own name. */}
                  <a
                    href={replayExportUrl(gameId)}
                    download
                    className={styles.action}
                    title="Download this game's replay file — its seed, decks and every action — to keep, or to watch later with 'Open replay file'."
                  >
                    Export
                  </a>
                  <button type="button" onClick={() => void handleShareReplay()} className={styles.actionPrimary} title="Copy a link to this replay">
                    {replayCopied ? 'Link copied' : 'Share replay'}
                  </button>
                </>
              )}
            </div>
          )}
        </div>
        <div className={styles.board}>
          <GameBoard spectatorMode topOffset={headerHeight} />
        </div>
      </div>
      <CombatArrows />
    </SpectatorContext.Provider>
  )
}

/**
 * The players' faces, overlapping like a hand of cards, ahead of the "A vs B" line — so a replay
 * says *who* played before it says anything else. Seats without a portrait simply drop out; with
 * none at all the row renders nothing and the header is the text it always was.
 */
function SeatFaces({ playerIds }: { playerIds: readonly string[] }) {
  // A duel's two faces wear their life orbs' colours (player 1 sits at the bottom, blue); a pod's
  // overlap on a dark ring that keeps neighbours apart.
  const duel = playerIds.length === 2
  return (
    <span className={styles.faces}>
      {playerIds.map((id, i) => (
        <SeatPortrait
          key={id}
          playerId={id}
          size={30}
          ringColor={duel ? DUEL_RINGS[i]! : 'rgba(12, 14, 22, 0.95)'}
          ringWidth={2}
          style={{ zIndex: playerIds.length - i }}
        />
      ))}
    </span>
  )
}
