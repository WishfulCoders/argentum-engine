import { useEffect, useState } from 'react'
import { useGameStore } from '@/store/gameStore.ts'

/**
 * The loop shortcut (MTR 4.4): after you play one iteration of a loop by hand, the server offers to
 * repeat it. Shows what one repetition does, a "repeat until they lose" button when the loop takes
 * life or gives poison, and a count for anything else. Every repetition is still played through the
 * rules engine; the panel only saves the clicking.
 */
export function LoopOfferPanel() {
  const offer = useGameStore((s) => s.loopOffer)
  const repeatLoop = useGameStore((s) => s.repeatLoop)
  const notice = useGameStore((s) => s.loopNotice)
  const clearNotice = useGameStore((s) => s.clearLoopNotice)
  const [count, setCount] = useState(10)
  const [dismissedKey, setDismissedKey] = useState<string | null>(null)

  useEffect(() => {
    if (!notice) return
    const id = window.setTimeout(clearNotice, 5000)
    return () => window.clearTimeout(id)
  }, [notice, clearNotice])

  if (notice) {
    return (
      <div role="status" style={styles.panel} onClick={clearNotice} data-testid="loop-notice">
        {notice}
      </div>
    )
  }
  if (!offer || offer.maxIterations < 1) return null
  const key = `${offer.label}|${offer.perIteration.join(',')}`
  if (key === dismissedKey) return null

  const toWin = offer.iterationsToWin
  const n = Math.max(1, Math.min(count, offer.maxIterations))
  return (
    <div role="dialog" aria-label="Loop shortcut" style={styles.panel} data-testid="loop-offer">
      <div style={styles.text}>
        <div>
          <b>Loop found:</b> {offer.label}
        </div>
        <div style={styles.changes}>Each time: {offer.perIteration.join(', ')}</div>
      </div>
      {toWin != null && toWin <= offer.maxIterations && (
        <button style={{ ...styles.button, ...styles.win }} onClick={() => repeatLoop(toWin)}>
          Repeat ×{toWin} (they lose)
        </button>
      )}
      <input
        type="number"
        min={1}
        max={offer.maxIterations}
        value={count}
        onChange={(e) => setCount(Number(e.target.value) || 1)}
        style={styles.input}
        aria-label="Repetitions"
        title={`At most ${offer.maxIterations}`}
      />
      <button style={{ ...styles.button, ...styles.repeat }} onClick={() => repeatLoop(n)}>
        Repeat ×{n}
      </button>
      <button style={{ ...styles.button, ...styles.dismiss }} onClick={() => setDismissedKey(key)}>
        Dismiss
      </button>
    </div>
  )
}

const styles: Record<string, React.CSSProperties> = {
  panel: {
    position: 'fixed',
    top: 100,
    left: '50%',
    transform: 'translateX(-50%)',
    zIndex: 455,
    display: 'flex',
    alignItems: 'center',
    gap: 10,
    maxWidth: 'min(760px, calc(100vw - 32px))',
    padding: '8px 12px',
    background: 'rgba(12, 12, 28, 0.95)',
    border: '1px solid rgba(96, 165, 250, 0.6)',
    borderRadius: 8,
    boxShadow: '0 4px 14px rgba(0, 0, 0, 0.5)',
    color: '#eee',
    fontSize: 14,
    pointerEvents: 'auto',
  },
  text: { display: 'flex', flexDirection: 'column', gap: 2, minWidth: 0 },
  changes: { fontSize: 12, color: '#bbb' },
  input: {
    width: 64,
    padding: '3px 6px',
    borderRadius: 6,
    border: '1px solid #555',
    background: '#1b1b2e',
    color: '#eee',
  },
  button: {
    padding: '4px 12px',
    borderRadius: 6,
    border: 'none',
    fontWeight: 600,
    cursor: 'pointer',
    whiteSpace: 'nowrap',
  },
  win: { background: '#b91c1c', color: '#fff' },
  repeat: { background: '#2563eb', color: '#fff' },
  dismiss: { background: '#444', color: '#eee' },
}
