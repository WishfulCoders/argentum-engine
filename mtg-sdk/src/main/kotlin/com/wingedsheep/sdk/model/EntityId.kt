package com.wingedsheep.sdk.model

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.util.UUID

/**
 * Universal identifier for all game entities.
 * Can represent cards, tokens, players, abilities, emblems, etc.
 *
 * EntityId is the single unified identifier type in the ECS architecture.
 * All game objects (players, cards, tokens, abilities) use this type.
 *
 * A plain final class rather than a `value class`: the engine keeps nearly everything in maps and
 * collections keyed by EntityId, and a value class is boxed afresh every time it enters one, so a
 * single map lookup allocated. That boxing was a fifth of all allocation in an AI game
 * (mtg-draft-ai `docs/56` §3 item 4). Equality, [hashCode] and [toString] are exactly the value
 * class's — those of [value] — so hash-map iteration order is unchanged, and [EntityIdSerializer]
 * writes the same bare string the value class did.
 */
@Serializable(with = EntityIdSerializer::class)
class EntityId(val value: String) {

    override fun equals(other: Any?): Boolean = this === other || (other is EntityId && value == other.value)

    override fun hashCode(): Int = value.hashCode()

    override fun toString(): String = value

    companion object {
        /**
         * Generate a new unique entity ID.
         */
        fun generate(): EntityId = EntityId(UUID.randomUUID().toString())

        /**
         * Create an EntityId from a string value.
         */
        fun of(value: String): EntityId = EntityId(value)
    }
}

/** Serializes an [EntityId] as its bare [EntityId.value], as the former value class did. */
object EntityIdSerializer : KSerializer<EntityId> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("com.wingedsheep.sdk.model.EntityId", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: EntityId) = encoder.encodeString(value.value)

    override fun deserialize(decoder: Decoder): EntityId = EntityId(decoder.decodeString())
}
