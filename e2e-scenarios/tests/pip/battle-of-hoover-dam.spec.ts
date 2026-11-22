import { test, expect } from '../../fixtures/scenarioFixture'
import { PLAYER_BATTLEFIELD, cardByName } from '../../helpers/selectors'

test('Battle of Hoover Dam returns an NCR graveyard target with a finality counter', async ({ createGame }) => {
  test.setTimeout(120_000)
  const { player1, player2 } = await createGame({
    player1Name: 'NCR Commander', player2Name: 'Opponent',
    player1: {
      lifeTotal: 20,
      hand: ['Battle of Hoover Dam'],
      battlefield: Array.from({ length: 4 }, () => ({ name: 'Plains' })),
      graveyard: ['Glory Seeker', 'Hill Giant'],
      library: ['Plains', 'Plains', 'Plains'],
    },
    player2: { lifeTotal: 20, library: ['Mountain', 'Mountain', 'Mountain'] },
    phase: 'POSTCOMBAT_MAIN', activePlayer: 1, priorityPlayer: 1,
    player1StopAtSteps: ['POSTCOMBAT_MAIN', 'END'],
  })
  const p1 = player1.gamePage
  const page = player1.page

  await p1.clickCard('Battle of Hoover Dam')
  await p1.selectAction('Cast')
  await expect(page.getByRole('button', { name: /^NCR/ })).toBeVisible()
  await expect(page.getByRole('button', { name: /^Legion/ })).toBeVisible()
  await p1.screenshot('Choose NCR or Legion as Battle of Hoover Dam enters')
  await p1.selectOption('NCR')
  await p1.expectOnBattlefield('Battle of Hoover Dam')
  await p1.expectNotOnBattlefield('Glory Seeker')

  await p1.pass()
  const heading = page.getByRole('heading', { name: 'Choose from Graveyard', exact: true })
  await expect(heading).toBeVisible()
  const overlay = page.locator('div')
    .filter({ has: heading })
    .filter({ has: page.getByRole('button', { name: /Confirm/ }) })
    .last()
  await expect(overlay.locator(cardByName('Glory Seeker'))).toBeVisible()
  await expect(overlay.locator(cardByName('Hill Giant'))).toHaveCount(0)
  await p1.screenshot('NCR end-step target excludes mana value four Hill Giant')
  await p1.selectCardInZoneOverlay('Glory Seeker')
  await p1.confirmTargets()
  await player2.gamePage.resolveStack('Battle of Hoover Dam trigger')

  await p1.expectOnBattlefield('Glory Seeker')
  await p1.expectNotOnBattlefield('Hill Giant')
  await p1.expectStats('Glory Seeker', '2/2')
  await p1.expectGraveyardSize(player1.playerId, 1)
  const returnedCreature = page.locator(`${PLAYER_BATTLEFIELD} [data-card-id]`)
    .filter({ has: page.locator(cardByName('Glory Seeker')) })
  const finalityBadge = returnedCreature.locator('i.ms-counter-finality').locator('..')
  await expect(finalityBadge).toBeVisible()
  await expect(finalityBadge).toHaveText('1')
  await page.mouse.move(0, 0)
  await p1.screenshot('Glory Seeker returned as a 2/2 with one finality counter')
})
