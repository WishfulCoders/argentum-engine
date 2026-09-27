import { useEffect, useRef, useState } from 'react'

/**
 * Keyboard shortcuts for a decision overlay: a key → handler map, live while the overlay is mounted.
 *
 * Keys are matched on `KeyboardEvent.key` case-insensitively ('Enter', 'y', '1'). A handler that
 * returns `false` declines the press (e.g. Enter while Confirm is disabled) and the event is left
 * alone. Presses with a modifier, auto-repeats, and presses aimed at a text field are ignored, so a
 * held key can't click through several prompts and typing in a note box never answers one.
 *
 * Handlers are read through a ref, so callers can pass a fresh object literal every render.
 */
export function useDecisionHotkeys(handlers: Record<string, () => boolean | void>, enabled: boolean = true): void {
  const handlersRef = useRef(handlers)
  handlersRef.current = handlers

  useEffect(() => {
    if (!enabled) return
    const onKeyDown = (e: KeyboardEvent) => {
      if (e.repeat || e.ctrlKey || e.metaKey || e.altKey) return
      const target = e.target as HTMLElement | null
      if (target?.closest('input, textarea, select, [contenteditable="true"]')) return
      const key = e.key.length === 1 ? e.key.toLowerCase() : e.key
      const handler = handlersRef.current[key]
      if (!handler) return
      if (handler() === false) return
      // Also keeps a focused button from being activated by the same Enter.
      e.preventDefault()
      e.stopPropagation()
    }
    window.addEventListener('keydown', onKeyDown)
    return () => window.removeEventListener('keydown', onKeyDown)
  }, [enabled])
}

/**
 * True while Tab is held: the decision overlays hide so the board underneath can be read, and come
 * back on release with every choice made so far intact (the overlay stays mounted, only hidden).
 */
export function usePeekKey(enabled: boolean): boolean {
  const [peeking, setPeeking] = useState(false)

  useEffect(() => {
    if (!enabled) {
      setPeeking(false)
      return
    }
    const down = (e: KeyboardEvent) => {
      if (e.key !== 'Tab' || e.ctrlKey || e.metaKey || e.altKey) return
      const target = e.target as HTMLElement | null
      if (target?.closest('input, textarea, select, [contenteditable="true"]')) return
      e.preventDefault()
      setPeeking(true)
    }
    const up = (e: KeyboardEvent) => {
      if (e.key === 'Tab') setPeeking(false)
    }
    // Alt-tabbing away while holding Tab never delivers the keyup here.
    const blur = () => setPeeking(false)
    window.addEventListener('keydown', down)
    window.addEventListener('keyup', up)
    window.addEventListener('blur', blur)
    return () => {
      window.removeEventListener('keydown', down)
      window.removeEventListener('keyup', up)
      window.removeEventListener('blur', blur)
    }
  }, [enabled])

  return peeking
}
