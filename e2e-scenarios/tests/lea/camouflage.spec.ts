import { test, expect } from '../../fixtures/scenarioFixture'

test.setTimeout(120_000)
test.use({ channel: 'chrome', viewport: { width: 1440, height: 1000 } })

test('Camouflage chooses optional piles on the battlefield and assigns them at random', async ({ createGame }) => {
  const { player1, player2 } = await createGame({
    player1: { hand: ['Camouflage'], battlefield: [{ name: 'Forest' }, { name: 'Grizzly Bears' }, { name: 'Savannah Lions' }], library: ['Forest', 'Forest'] },
    player2: { battlefield: [{ name: 'Wall of Wood' }, { name: 'Giant Spider' }], library: ['Forest', 'Forest'] },
    phase: 'COMBAT', step: 'DECLARE_ATTACKERS', activePlayer: 1, priorityPlayer: 1,
    player1StopAtSteps: ['DECLARE_ATTACKERS', 'DECLARE_BLOCKERS'], player2StopAtSteps: ['DECLARE_ATTACKERS', 'DECLARE_BLOCKERS'],
    player1OpponentStopAtSteps: ['DECLARE_ATTACKERS', 'DECLARE_BLOCKERS'], player2OpponentStopAtSteps: ['DECLARE_ATTACKERS', 'DECLARE_BLOCKERS'],
  })
  player1.page.setDefaultTimeout(15_000)
  player2.page.setDefaultTimeout(15_000)
  await player1.gamePage.declareAttacker('Grizzly Bears')
  await player1.gamePage.declareAttacker('Savannah Lions')
  await player1.page.getByRole('button', { name: 'Attack with 2', exact: true }).click()
  await player1.gamePage.clickCard('Camouflage')
  await player1.gamePage.selectAction('Cast')
  await player2.gamePage.pass()
  await player1.page.getByRole('button', { name: 'To Blockers', exact: true }).click()
  await player2.gamePage.pass()
  await player2.page.getByRole('button', { name: 'Choose blocker piles', exact: true }).click()
  await expect(player2.page.getByTestId('blocker-pile-0')).toBeVisible()
  await player2.gamePage.clickCard('Wall of Wood')
  await expect(player2.page.getByTestId('blocker-pile-0')).toHaveText('Pile 1 (1)')
  await player2.page.getByTestId('blocker-pile-1').click()
  await expect(player2.page.getByTestId('blocker-pile-1')).toHaveText('Pile 2 (0)')
  await player2.gamePage.clickCard('Giant Spider')
  await expect(player2.page.getByTestId('blocker-pile-1')).toHaveText('Pile 2 (1)')
  await expect(player1.page.getByRole('button', { name: 'Assign piles at random', exact: true })).toHaveCount(0)
  await player2.gamePage.screenshot('Two blocker piles selected')
  await player2.page.getByRole('button', { name: 'Assign piles at random', exact: true }).click()
  await expect(player2.page.getByRole('button', { name: 'Assign piles at random', exact: true })).toHaveCount(0)
})
