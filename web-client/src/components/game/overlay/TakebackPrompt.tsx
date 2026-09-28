import { useEffect } from 'react'
import { useGameStore } from '@/store/gameStore.ts'

/**
 * Take-back messages at the top of the board: the opponent asking to take back a decision (Allow /
 * Deny — the request rides on every state update and vanishes once anyone acts), and a short
 * notice when your own request was declined.
 */
export function TakebackPrompt() {
  const request = useGameStore((s) => s.takebackRequest)
  const respond = useGameStore((s) => s.respondTakeback)
  const notice = useGameStore((s) => s.takebackNotice)
  const clearNotice = useGameStore((s) => s.clearTakebackNotice)

  useEffect(() => {
    if (!notice) return
    const id = window.setTimeout(clearNotice, 4000)
    return () => window.clearTimeout(id)
  }, [notice, clearNotice])

  if (request) {
    return (
      <div role="alertdialog" aria-label="Take-back request" style={styles.panel} data-testid="takeback-request">
        <span>
          <b>{request.requesterName}</b> asks to take back: <b>{request.label}</b>
        </span>
        <button style={{ ...styles.button, ...styles.allow }} onClick={() => respond(true)}>Allow</button>
        <button style={{ ...styles.button, ...styles.deny }} onClick={() => respond(false)}>Deny</button>
      </div>
    )
  }
  if (notice) {
    return (
      <div role="status" style={styles.panel} onClick={clearNotice}>
        Take-back not allowed: {notice}
      </div>
    )
  }
  return null
}

const styles: Record<string, React.CSSProperties> = {
  panel: {
    position: 'fixed',
    top: 56,
    left: '50%',
    transform: 'translateX(-50%)',
    zIndex: 460,
    display: 'flex',
    alignItems: 'center',
    gap: 10,
    padding: '8px 12px',
    background: 'rgba(12, 12, 28, 0.95)',
    border: '1px solid rgba(212, 160, 23, 0.6)',
    borderRadius: 8,
    boxShadow: '0 4px 14px rgba(0, 0, 0, 0.5)',
    color: '#eee',
    fontSize: 14,
    pointerEvents: 'auto',
  },
  button: {
    padding: '4px 12px',
    borderRadius: 6,
    border: 'none',
    fontWeight: 600,
    cursor: 'pointer',
  },
  allow: { background: '#16a34a', color: '#fff' },
  deny: { background: '#444', color: '#eee' },
}
