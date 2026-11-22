import { useState, useEffect } from 'react'
import styles from './GameUI.module.css'

/**
 * Fullscreen toggle button. Pinned to the corner of the landing and lobby screens.
 *
 * `compact` draws an icon instead of the label, for the landing and lobby top bars.
 */
export function FullscreenButton({ compact = false }: { compact?: boolean } = {}) {
  const [isFullscreen, setIsFullscreen] = useState(false)

  useEffect(() => {
    const handleFullscreenChange = () => {
      setIsFullscreen(!!document.fullscreenElement)
    }
    document.addEventListener('fullscreenchange', handleFullscreenChange)
    return () => document.removeEventListener('fullscreenchange', handleFullscreenChange)
  }, [])

  const toggleFullscreen = async () => {
    try {
      if (!document.fullscreenElement) {
        await document.documentElement.requestFullscreen()
      } else {
        await document.exitFullscreen()
      }
    } catch (err) {
      console.error('Fullscreen error:', err)
    }
  }

  const title = isFullscreen ? 'Exit fullscreen (Esc)' : 'Enter fullscreen'
  if (compact) {
    return (
      <button
        type="button"
        onClick={toggleFullscreen}
        className={styles.fullscreenButtonCompact}
        title={title}
        aria-label={title}
      >
        <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden>
          {isFullscreen
            ? <path d="M9 4v5H4 M15 4v5h5 M9 20v-5H4 M15 20v-5h5" />
            : <path d="M4 9V4h5 M20 9V4h-5 M4 15v5h5 M20 15v5h-5" />}
        </svg>
      </button>
    )
  }

  return (
    <button
      onClick={toggleFullscreen}
      className={styles.fullscreenButton}
      title={title}
    >
      {isFullscreen ? '⛶ Exit' : '⛶ Fullscreen'}
    </button>
  )
}
