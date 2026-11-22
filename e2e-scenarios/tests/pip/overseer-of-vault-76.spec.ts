import { test, expect } from '../../fixtures/scenarioFixture'

for (const split of [false, true]) {
  test(`Overseer pays three quest counters ${split ? 'across two permanents' : 'from one permanent'}`, async ({ createGame }) => {
    test.setTimeout(120_000)
    const { player1, player2 } = await createGame({
      player1Name: 'Vault Dweller', player2Name: 'Opponent',
      player1: {
        battlefield: [
          { name: 'Overseer of Vault 76', counters: { quest: split ? 2 : 3 } },
          { name: 'Plains', counters: split ? { quest: 1 } : {} },
          { name: 'Grizzly Bears' },
        ],
        library: ['Plains', 'Plains', 'Plains'],
      },
      player2: { battlefield: [{ name: 'Glory Seeker' }], library: ['Mountain', 'Mountain', 'Mountain'] },
      phase: 'PRECOMBAT_MAIN', activePlayer: 1, priorityPlayer: 1,
      player1StopAtSteps: ['PRECOMBAT_MAIN', 'BEGIN_COMBAT'],
    })
    const p1 = player1.gamePage
    const page = player1.page
    await p1.pass()
    await player2.gamePage.resolveStack("Overseer of Vault 76 trigger")
    const plus = page.getByRole('button', { name: 'Allocate one counter from Overseer of Vault 76', exact: true })
    await expect(plus).toBeVisible()
    const minus = page.getByRole('button', { name: 'Remove one allocation from Overseer of Vault 76', exact: true })
    for (const button of [plus, minus]) {
      const box = await button.boundingBox()
      expect(box!.width).toBeGreaterThanOrEqual(24)
      expect(box!.height).toBeGreaterThanOrEqual(24)
    }
    await p1.screenshot('Quest payment with per-permanent capacity and battlefield allocation controls')
    await plus.click()
    await expect(page.getByRole('button', { name: 'Confirm (1)', exact: true })).toBeDisabled()
    await plus.click()
    await page.getByRole('button', { name: 'Remove one allocation from Overseer of Vault 76', exact: true }).click()
    await plus.click()
    if (split) {
      await expect(plus).toBeDisabled()
      await page.getByRole('button', { name: 'Allocate one counter from Plains', exact: true }).click()
    } else {
      await plus.click()
    }
    await expect(plus).toBeDisabled()
    await expect(page.getByRole('button', { name: 'Allocate one counter from Glory Seeker', exact: true })).toHaveCount(0)
    await p1.screenshot('Exactly three quest counters allocated before confirmation')
    await page.getByRole('button', { name: 'Confirm (3)', exact: true }).click()
    await expect(plus).toHaveCount(0)
    await p1.expectStats('Grizzly Bears', '2/2')
    await p1.screenshot('Payment completed; separate reflexive trigger awaits resolution')
    await player2.gamePage.resolveStack("Overseer of Vault 76 trigger")
    await p1.expectStats('Grizzly Bears', '3/3')
    await p1.expectStats('Overseer of Vault 76', '4/4')
    await p1.screenshot('Team receives one counter after reflexive trigger resolves')
    await player2.gamePage.expectStats('Glory Seeker', '2/2')
  })
}

test('Overseer counter allocation remains correctable in the collapsed attachment browser', async ({ createGame }) => {
  test.setTimeout(120_000)
  const { player1, player2 } = await createGame({
    player1Name: 'Vault Dweller', player2Name: 'Opponent',
    player1: {
      battlefield: [
        { name: 'Overseer of Vault 76' },
        { name: 'Grizzly Bears' },
        { name: 'Bonesplitter', attachedTo: 'Grizzly Bears' },
        { name: 'Leonin Scimitar', attachedTo: 'Grizzly Bears' },
        { name: 'Holy Strength', attachedTo: 'Grizzly Bears', counters: { quest: 3 } },
      ],
      library: ['Plains', 'Plains', 'Plains'],
    },
    player2: { battlefield: [{ name: 'Glory Seeker' }], library: ['Mountain', 'Mountain', 'Mountain'] },
    phase: 'PRECOMBAT_MAIN', activePlayer: 1, priorityPlayer: 1,
    player1StopAtSteps: ['PRECOMBAT_MAIN', 'BEGIN_COMBAT'],
  })
  const p1 = player1.gamePage
  const page = player1.page
  await page.setViewportSize({ width: 480, height: 800 })
  await p1.pass()
  await player2.gamePage.resolveStack('Overseer of Vault 76 trigger')
  await page.locator('button[title^="3 attached"]').click()
  const heading = page.getByRole('heading', { name: 'Attached to Grizzly Bears', exact: true })
  await expect(heading).toBeVisible()
  const plus = page.getByRole('button', { name: 'Allocate one counter from Holy Strength', exact: true })
  const minus = page.getByRole('button', { name: 'Remove one allocation from Holy Strength', exact: true })
  await plus.click()
  await plus.click()
  await minus.click()
  await minus.click()
  await expect(minus).toBeDisabled()
  await expect(heading).toBeVisible()
  await page.mouse.move(5, 5)
  await page.waitForTimeout(400)
  await p1.screenshot('Attached permanent allocation can be corrected back to zero without closing the browser')
  await page.getByRole('button', { name: 'Close attachments', exact: true }).click()
  await expect(page.getByRole('button', { name: 'Select None', exact: true })).toBeVisible()
  await page.locator('button[title^="3 attached"]').click()
  await plus.click()
  await plus.click()
  await plus.click()
  await expect(plus).toBeDisabled()
  await expect(heading).toBeVisible()
  await page.mouse.move(5, 5)
  await page.waitForTimeout(400)
  await p1.screenshot('Three quest counters allocated from the hidden Aura in the attachment browser')
  await page.getByRole('button', { name: 'Close attachments', exact: true }).click()
  await page.getByText('Adjust allocations', { exact: true }).click()
  const listMinus = page.getByRole('button', { name: 'Remove one allocation from Holy Strength in allocation list', exact: true })
  for (let i = 0; i < 3; i++) await listMinus.click()
  await expect(listMinus).toBeDisabled()
  await expect(page.getByRole('button', { name: 'Select None', exact: true })).toBeVisible()
  const listPlus = page.getByRole('button', { name: 'Allocate one counter from Holy Strength in allocation list', exact: true })
  const fallbackBox = await listPlus.boundingBox()
  expect(fallbackBox!.width).toBeGreaterThanOrEqual(24)
  expect(fallbackBox!.height).toBeGreaterThanOrEqual(24)
  await p1.screenshot('Named allocation fallback clears a hidden Aura payment back to zero')
  for (let i = 0; i < 3; i++) await listPlus.click()
  await expect(listPlus).toBeDisabled()
  await page.getByRole('button', { name: 'Confirm (3)', exact: true }).click()
  await player2.gamePage.resolveStack('Overseer of Vault 76 trigger')
  await p1.expectStats('Overseer of Vault 76', '4/4')
})
