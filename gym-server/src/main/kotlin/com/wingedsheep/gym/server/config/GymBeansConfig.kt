package com.wingedsheep.gym.server.config

import com.wingedsheep.engine.limited.BoosterGenerator
import com.wingedsheep.gym.service.MultiEnvService
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.registry.PrintingRegistry
import com.wingedsheep.mtg.sets.MtgSetCatalog
import com.wingedsheep.mtg.sets.tokens.PredefinedTokens
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.MtgSet
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Wires a default [CardRegistry] and [MultiEnvService] as Spring singletons.
 *
 * The catalogue is [MtgSetCatalog.all] — every set the engine knows about — so a
 * constructed deck can name any implemented card and a [DeckSpec.RandomSealed] can
 * draw from any sealed-supported set. This mirrors `game-server`'s `GameBeansConfig`;
 * `MtgSetCatalog` is the single registration point, so newly-added sets show up here
 * automatically with no edit to this file.
 */
@Configuration
class GymBeansConfig {

    @Bean
    fun cardRegistry(): CardRegistry = CardRegistry().apply {
        // Predefined tokens (Treasure, Food, Clue, Map, Incubator, …) are looked up by name at
        // resolution time by `CreatePredefinedTokenExecutor`, which returns an `EffectResult.error`
        // when the name is unregistered — so without this line every card in the corpus that mints
        // one silently minted nothing over the gym API, while `game-server`'s `GameBeansConfig`
        // (which does register them) behaved correctly. Self-play then reported those cards as broken.
        register(PredefinedTokens.allTokens)
        for (set in MtgSetCatalog.all) {
            register(set.cards.stampSetCode(set.code))
            // Basic-land variants are needed for the RandomSealed path so that
            // variant names like "Swamp#BLB-270" resolve during GameInitializer.
            register(set.basicLands)
            set.basicLandsFallback?.let { register(it.basicLands) }
        }
    }

    @Bean
    fun printingRegistry(cardRegistry: CardRegistry): PrintingRegistry = PrintingRegistry().apply {
        // One synthesised printing per registered card (its canonical printing)...
        for (name in cardRegistry.allCardNames()) {
            cardRegistry.getCardsByName(name).forEach(::registerSynthesizedDefault)
        }
        // ...then explicit reprint rows, which overwrite synthesised entries sharing a key.
        for (set in MtgSetCatalog.all) {
            register(set.printings)
        }
    }

    /**
     * Pools are built the way `game-server`'s `GameBeansConfig.boosterGenerator` builds them: a
     * set's own cards plus its reprints, limited to the names Scryfall puts in the set's booster
     * ([BoosterCatalogue]) with those forced `inBooster`. Reading the per-card flag alone left a set
     * Scryfall has not yet flagged (Reality Fracture, before release) drawing from ~15 cards, and
     * left every reprint (a set's fetch and shock lands) unopenable.
     */
    @Bean
    fun boosterGenerator(cardRegistry: CardRegistry): BoosterGenerator = BoosterGenerator(
        MtgSetCatalog.all
            .filter { it.sealedSupported }
            .mapNotNull { set ->
                val pool = set.boosterCardPool(cardRegistry, BoosterCatalogue.limitedCardNames(set.code))
                if (pool.isEmpty()) null else set.code to set.toBoosterSetConfig(pool)
            }
            .toMap()
    )

    @Bean
    fun multiEnvService(
        cardRegistry: CardRegistry,
        boosterGenerator: BoosterGenerator
    ): MultiEnvService = MultiEnvService(cardRegistry, boosterGenerator)
}

/** Stamp a set code onto any card that doesn't already carry one, so printing synthesis keys correctly. */
private fun List<CardDefinition>.stampSetCode(setCode: String): List<CardDefinition> =
    map { if (it.setCode == null) it.copy(setCode = setCode) else it }

/**
 * The cards [this] set contributes to booster / sealed generation — the gym's copy of
 * `game-server`'s `MtgSet.boosterCardPool` (private there, and `gym-server` cannot see
 * `game-server`). Own cards and each distinct reprint resolved to its canonical definition via
 * [registry], overlaid with the reprint's presentation and rarity. With [limitedCardNames] from
 * [BoosterCatalogue], only those names are kept and they are marked `inBooster`; with null, the
 * cards' own `inBooster` flags decide, as before.
 */
private fun MtgSet.boosterCardPool(
    registry: CardRegistry,
    limitedCardNames: Set<String>?,
): List<CardDefinition> {
    val eligibleOwnCards = cards.stampSetCode(code)
        .asSequence()
        .filter { limitedCardNames == null || it.name in limitedCardNames }
        .map { card ->
            if (limitedCardNames == null) card
            else card.copy(metadata = card.metadata.copy(inBooster = true))
        }
        .toList()
    val ownNames = eligibleOwnCards.asSequence().map { it.name }.toHashSet()
    val resolvedReprints = printings
        .asSequence()
        .filter { limitedCardNames == null || it.name in limitedCardNames }
        .filter { it.name !in ownNames }
        .groupBy { it.name }
        .mapNotNull { (_, treatments) ->
            val printing = treatments.firstOrNull { !it.isAlternateFrame && !it.isPromo }
                ?: treatments.first()
            registry.getCardsByName(printing.name).firstOrNull()?.let { canonical ->
                val withArt = canonical.withPrinting(printing)
                withArt.copy(
                    metadata = withArt.metadata.copy(
                        rarity = printing.rarity,
                        inBooster = limitedCardNames != null || withArt.metadata.inBooster,
                    ),
                )
            }
        }
    return (eligibleOwnCards + resolvedReprints).sortedBy { it.name }
}

private fun MtgSet.toBoosterSetConfig(pool: List<CardDefinition>): BoosterGenerator.SetConfig =
    BoosterGenerator.SetConfig(
        setCode = code,
        setName = displayName,
        cards = pool,
        basicLands = (basicLandsFallback ?: this).basicLands,
        incomplete = incomplete,
        block = block,
        boosterStrategy = boosterStrategy,
    )
