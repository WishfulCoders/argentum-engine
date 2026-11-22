import { describe, expect, it } from 'vitest'
import { placePicker } from './pickerPlacement'

const picker = { width: 420, height: 400 }
const anchorAt = (top: number, left = 600) => ({ left, right: left + 30, top, bottom: top + 30 })
const overlaps = (p: { left: number; top: number }, a: ReturnType<typeof anchorAt>) =>
  p.left < a.right && p.left + picker.width > a.left && p.top < a.bottom && p.top + picker.height > a.top

describe('placePicker', () => {
  it('opens above the button when there is room', () => {
    expect(placePicker(anchorAt(500), picker, { width: 1440, height: 900 }))
      .toEqual({ left: 594, top: 90, direction: 'up' })
  })

  it('opens beside the button on a laptop-height window, inside the viewport and clear of the button', () => {
    const anchor = anchorAt(370, 740)
    const p = placePicker(anchor, picker, { width: 1280, height: 680 })
    expect(p.direction).toBe('side')
    expect(p.left).toBe(780)
    expect(p.top).toBeGreaterThanOrEqual(8)
    expect(p.top + picker.height).toBeLessThanOrEqual(672)
    expect(overlaps(p, anchor)).toBe(false)
  })

  it('opens to the left when the right side is too narrow', () => {
    const p = placePicker(anchorAt(300, 1100), picker, { width: 1280, height: 640 })
    expect(p).toMatchObject({ direction: 'side', left: 1100 - 10 - 420 })
  })

  it('opens below when it fits neither above nor beside', () => {
    const p = placePicker(anchorAt(100, 400), picker, { width: 800, height: 900 })
    expect(p).toMatchObject({ direction: 'down', top: 140 })
  })

  it('scrolls on a viewport shorter than the picker', () => {
    expect(placePicker(anchorAt(150), picker, { width: 1280, height: 300 })).toMatchObject({ top: 8, maxHeight: 284 })
  })
})
