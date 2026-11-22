package com.wingedsheep.gameserver.cube

import com.wingedsheep.engine.limited.BoosterGenerator
import com.wingedsheep.sdk.limited.BoosterStrategy

/**
 * Builds the synthetic set config that lets existing limited infrastructure describe a cube. A
 * booster-style cube prints its packs through this config's strategy exactly as a catalogued set
 * does; a shuffled cube is dealt by `CubeDealer` and its strategy must never run.
 */
object CubeSetConfig {
    const val SET_CODE = "CUBE"

    private val unsupportedBoosterStrategy = BoosterStrategy { _, _ ->
        error("Shuffled cube packs must be dealt through CubeDealer, not BoosterStrategy")
    }

    fun of(
        cube: ResolvedCube,
        boosterGenerator: BoosterGenerator,
    ): BoosterGenerator.SetConfig {
        val basicLandSource = requireNotNull(
            boosterGenerator.getSetConfig(cube.basicLandSetCode)
        ) {
            "Unknown cube basic-land set code: ${cube.basicLandSetCode}"
        }

        return BoosterGenerator.SetConfig(
            setCode = SET_CODE,
            setName = cube.name,
            cards = cube.cards,
            basicLands = basicLandSource.basicLands,
            incomplete = false,
            sealedSupported = true,
            extensionSet = false,
            boosterStrategy = cube.boosterStyle.strategy ?: unsupportedBoosterStrategy,
            variantChance = 0.0,
        )
    }
}
