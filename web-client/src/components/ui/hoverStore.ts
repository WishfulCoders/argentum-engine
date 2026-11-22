/**
 * The hovered card, kept outside React state.
 *
 * A list view's rail preview follows the pointer from row to row. Held as page state, every hover
 * re-rendered the whole page — every row, the stats, the search bar — to change one image. Held
 * here, a hover re-renders only what subscribes: the preview, and the rows whose highlight flips.
 */
import { useState, useSyncExternalStore } from 'react'

export interface HoverStore<T> {
  get(): T | null
  set(value: T | null): void
  subscribe(listener: () => void): () => void
}

export function createHoverStore<T>(): HoverStore<T> {
  let value: T | null = null
  const listeners = new Set<() => void>()
  return {
    get: () => value,
    set(next) {
      if (Object.is(next, value)) return
      value = next
      listeners.forEach((l) => l())
    },
    subscribe(listener) {
      listeners.add(listener)
      return () => listeners.delete(listener)
    },
  }
}

/** One store for the component's lifetime. */
export function useHoverStore<T>(): HoverStore<T> {
  return useState(() => createHoverStore<T>())[0]
}

/** The hovered value; re-renders whenever it changes. */
export function useHovered<T>(store: HoverStore<T>): T | null {
  return useSyncExternalStore(store.subscribe, store.get)
}

/** A derived slice of the hovered value; re-renders only when the slice changes (e.g. "is it me?"). */
export function useHoveredSelect<T, R>(store: HoverStore<T>, select: (value: T | null) => R): R {
  return useSyncExternalStore(store.subscribe, () => select(store.get()))
}
