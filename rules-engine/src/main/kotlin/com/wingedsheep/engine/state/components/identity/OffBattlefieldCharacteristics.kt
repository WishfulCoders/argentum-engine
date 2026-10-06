package com.wingedsheep.engine.state.components.identity

import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.Component
import com.wingedsheep.engine.state.ComponentContainer
import com.wingedsheep.sdk.core.CardType
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.TypeLine
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.CreatureStats
import com.wingedsheep.sdk.scripting.CreatureOutsideBattlefield
import kotlinx.serialization.Serializable

/**
 * The type line and base power/toughness a card had before
 * [OffBattlefieldCharacteristics.leaveBattlefield] added its [CreatureOutsideBattlefield]
 * characteristics — what [OffBattlefieldCharacteristics.enterBattlefield] restores. Present exactly
 * while the additions are applied, so applying twice is a no-op.
 */
@Serializable
data class OffBattlefieldCharacteristicsComponent(
    val printedTypeLine: TypeLine,
    val printedStats: CreatureStats?,
) : Component

/**
 * Grist, the Hunger Tide's "As long as Grist isn't on the battlefield, it's a 1/1 Insect creature in
 * addition to its other types" ([CreatureOutsideBattlefield]).
 *
 * The ability functions everywhere except the battlefield (CR 113.6c). Objects outside the
 * battlefield have no projected state — every rule that asks "is this a creature card / creature
 * spell" (library searches, graveyard counts, "counter target creature spell", "whenever you cast a
 * creature spell") reads the card's own [CardComponent] — so the additions are written into the
 * [CardComponent] for as long as the card is outside the battlefield:
 *
 *  - [leaveBattlefield] when the card is created outside the battlefield (game start) and whenever
 *    it moves from the battlefield to any other zone;
 *  - [enterBattlefield] as it becomes a permanent (spell resolution or "put onto the battlefield"),
 *    restoring the printed characteristics before anything looks at the entering permanent — so it
 *    never enters as a creature and "whenever a creature enters" doesn't see it.
 *
 * Moves between two non-battlefield zones (hand → stack → graveyard) keep the additions, which is
 * what makes a cast Grist a creature spell.
 */
object OffBattlefieldCharacteristics {

    /** Apply the card's [CreatureOutsideBattlefield] additions, if it has any and they aren't applied. */
    fun leaveBattlefield(container: ComponentContainer, cardRegistry: CardRegistry): ComponentContainer {
        val card = container.get<CardComponent>() ?: return container
        return leaveBattlefield(container, cardRegistry.getCard(card.cardDefinitionId))
    }

    /** [leaveBattlefield] with the card's definition already in hand (card creation). */
    fun leaveBattlefield(container: ComponentContainer, cardDef: CardDefinition?): ComponentContainer {
        if (container.has<OffBattlefieldCharacteristicsComponent>()) return container
        val card = container.get<CardComponent>() ?: return container
        // A face-down card has no characteristics but those its face-down rules give it (CR 708.2).
        if (container.has<FaceDownComponent>()) return container
        val ability = cardDef
            ?.script?.staticAbilities
            ?.filterIsInstance<CreatureOutsideBattlefield>()
            ?.firstOrNull()
            ?: return container
        val typeLine = card.typeLine.copy(
            cardTypes = card.typeLine.cardTypes + CardType.CREATURE,
            subtypes = card.typeLine.subtypes + ability.subtypes.map { Subtype(it) },
        )
        return container
            .with(OffBattlefieldCharacteristicsComponent(card.typeLine, card.baseStats))
            .with(card.copy(typeLine = typeLine, baseStats = CreatureStats(ability.power, ability.toughness)))
    }

    /** Restore the printed characteristics as the card enters the battlefield. No-op when none were added. */
    fun enterBattlefield(container: ComponentContainer): ComponentContainer {
        val snapshot = container.get<OffBattlefieldCharacteristicsComponent>() ?: return container
        val card = container.get<CardComponent>() ?: return container.without<OffBattlefieldCharacteristicsComponent>()
        return container
            .with(card.copy(typeLine = snapshot.printedTypeLine, baseStats = snapshot.printedStats))
            .without<OffBattlefieldCharacteristicsComponent>()
    }
}
