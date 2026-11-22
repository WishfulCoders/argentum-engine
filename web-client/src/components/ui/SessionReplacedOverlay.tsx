import { useState } from 'react'
import { useGameStore } from '@/store/gameStore.ts'
import overlay from '@/components/game/overlay/GameOverlays.module.css'

/**
 * Full-screen overlay shown when the server reports this tab's session was taken over
 * by another tab or device. Auto-reconnect is stopped at that point (reconnecting would
 * steal the session straight back and the tabs would fight); "Use here" reclaims it
 * explicitly — the other tab then gets this same overlay.
 */
export function SessionReplacedOverlay() {
  const sessionReplaced = useGameStore((s) => s.sessionReplaced)
  const connect = useGameStore((s) => s.connect)
  const [reclaiming, setReclaiming] = useState(false)

  if (!sessionReplaced) return null

  const storedName = localStorage.getItem('argentum-player-name')

  const reclaim = () => {
    if (!storedName) return
    setReclaiming(true)
    connect(storedName)
  }

  return (
    <div className={`${overlay.scrim} ${overlay.scrimFixed}`} role="dialog" aria-modal="true" aria-labelledby="session-replaced-title">
      <div className={overlay.card}>
        <p className={overlay.eyebrow}>Session moved</p>
        <h2 id="session-replaced-title" className={overlay.waitTitle}>Opened in another tab</h2>
        <p className={overlay.reason}>
          Your session is now active in a different tab or device.
          {storedName ? ' You can take it back and continue playing here.' : ' Refresh the page to continue here.'}
        </p>
        {storedName && (
          <div className={overlay.actions}>
            <button type="button" className={overlay.primary} onClick={reclaim} disabled={reclaiming}>
              {reclaiming ? 'Reconnecting…' : 'Use here'}
            </button>
          </div>
        )}
      </div>
    </div>
  )
}
