/**
 * Where the emote picker goes, given the button it hangs off and the viewport. In order of
 * preference:
 *
 * 1. **Above** the button — it sits mid-screen in the HUD row with your hand below it.
 * 2. **Beside** it, vertically centred and kept inside the viewport — a laptop-height window has no
 *    room above, and opening sideways keeps both the button and your hand visible.
 * 3. **Below** it.
 * 4. Pinned inside the viewport with a max height, so it scrolls instead of running off the edge.
 */
export interface Rect { readonly left: number; readonly right: number; readonly top: number; readonly bottom: number }
export interface Size { readonly width: number; readonly height: number }

export interface Placement {
  readonly left: number
  readonly top: number
  /** Set only when the picker can't fit whole anywhere. */
  readonly maxHeight?: number
  readonly direction: 'up' | 'down' | 'side'
}

const MARGIN = 8
const GAP = 10

export function placePicker(anchor: Rect, picker: Size, viewport: Size): Placement {
  const clampLeft = (x: number) => Math.max(MARGIN, Math.min(x, viewport.width - picker.width - MARGIN))
  const clampTop = (y: number) => Math.max(MARGIN, Math.min(y, viewport.height - picker.height - MARGIN))
  const usable = viewport.height - 2 * MARGIN

  if (picker.height <= anchor.top - GAP - MARGIN) {
    return { left: clampLeft(anchor.left - 6), top: anchor.top - GAP - picker.height, direction: 'up' }
  }

  if (picker.height <= usable) {
    const top = clampTop((anchor.top + anchor.bottom) / 2 - picker.height / 2)
    if (anchor.right + GAP + picker.width + MARGIN <= viewport.width) {
      return { left: anchor.right + GAP, top, direction: 'side' }
    }
    if (anchor.left - GAP - picker.width >= MARGIN) {
      return { left: anchor.left - GAP - picker.width, top, direction: 'side' }
    }
  }

  if (picker.height <= viewport.height - anchor.bottom - GAP - MARGIN) {
    return { left: clampLeft(anchor.left - 6), top: anchor.bottom + GAP, direction: 'down' }
  }

  return picker.height <= usable
    ? { left: clampLeft(anchor.left - 6), top: clampTop(anchor.top - GAP - picker.height), direction: 'up' }
    : { left: clampLeft(anchor.left - 6), top: MARGIN, maxHeight: usable, direction: 'up' }
}
