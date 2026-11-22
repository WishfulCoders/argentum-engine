import { useEffect, useState } from 'react'
import { useGameStore } from '@/store/gameStore.ts'
import overlay from '@/components/game/overlay/GameOverlays.module.css'
import styles from './Matchmaking.module.css'
import type { MatchmakingMode } from '@/types'
import { queueLabel } from './queues'

/** What accepting leads to, so nobody is surprised to land straight in a game. */
const AFTER_ACCEPT: Record<MatchmakingMode, string> = {
  RANDOM_DECK: 'The game starts as soon as you both accept.',
  MOMIR_BASIC: 'The game starts as soon as you both accept.',
  JUMP_IN: 'Once you both accept, pick your two packs.',
  CONSTRUCTED: 'Once you both accept, pick your deck.',
}

/**
 * "Match found" — the accept prompt both paired players see. Mounted app-wide so it reaches the
 * player wherever they are. The server runs the real deadline; the countdown here only shows it.
 * Once both accept, the server seats them and this closes: Random deck and Momir Basic go straight
 * into the game, Jump In to the pack choice, Constructed to a lobby to pick decks in.
 */
export function MatchFoundDialog() {
  const offer = useGameStore((s) => s.matchOffer)
  const respond = useGameStore((s) => s.respondToMatch)
  // Matchmaking keeps you searching through a warm-up against the AI; accepting ends that game.
  const inWarmUp = useGameStore((s) => s.gameState != null && s.gameOverState == null)
  const [now, setNow] = useState(() => Date.now())

  useEffect(() => {
    if (!offer) return
    const id = window.setInterval(() => setNow(Date.now()), 250)
    // A backgrounded tab still gets the news through its title.
    const previousTitle = document.title
    document.title = 'Match found! — Argentum'
    return () => {
      window.clearInterval(id)
      document.title = previousTitle
    }
  }, [offer])

  if (!offer) return null

  const remainingMs = Math.max(0, offer.receivedAt + offer.acceptWindowMs - now)
  const seconds = Math.ceil(remainingMs / 1000)
  const accepted = offer.youAccepted === true

  return (
    <div className={`${overlay.scrim} ${overlay.scrimFixed}`} role="dialog" aria-modal="true" aria-labelledby="match-found-title">
      <div className={overlay.card} data-testid="match-found">
        <p className={overlay.eyebrow}>{queueLabel(offer.mode, offer.format, offer.ranked)}</p>
        <h2 id="match-found-title" className={overlay.waitTitle}>Match found</h2>
        <p className={overlay.reason}>
          Opponent: <strong>{offer.opponentName}</strong>
          {offer.opponentRating != null && <> · rating {offer.opponentRating}</>}
        </p>
        <div className={styles.countdown} aria-hidden>
          <div
            className={styles.countdownBar}
            style={{ width: `${offer.acceptWindowMs > 0 ? (remainingMs / offer.acceptWindowMs) * 100 : 0}%` }}
          />
        </div>
        <p className={overlay.reason}>{AFTER_ACCEPT[offer.mode]}</p>
        <p className={overlay.reason}>
          {accepted
            ? `Waiting for ${offer.opponentName} to accept… (${seconds}s)`
            : `Accept within ${seconds}s or you leave the queue.`}
        </p>
        {inWarmUp && !accepted && (
          <p className={styles.warmUpNote}>Accepting ends your game against the AI.</p>
        )}
        <div className={overlay.actions}>
          <button type="button" className={overlay.secondary} onClick={() => respond(false)}>
            Decline
          </button>
          <button
            type="button"
            className={overlay.primary}
            onClick={() => respond(true)}
            disabled={accepted}
            autoFocus
            data-testid="match-found-accept"
          >
            {accepted ? 'Accepted' : 'Accept'}
          </button>
        </div>
      </div>
    </div>
  )
}
