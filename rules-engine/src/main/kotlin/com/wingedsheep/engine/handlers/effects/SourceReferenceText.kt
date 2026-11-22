package com.wingedsheep.engine.handlers.effects

import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.view.projection.StackTextRenderer
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.effects.SelfReferentialDescription
import com.wingedsheep.sdk.scripting.targets.DEFAULT_SELF_NOUN
import com.wingedsheep.sdk.scripting.targets.SELF_NOUN_TOKEN
import com.wingedsheep.sdk.scripting.targets.resolveSelfNoun

/**
 * Turns an effect's generated text into decision-prompt text that names the permanent it is about.
 *
 * The SDK writes a source permanent generically — the self-noun placeholder, its default "this
 * permanent", or the legacy "this creature" — because it doesn't know the card. A prompt does: it
 * names the card ("If you don't, tap Multiversal Passage", "exile PuPu UFO"), which is both
 * unambiguous and never calls a land "this creature". Quoted ability text is left alone — inside a
 * granted "When this creature dies, …" the noun means whatever gains the ability, not the source.
 */
object SourceReferenceText {

    private val SELF_NOUNS = listOf(
        SELF_NOUN_TOKEN, "this creature", "this permanent", "this land", "this artifact",
        "this enchantment", "this planeswalker", "this battle"
    )

    /**
     * How a prompt should refer to [sourceId]: its name, or — when the permanent is face down and
     * its name is hidden information — the type noun ("this creature"). Null when there is no
     * source to name.
     */
    fun referenceFor(state: GameState, sourceId: EntityId?): String? {
        val entity = sourceId?.let { state.getEntity(it) } ?: return null
        if (entity.has<FaceDownComponent>()) return StackTextRenderer.selfNounFor(state, sourceId)
        return entity.get<CardComponent>()?.name
    }

    /** [effect]'s text, sentence-cased as generated, with source references replaced by [reference]. */
    fun sentence(effect: Effect, reference: String?): String {
        val raw = (effect as? SelfReferentialDescription)?.descriptionTemplate ?: effect.description
        return name(raw, reference)
    }

    /**
     * [effect]'s text as a clause to splice mid-sentence ("Pay 2 life or exile PuPu UFO?"): first
     * letter lowered *before* the card name goes in (so a name keeps its capital), trailing period
     * dropped.
     */
    fun clause(effect: Effect, reference: String?): String {
        val raw = (effect as? SelfReferentialDescription)?.descriptionTemplate ?: effect.description
        return name(raw.trim().removeSuffix(".").replaceFirstChar { it.lowercaseChar() }, reference)
    }

    /** Replace every unquoted source reference in [text] with [reference] (or the default noun). */
    fun name(text: String, reference: String?): String {
        if (reference == null) return resolveSelfNoun(text, DEFAULT_SELF_NOUN)
        val out = StringBuilder()
        // Split on double quotes: even-indexed pieces are outside quotes.
        text.split('"').forEachIndexed { index, piece ->
            if (index > 0) out.append('"')
            out.append(if (index % 2 == 0) SELF_NOUNS.fold(piece) { acc, noun -> acc.replace(noun, reference) } else piece)
        }
        return out.toString()
    }
}
