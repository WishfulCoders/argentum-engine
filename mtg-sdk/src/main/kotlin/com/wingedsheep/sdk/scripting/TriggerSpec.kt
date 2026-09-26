package com.wingedsheep.sdk.scripting

import kotlinx.serialization.Serializable

/**
 * Bundles an [EventPattern] with a [TriggerBinding] to fully specify when a triggered ability fires.
 *
 * Cards never build one directly: `trigger = Triggers.self.enters()` — a subject and a verb from
 * [com.wingedsheep.sdk.dsl.Triggers] — produces it, and the subject fixes the binding.
 */
@Serializable
data class TriggerSpec(
    val event: EventPattern,
    val binding: TriggerBinding = TriggerBinding.SELF
)
