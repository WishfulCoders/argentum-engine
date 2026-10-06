import { test, expect } from '../../fixtures/scenarioFixture'

test('spell copies can keep their original player target', async ({ createGame }) => {
  test.setTimeout(60_000)
  const { player1, player2 } = await createGame({
    player1Name: 'Copier', player2Name: 'Opponent',
    player1: {
      hand: ['Ancestral Recall', 'Grapeshot'],
      battlefield: [{ name: 'Island' }, { name: 'Mountain' }, { name: 'Mountain' }],
      library: ['Island', 'Island', 'Island', 'Island', 'Island', 'Island'],
    },
    player2: { library: ['Forest', 'Forest', 'Forest'] },
    phase: 'PRECOMBAT_MAIN', activePlayer: 1,
  })
  const p1 = player1.gamePage
  await p1.clickCard('Ancestral Recall')
  await p1.selectAction('Cast Ancestral Recall')
  await p1.selectPlayer(player1.playerId)
  await p1.confirmTargets()
  await player2.gamePage.resolveStack('Ancestral Recall')
  await p1.expectNotInHand('Ancestral Recall')
  await p1.clickCard('Grapeshot')
  await p1.selectAction('Cast Grapeshot')
  await p1.selectPlayer(player2.playerId)
  await p1.confirmTargets()
  await player2.gamePage.pass()
  const keep = player1.page.getByRole('button', { name: 'Keep original target', exact: true })
  await expect(keep).toBeVisible()
  await player1.page.screenshot({ path: test.info().outputPath('keep-original-target.png') })
  await keep.click()
  await player2.gamePage.pass()
  await player2.gamePage.pass()
  await p1.expectLifeTotal(player2.playerId, 18)
})
