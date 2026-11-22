import { useEffect, useMemo, useState } from 'react'
import { useGameStore, type DeckBuildingState } from '@/store/gameStore.ts'
import { fetchAdvisors, type AdvisorInfo } from '@/api/aiAssist'
import { sameDeck } from '@/store/slices/assistDeck'
import type { AutoBuildResult } from '@/store/slices/types'
import { ManaSymbol } from '../ui/ManaSymbols'
import { HelpTip } from '../help/HelpTip'
import styles from './assist.module.css'

/**
 * The deck builder's AI assistance, in two pieces:
 *
 *  - {@link AssistToolbar} — one grouped control in the header: the engine, Auto-build and the
 *    card score toggle.
 *  - {@link AssistStrip} — a strip under the header that only appears when there is something to
 *    say: what the AI is working on, what went wrong, the builds Auto-build found (with Undo), and
 *    the score legend.
 *
 * Both only render when the lobby has AI assistance on.
 */

export type ScoreTone = 'strong' | 'playable' | 'weak'

/** Same thresholds as the draft Suggest Pick badges: 70+ strong, 45+ playable. */
export function scoreTone(score: number): ScoreTone {
  return score >= 70 ? 'strong' : score >= 45 ? 'playable' : 'weak'
}

/** A 0–100 AI score as a small pill; `stale` dims it when the deck has changed since scoring. */
export function ScoreBadge({ score, stale = false }: { score: number; stale?: boolean }) {
  return (
    <span className={styles.score} data-tone={scoreTone(score)} data-stale={stale}>
      {score}
    </span>
  )
}

/** True when the card scores were computed against a deck other than the current one. */
export function useScoresStale(state: DeckBuildingState): boolean {
  const context = useGameStore((s) => s.deckCardScoresContext)
  return context != null && !sameDeck(context, { deck: state.deck, landCounts: state.landCounts })
}

/**
 * The deckbuild engines the server offers, and the player's pick. The pick lives in the store so it
 * survives these controls remounting on every deck edit; `fetchAdvisors` caches the list.
 */
function useDeckbuildEngines() {
  const advisorId = useGameStore((s) => s.deckbuildAdvisorId)
  const setAdvisorId = useGameStore((s) => s.setDeckbuildAdvisorId)
  const [engines, setEngines] = useState<readonly AdvisorInfo[]>([])

  useEffect(() => {
    let cancelled = false
    fetchAdvisors()
      .then((r) => {
        if (cancelled) return
        setEngines(r.deckbuild)
        // Keep the player's saved choice; only fall back to the default if it's unset or stale.
        const saved = useGameStore.getState().deckbuildAdvisorId
        if (saved == null || !r.deckbuild.some((a) => a.id === saved)) {
          setAdvisorId(r.deckbuild[0]?.id ?? null)
        }
      })
      .catch(() => {
        // No picker; the buttons still work using the server's default engine.
      })
    return () => {
      cancelled = true
    }
  }, [setAdvisorId])

  return { engines, advisorId, setAdvisorId }
}

export function AssistToolbar({
  hasCards,
  compact,
}: {
  hasCards: boolean
  /** Phones: icon-only labels so the group fits on one line. */
  compact: boolean
}) {
  const autoBuildDeck = useGameStore((s) => s.autoBuildDeck)
  const scoreDeckCards = useGameStore((s) => s.scoreDeckCards)
  const clearDeckCardScores = useGameStore((s) => s.clearDeckCardScores)
  const busy = useGameStore((s) => s.aiAssistBusy)
  const task = useGameStore((s) => s.deckAssistTask)
  const hasScores = useGameStore((s) => s.deckCardScores != null)
  const { engines, advisorId, setAdvisorId } = useDeckbuildEngines()

  const engine = advisorId ?? undefined
  const buildLabel = hasCards ? 'Complete deck' : 'Auto-build'

  return (
    <div className={styles.group} role="group" aria-label="AI assistance">
      {!compact && (
        <span className={styles.tag} aria-hidden>
          <SparkleIcon size={12} /> AI
        </span>
      )}
      {engines.length > 1 && (
        <select
          className={styles.select}
          value={advisorId ?? ''}
          onChange={(e) => setAdvisorId(e.target.value)}
          aria-label="AI engine for Auto-build and card scores"
          title="Engine used by Auto-build and card scores"
          disabled={busy}
        >
          {engines.map((a) => (
            <option key={a.id} value={a.id}>
              {a.name}
            </option>
          ))}
        </select>
      )}
      <button
        type="button"
        className={styles.btn}
        onClick={() => void autoBuildDeck(engine)}
        disabled={busy}
        aria-label={buildLabel}
        title={
          hasCards
            ? 'Keep the cards you picked and fill the rest of the deck from your pool, lands included'
            : 'Build the best deck it can from your pool, lands included'
        }
      >
        {busy && task === 'build' ? <span className={styles.spinner} aria-hidden /> : <WandIcon />}
        {!compact && buildLabel}
      </button>
      <button
        type="button"
        className={styles.btn}
        aria-pressed={hasScores}
        onClick={() => (hasScores ? clearDeckCardScores() : void scoreDeckCards(engine))}
        disabled={busy}
        aria-label="Card scores"
        title={
          hasScores
            ? 'Hide the score badges'
            : 'Rate every card in your pool from 0 to 100, using your deck so far for colour context'
        }
      >
        {busy && task === 'score' ? <span className={styles.spinner} aria-hidden /> : <GaugeIcon />}
        {!compact && 'Card scores'}
      </button>
      {!compact && <HelpTip topicId="draft-assist" size="sm" />}
    </div>
  )
}

export function AssistStrip({ state }: { state: DeckBuildingState }) {
  const { engines, advisorId } = useDeckbuildEngines()
  // Only worth naming when there is a choice of engine.
  const engineName = engines.length > 1 ? engines.find((e) => e.id === advisorId)?.name ?? null : null
  const busy = useGameStore((s) => s.aiAssistBusy)
  const task = useGameStore((s) => s.deckAssistTask)
  const error = useGameStore((s) => s.aiAssistError)
  const clearError = useGameStore((s) => s.clearAiAssistError)
  const result = useGameStore((s) => s.autoBuildResult)
  const undo = useGameStore((s) => s.assistUndo)
  const undoAssistDeck = useGameStore((s) => s.undoAssistDeck)
  const clearAutoBuildResult = useGameStore((s) => s.clearAutoBuildResult)
  const applyAutoBuildOption = useGameStore((s) => s.applyAutoBuildOption)
  const hasScores = useGameStore((s) => s.deckCardScores != null)
  const scoreDeckCards = useGameStore((s) => s.scoreDeckCards)
  const scoresStale = useScoresStale(state)

  const current = useMemo(() => ({ deck: state.deck, landCounts: state.landCounts }), [state.deck, state.landCounts])
  // Undo only while the deck is still exactly what the AI left — after a hand edit it would wipe the edit.
  const canUndo = undo != null && sameDeck(current, undo.applied)

  if (busy) {
    return (
      <div className={styles.strip} role="status" aria-live="polite">
        <span className={styles.section}>
          <span className={styles.spinner} aria-hidden />
          {task === 'score'
            ? `Scoring every card in your pool${engineName ? ` with ${engineName}` : ''}…`
            : `Building your deck${engineName ? ` with ${engineName}` : ''}…`}
        </span>
      </div>
    )
  }

  const showBuild = result != null
  if (!error && !showBuild && !hasScores) return null

  return (
    <div className={styles.strip} data-tone={error ? 'error' : undefined} role="status" aria-live="polite">
      {error && (
        <span className={styles.section}>
          <span className={styles.error}>{friendlyError(error)}</span>
          <button type="button" className={styles.close} onClick={clearError} aria-label="Dismiss">×</button>
        </span>
      )}

      {showBuild && (
        <BuildChoices
          result={result}
          isApplied={(i) => canUndo && i === result.appliedIndex}
          onPick={applyAutoBuildOption}
          canUndo={canUndo}
          onUndo={undoAssistDeck}
          onDismiss={clearAutoBuildResult}
        />
      )}

      {hasScores && (
        <span className={styles.section}>
          <span className={styles.label}>Card scores</span>
          <span className={styles.legend}>
            <LegendItem tone="strong" text="70+ strong" />
            <LegendItem tone="playable" text="45+ playable" />
            <LegendItem tone="weak" text="weak" />
          </span>
          {scoresStale ? (
            <>
              <span className={styles.muted}>· your deck changed since</span>
              <button type="button" className={styles.link} onClick={() => void scoreDeckCards(advisorId ?? undefined)}>
                Re-score
              </button>
            </>
          ) : (
            <span className={styles.muted}>· hover a card for why</span>
          )}
        </span>
      )}
    </div>
  )
}

function BuildChoices({
  result,
  isApplied,
  onPick,
  canUndo,
  onUndo,
  onDismiss,
}: {
  result: AutoBuildResult
  isApplied: (index: number) => boolean
  onPick: (index: number) => void
  canUndo: boolean
  onUndo: () => void
  onDismiss: () => void
}) {
  const multiple = result.options.length > 1
  if (!multiple) {
    // One candidate: nothing to choose between, so a sentence rather than a lone chip.
    const option = result.options[0]!
    const score = option.score != null ? formatBuildScore(result.advisorId, option.score) : null
    return (
      <span className={styles.section}>
        <span className={styles.label}>Auto-build</span>
        {option.colors.length > 0 && (
          <span className={styles.pips}>
            {option.colors.map((c) => <ManaSymbol key={c} symbol={c} size={14} />)}
          </span>
        )}
        <span>{option.archetype ? `Built a ${option.archetype} deck` : 'Deck built'}</span>
        {score && (
          <span className={styles.muted} title={scoreExplanation(result.advisorId)}>
            · rated <span className={styles.chipScore}>{score}</span>
          </span>
        )}
        {canUndo ? (
          <button type="button" className={styles.link} onClick={onUndo} title="Put back the deck you had before Auto-build">
            Undo
          </button>
        ) : !isApplied(0) && (
          <button type="button" className={styles.link} onClick={() => onPick(0)} title="Throw away your edits and use this build again">
            Re-apply
          </button>
        )}
        <button type="button" className={styles.close} onClick={onDismiss} aria-label="Dismiss Auto-build" title="Dismiss">
          ×
        </button>
      </span>
    )
  }
  return (
    <span className={styles.section}>
      <span className={styles.label}>Builds</span>
      {result.options.map((option, i) => {
        const applied = isApplied(i)
        const score = option.score != null ? formatBuildScore(result.advisorId, option.score) : null
        const name = option.archetype ?? (option.colors.length > 0 ? null : `Build ${i + 1}`)
        return (
          <button
            key={i}
            type="button"
            className={styles.chip}
            aria-pressed={applied}
            onClick={() => onPick(i)}
            title={
              applied
                ? 'This build is your deck'
                : `Replace your deck with this build${score ? ` — ${scoreExplanation(result.advisorId)}` : ''}`
            }
          >
            {option.colors.length > 0 && (
              <span className={styles.pips}>
                {option.colors.map((c) => <ManaSymbol key={c} symbol={c} size={14} />)}
              </span>
            )}
            {name && <span>{name}</span>}
            {score && <span className={styles.chipScore}>{score}</span>}
          </button>
        )
      })}
      {canUndo && (
        <button type="button" className={styles.link} onClick={onUndo} title="Put back the deck you had before Auto-build">
          Undo
        </button>
      )}
      <button type="button" className={styles.close} onClick={onDismiss} aria-label="Dismiss Auto-build" title="Dismiss">
        ×
      </button>
    </span>
  )
}

function LegendItem({ tone, text }: { tone: ScoreTone; text: string }) {
  const color = tone === 'strong' ? '#5fd08c' : tone === 'playable' ? '#f2b45c' : '#8f98ac'
  return (
    <span className={styles.legendItem}>
      <span className={styles.dot} style={{ background: color }} />
      {text}
    </span>
  )
}

/** Draftsim reports a 0–10 deck score; the heuristic engine a raw rating sum. */
function formatBuildScore(advisorId: string, score: number): string {
  return advisorId === 'draftsim' ? `${score.toFixed(1)}/10` : `${Math.round(score)} pts`
}

function scoreExplanation(advisorId: string): string {
  return advisorId === 'draftsim'
    ? 'Draftsim deck rating out of 10'
    : 'sum of the card ratings; only comparable between builds'
}

/** The store keeps the raw message; give the common ones a sentence a player can act on. */
function friendlyError(message: string): string {
  if (/disabled|not enabled|403/i.test(message)) return 'AI assistance is turned off for this lobby.'
  if (/failed to fetch|network/i.test(message)) return 'Couldn’t reach the server — check your connection and try again.'
  return message
}

// ── Icons (16px, stroke = currentColor) ─────────────────────────────────

function SvgIcon({ size = 16, children }: { size?: number; children: React.ReactNode }) {
  return (
    <svg width={size} height={size} viewBox="0 0 16 16" fill="none" stroke="currentColor" strokeWidth="1.5"
      strokeLinecap="round" strokeLinejoin="round" aria-hidden>
      {children}
    </svg>
  )
}

function SparkleIcon({ size = 16 }: { size?: number }) {
  return (
    <SvgIcon size={size}>
      <path d="M8 1.5l1.6 4.4 4.4 1.6-4.4 1.6L8 13.5 6.4 9.1 2 7.5l4.4-1.6z" fill="currentColor" stroke="none" />
    </SvgIcon>
  )
}

function WandIcon() {
  return (
    <SvgIcon>
      <path d="M2.5 13.5l7-7" />
      <path d="M8.5 5.5l2 2" />
      <path d="M12 1.5v2M12 6.5v2M9.5 4h-1M15.5 4h-2" />
    </SvgIcon>
  )
}

function GaugeIcon() {
  return (
    <SvgIcon>
      <path d="M2.5 11.5a5.5 5.5 0 1 1 11 0" />
      <path d="M8 11.5l2.5-3.5" />
      <circle cx="8" cy="11.5" r="0.9" fill="currentColor" />
    </SvgIcon>
  )
}
