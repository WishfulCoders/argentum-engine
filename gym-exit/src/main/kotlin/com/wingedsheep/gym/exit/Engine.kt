package com.wingedsheep.gym.exit

import com.wingedsheep.engine.core.GameConfig
import com.wingedsheep.engine.core.PlayerConfig
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.gym.GameEnvironment
import com.wingedsheep.gym.GameGymEnv
import com.wingedsheep.gym.contract.ObservationResult
import com.wingedsheep.gym.service.DeckResolver
import com.wingedsheep.gym.service.EnvConfig
import com.wingedsheep.mtg.sets.MtgSetCatalog
import com.wingedsheep.mtg.sets.tokens.PredefinedTokens

/**
 * The gym server's game envs without the server: the card registry `GymBeansConfig` wires, and env creation as
 * `MultiEnvService.create` does it, so an [EnvConfig] the Python side would POST plays the same game here.
 */
class Engine {
    val cardRegistry: CardRegistry = CardRegistry().apply {
        register(PredefinedTokens.allTokens)
        for (set in MtgSetCatalog.all) {
            register(set.cards.map { if (it.setCode == null) it.copy(setCode = set.code) else it })
            register(set.basicLands)
            set.basicLandsFallback?.let { register(it.basicLands) }
        }
    }
    private val decks = DeckResolver(cardRegistry)

    /** A new game env at its opening learner decision. */
    fun create(config: EnvConfig): Pair<GameGymEnv, ObservationResult> {
        val env = GameGymEnv(
            GameEnvironment.create(cardRegistry), config.perspectivePlayerIndex, config.revealAll,
            limits = config.limits, agents = config.players.map { it.agent },
        )
        val gameConfig = GameConfig(
            players = config.players.map { spec ->
                PlayerConfig(
                    name = spec.name,
                    deck = decks.resolve(spec.deck),
                    startingLife = spec.startingLife,
                    playerId = spec.playerId,
                )
            },
            startingHandSize = config.startingHandSize,
            skipMulligans = config.skipMulligans,
            useHandSmoother = config.useHandSmoother,
            startingPlayerIndex = config.startingPlayerIndex,
            seed = config.seed,
        )
        return env to env.reset(gameConfig)
    }
}
