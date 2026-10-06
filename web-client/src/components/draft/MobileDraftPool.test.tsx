import { renderToStaticMarkup } from 'react-dom/server'
import { describe, expect, it, vi } from 'vitest'
import type { SealedCardInfo } from '@/types'
import { MobileDraftTabBar, MobilePoolView } from './MobileDraftPool'

vi.mock('@/utils/cardImages.ts', () => ({
  getCardImageUrl: (name: string) => `https://example.test/${encodeURIComponent(name)}.jpg`,
}))

function card(name: string, manaCost: string, typeLine: string): SealedCardInfo {
  return { name, manaCost, typeLine, rarity: 'COMMON', imageUri: null }
}

const noop = () => {}

describe('MobilePoolView', () => {
  const pool = [
    card('Glory Seeker', '{1}{W}', 'Creature — Human Soldier'),
    card('Shock', '{R}', 'Instant'),
    card('Shock', '{R}', 'Instant'),
    card('Sparksmith', '{1}{R}', 'Creature — Goblin'),
  ]

  it('shows every drafted card as an image, with a column per color', () => {
    const html = renderToStaticMarkup(
      <MobilePoolView cards={pool} picksPerRound={1} grouping="color" onGroupingChange={noop} onPreview={noop} />,
    )
    expect(html).toContain('White 1')
    expect(html).toContain('Red 3')
    for (const name of ['Glory Seeker', 'Shock', 'Sparksmith']) {
      expect(html).toContain(`src="https://example.test/${encodeURIComponent(name)}.jpg"`)
    }
  })

  it('marks duplicates with a count and the newest pick as NEW', () => {
    const html = renderToStaticMarkup(
      <MobilePoolView cards={pool} picksPerRound={1} grouping="color" onGroupingChange={noop} onPreview={noop} />,
    )
    expect(html).toContain('aria-label="Shock ×2"')
    expect(html.match(/>NEW</g)).toHaveLength(1)
    // The NEW badge belongs to the last pick, Sparksmith.
    const sparksmith = html.indexOf('aria-label="Sparksmith"')
    expect(html.indexOf('>NEW<', sparksmith)).toBeGreaterThan(sparksmith)
  })

  it('marks every card from the latest pick as NEW in pick-2 formats', () => {
    const html = renderToStaticMarkup(
      <MobilePoolView cards={pool} picksPerRound={2} grouping="color" onGroupingChange={noop} onPreview={noop} />,
    )
    // The last two picks were Shock and Sparksmith; Glory Seeker was earlier.
    expect(html.match(/>NEW</g)).toHaveLength(2)
    const glorySeeker = html.indexOf('aria-label="Glory Seeker"')
    expect(html.slice(glorySeeker, html.indexOf('aria-label=', glorySeeker + 1))).not.toContain('>NEW<')
  })

  it('shows the creature/spell split and marks the active grouping', () => {
    const html = renderToStaticMarkup(
      <MobilePoolView cards={pool} picksPerRound={1} grouping="type" onGroupingChange={noop} onPreview={noop} />,
    )
    expect(html).toMatch(/>2<\/span><span[^>]*>Creatures</)
    expect(html).toMatch(/>2<\/span><span[^>]*>Spells</)
    expect(html).toMatch(/aria-pressed="true"[^>]*>Type</)
    expect(html).toContain('Creature 2')
  })

  it('shows an empty state before the first pick', () => {
    const html = renderToStaticMarkup(
      <MobilePoolView cards={[]} picksPerRound={1} grouping="color" onGroupingChange={noop} onPreview={noop} />,
    )
    expect(html).toContain('Cards you pick will show up here.')
  })
})

describe('MobileDraftTabBar', () => {
  it('shows the picked count on Pool and badges waiting packs on Pack', () => {
    const html = renderToStaticMarkup(
      <MobileDraftTabBar tab="pool" onChange={noop} packsWaiting={2} pickedLabel="17 / 45" />,
    )
    expect(html).toContain('17 / 45')
    expect(html).toContain('aria-label="2 packs waiting"')
    expect(html).toMatch(/aria-selected="true"[^>]*>.*Pool/)
  })

  it('hides the badge when no pack is waiting', () => {
    const html = renderToStaticMarkup(
      <MobileDraftTabBar tab="pack" onChange={noop} packsWaiting={0} pickedLabel="3" />,
    )
    expect(html).not.toContain('waiting')
  })
})
