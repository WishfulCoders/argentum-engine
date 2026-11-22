import { useEffect, useState } from 'react'
import { useGameStore } from '@/store/gameStore.ts'
import styles from './SubgameTransition.module.css'

type Phase = 'in' | 'hold' | 'out'

const HOLD_MS = 2600
const OUT_MS = 450

/**
 * The interstitial as a Magic subgame (Shahrazad, CR 729) begins or ends. The whole table is about
 * to become a different game — fresh life totals, an empty battlefield, a mulligan — and a player
 * who simply found the board changed under them would think something broke. So the moment gets a
 * beat of its own: the spell's card, gilded frames closing in (a game inside the game) or opening
 * out (back to the one outside), and the one fact that matters next — who goes first, or who won.
 *
 * Click anywhere to dismiss early. Purely presentational, driven by the server's subgame events.
 */
export function SubgameTransition() {
  const transition = useGameStore((s) => s.subgameTransition)
  const clear = useGameStore((s) => s.clearSubgameTransition)
  const [phase, setPhase] = useState<Phase>('in')

  useEffect(() => {
    if (!transition) return
    setPhase('in')
    const timers = [
      setTimeout(() => setPhase('hold'), 40),
      setTimeout(() => setPhase('out'), HOLD_MS),
      setTimeout(clear, HOLD_MS + OUT_MS),
    ]
    return () => timers.forEach(clearTimeout)
  }, [transition, clear])

  if (!transition) return null
  const starting = transition.kind === 'start'
  const deeper = transition.depth > 1

  const eyebrow = transition.sourceName ?? 'Subgame'
  const title = starting
    ? (deeper ? 'A subgame within the subgame' : 'A game within the game')
    : 'The subgame is over'
  const detail = starting
    ? 'Each library is now a deck. Fresh life, fresh hands — the main game waits.'
    : transition.winnerNames.length === 0
      ? 'It ended in a draw — nobody won it.'
      : transition.youWon
        ? 'You won it.'
        : `${transition.winnerNames.join(' and ')} won it.`
  const chip = starting
    ? (transition.youGoFirst ? 'You go first' : `${transition.firstPlayerName} goes first`)
    : (deeper ? 'Back to the subgame it came from' : 'Back to the main game')

  return (
    <div
      className={`${styles.backdrop} ${phase === 'hold' ? styles.visible : ''}`}
      data-kind={transition.kind}
      role="status"
      aria-live="polite"
      onClick={clear}
    >
      <div className={styles.frames} aria-hidden>
        <span /><span /><span /><span />
      </div>
      <div className={styles.content}>
        {transition.sourceImageUri && (
          <img className={styles.card} src={transition.sourceImageUri} alt="" />
        )}
        <div className={styles.text}>
          <p className={styles.eyebrow}>{eyebrow}</p>
          <h2 className={styles.title}>{title}</h2>
          <p className={styles.detail}>{detail}</p>
          <p className={styles.chip}>{chip}</p>
        </div>
      </div>
    </div>
  )
}
