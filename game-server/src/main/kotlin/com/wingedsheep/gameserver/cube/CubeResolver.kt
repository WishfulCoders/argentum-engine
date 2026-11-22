package com.wingedsheep.gameserver.cube

import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.registry.PrintingRegistry
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity

/**
 * Resolves user-authored names and optional printing pins against the authoritative registries.
 *
 * Resolution is all-or-nothing: callers receive every miss and no partial playable cube.
 */
class CubeResolver(
    private val cardRegistry: CardRegistry,
    private val printingRegistry: PrintingRegistry,
) {
    fun resolve(cube: CubeList): CubeResolution {
        val unresolved = mutableListOf<UnresolvedCubeCard>()
        val resolvedCards = buildList {
            for (entry in cube.cards) {
                val canonical = cardRegistry.getCard(entry.name)
                if (canonical == null) {
                    unresolved += UnresolvedCubeCard(entry.name, "Card is not implemented")
                    continue
                }

                val resolved = entry.printing?.let { ref ->
                    val printing = printingRegistry.getPrinting(ref)
                    when {
                        printing == null -> {
                            unresolved += UnresolvedCubeCard(
                                entry.name,
                                "Printing ${ref.identifier()} is not available",
                            )
                            null
                        }
                        printing.name != canonical.name -> {
                            unresolved += UnresolvedCubeCard(
                                entry.name,
                                "Printing ${ref.identifier()} belongs to ${printing.name}",
                            )
                            null
                        }
                        else -> canonical.withPrinting(printing)
                    }
                } ?: if (entry.printing == null) canonical else null

                if (resolved != null) {
                    val cubeCard = resolved.forCube(entry.rarity, cube.boosterStyle)
                    repeat(entry.count) { add(cubeCard) }
                }
            }
        }

        if (unresolved.isNotEmpty()) return CubeResolution.Failure(unresolved)
        return CubeResolution.Success(
            ResolvedCube(
                name = cube.name,
                cards = resolvedCards,
                basicLandSetCode = cube.basicLandSetCode,
                packSize = cube.packSize,
                boosterStyle = cube.boosterStyle,
            )
        )
    }

    /**
     * Stamp the cube's rarity onto [this] and mark it booster-eligible: the curator put it in the
     * cube, so a card its own set kept out of boosters (a precon or promo card) still opens. A
     * booster-style cube prints by rarity slot, and packs only have common/uncommon/rare/mythic
     * slots — so a special or bonus-sheet rarity is printed as a rare rather than never opening.
     */
    private fun CardDefinition.forCube(override: Rarity?, style: CubeBoosterStyle): CardDefinition {
        val rarity = override ?: metadata.rarity
        val printable = if (style.printsPacks && rarity !in PACK_RARITIES) Rarity.RARE else rarity
        if (printable == metadata.rarity && metadata.inBooster) return this
        return copy(metadata = metadata.copy(rarity = printable, inBooster = true))
    }

    private companion object {
        val PACK_RARITIES = setOf(Rarity.COMMON, Rarity.UNCOMMON, Rarity.RARE, Rarity.MYTHIC)
    }
}

sealed interface CubeResolution {
    data class Success(val cube: ResolvedCube) : CubeResolution
    data class Failure(val unresolved: List<UnresolvedCubeCard>) : CubeResolution
}

data class UnresolvedCubeCard(
    val name: String,
    val reason: String,
)
