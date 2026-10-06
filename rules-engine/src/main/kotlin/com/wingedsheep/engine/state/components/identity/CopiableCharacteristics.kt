package com.wingedsheep.engine.state.components.identity

import com.wingedsheep.engine.state.Component
import com.wingedsheep.engine.state.ComponentContainer
import kotlinx.serialization.Serializable
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.TypeLine
import com.wingedsheep.sdk.model.CreatureStats

/**
 * Copy effects read face-down characteristics before the upright half of a flipped card.
 * Face-down values are copiable; flipped status itself is not (CR 707.2, 708.2a).
 * A separate definition id prevents ability lookups from exposing the hidden rules text.
 */
fun ComponentContainer.copiableCardComponent(): CardComponent? {
    val card = get<CardComponent>() ?: return null
    if (has<FaceDownComponent>()) {
        val mode = get<FaceDownModeComponent>()?.mode
        val ward = mode?.faceDownWard
        return CardComponent(
            cardDefinitionId = "face-down",
            name = "",
            manaCost = ManaCost.ZERO,
            typeLine = TypeLine.parse("Creature"),
            baseStats = CreatureStats(2, 2),
            baseKeywords = if (ward == null) emptySet() else setOf(Keyword.WARD),
            copyWardCosts = listOfNotNull(ward),
            ownerId = card.ownerId,
            imageUri = mode?.helperCardImageUri,
        )
    }
    return get<FlippedComponent>()?.unflippedCard ?: card
}

/** A face-down double-faced source still makes a double-faced token, with only public values. */
fun ComponentContainer.copiableDoubleFacedComponent(
    copy: (CardComponent) -> CardComponent,
): DoubleFacedComponent? {
    val dfc = get<DoubleFacedComponent>() ?: return null
    val faces = if (has<FaceDownComponent>()) {
        val publicCard = copiableCardComponent() ?: return null
        CopiedCardFaces(copy(publicCard), copy(publicCard))
    } else {
        dfc.copiedFaces?.let { CopiedCardFaces(copy(it.front), copy(it.back)) }
    }
    return if (faces == null) dfc.copy(frontFaceCard = null, faceChanges = 0) else DoubleFacedComponent(
        frontCardDefinitionId = faces.front.cardDefinitionId,
        backCardDefinitionId = faces.back.cardDefinitionId,
        currentFace = if (has<FaceDownComponent>()) DoubleFacedComponent.Face.FRONT else dfc.currentFace,
        copiedFaces = faces,
    )
}

/**
 * A permanent's copiable values as they last existed on the battlefield (CR 608.2h, CR 707.2),
 * kept on the departed card for the rest of its stay in the new zone.
 *
 * Leaving the battlefield rewrites what the card *presents*: a copy effect ends and it is its
 * printed self again (CR 400.7), a transformed or flipped card is its front / upright half again
 * (CR 712.8a, CR 710.4), a face-down permanent is turned face up. An effect that copies a
 * *permanent* it has just moved — Fractured Identity: "Exile target nonland permanent. Each player
 * other than its controller creates a token that's a copy of it." — must copy what that permanent
 * was, not what the card in exile now is (the Fractured Identity ruling: "If the copied permanent
 * was copying something else, the tokens enter the battlefield as whatever that permanent was
 * copying").
 *
 * [components] are the pre-departure copy-relevant components — the [CardComponent] plus whichever
 * of [FaceDownComponent], [FaceDownModeComponent], [FlippedComponent], [DoubleFacedComponent],
 * [ToxicComponent] and [NumericKeywordValuesComponent] it had — so a copy path reads them through
 * the same [copiableCardComponent] / [copiableDoubleFacedComponent] /
 * `CopyExceptionApplier.withNumericKeywords` helpers it uses for a live permanent.
 *
 * Stamped by `ZoneTransitionService` only when the departure actually changed the card's copiable
 * values; stripped on the card's next zone change (a new object again, CR 400.7). Read only for a
 * target chosen as a *permanent* that has since left the battlefield — a card later targeted in its
 * new zone is a new object whose copiable values are its own.
 */
@Serializable
data class LastKnownCopiableComponent(
    val components: List<Component>,
) : Component {
    /** The pre-departure object as a container the copy helpers can read. */
    fun asContainer(): ComponentContainer = ComponentContainer.of(*components.toTypedArray())

    companion object {
        /**
         * The last-known copiable view of [before] (the entity as it was on the battlefield), or
         * null when leaving changed nothing a copy would read — [after] is the departed entity.
         */
        fun capture(before: ComponentContainer, after: ComponentContainer?): LastKnownCopiableComponent? {
            val beforeCard = before.copiableCardComponent() ?: return null
            val afterCard = after?.copiableCardComponent()
            val beforeFace = before.get<DoubleFacedComponent>()?.currentFace
            val afterFace = after?.get<DoubleFacedComponent>()?.currentFace
            if (beforeCard == afterCard && beforeFace == afterFace &&
                before.get<ToxicComponent>() == after?.get<ToxicComponent>() &&
                before.get<NumericKeywordValuesComponent>() == after?.get<NumericKeywordValuesComponent>()
            ) return null
            return LastKnownCopiableComponent(
                listOfNotNull(
                    before.get<CardComponent>(),
                    FaceDownComponent.takeIf { before.has<FaceDownComponent>() },
                    before.get<FaceDownModeComponent>(),
                    before.get<FlippedComponent>(),
                    before.get<DoubleFacedComponent>(),
                    before.get<ToxicComponent>(),
                    before.get<NumericKeywordValuesComponent>(),
                )
            )
        }
    }
}
