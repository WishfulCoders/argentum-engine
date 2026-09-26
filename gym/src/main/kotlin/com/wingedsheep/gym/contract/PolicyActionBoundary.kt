package com.wingedsheep.gym.contract

import com.wingedsheep.ai.engine.GameSimulator
import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.legalactions.LegalAction
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.ClassLevelComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.ManaSymbol
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import com.wingedsheep.sdk.scripting.AlternativePaymentChoice
import com.wingedsheep.sdk.scripting.ConvokePayment
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.CostReductionSource
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget

/** The action mask shared by the arena BC bridge and learner-seat gym observations. */
object PolicyActionBoundary {
    fun requirePolicyPayment(action: LegalAction, params: ActionParams) {
        val options = action.policyBlightTargetOptions
        require(params.blightTarget == null || params.blightTarget in options) {
            "blightTarget is not an engine-checked option for this action"
        }
        require(options.isEmpty() || params.blightTarget != null) {
            "Blight activation requires blightTarget"
        }
        val tapOptions = action.policyTapPaymentOptions
        require(params.tappedPermanents.isEmpty() || params.tappedPermanents in tapOptions) {
            "tappedPermanents is not an engine-checked option for this action"
        }
        require(tapOptions.isEmpty() || params.tappedPermanents.isNotEmpty()) {
            "TapPermanents activation requires tappedPermanents"
        }
        val beholdOptions = action.policyBeholdPaymentOptions
        require(params.beheldCards.isEmpty() ||
            (params.targets.size == 1 && params.beheldCards in beholdOptions[params.targets.single()].orEmpty())) {
            "beheldCards is not an engine-checked option for this target"
        }
        require(beholdOptions.isEmpty() || params.beheldCards.isNotEmpty()) {
            "Behold cast requires beheldCards"
        }
    }

    fun callable(action: LegalAction): Boolean =
        action.actionType !in setOf("CrewVehicle", "SaddleMount") &&
            additionalCostCallable(action) &&
            convokeCallable(action) &&
            !action.hasDelve &&
            !action.hasTapForGeneric &&
            !action.hasHarmonize &&
            !action.requiresManaColorChoice &&
            action.manaCostPerExtraTarget == null

    fun mask(actions: List<LegalAction>): List<LegalAction> = actions.map { action ->
        if (callable(action)) action else action.copy(affordable = false)
    }

    /** Share the same bounded, engine-checked convoke choices with arena and learner gym. */
    fun mask(actions: List<LegalAction>, state: GameState, simulator: GameSimulator): List<LegalAction> {
        val battlefieldPricesTargets by lazy { battlefieldPricesTargets(state, simulator.cardRegistry) }
        // Every permanent the auto-payer could tap or sacrifice for mana: a spell that may target one is paid
        // for differently depending on the target.
        val manaSources by lazy {
            actions.filter { it.isManaAbility }.mapNotNull { (it.action as? ActivateAbility)?.sourceId }.toSet()
        }
        return mask(actions.map { action ->
            var prepared = targetPriced(action, state, simulator, { battlefieldPricesTargets }, { manaSources })
            if (action.hasConvoke && !action.canPayWithoutConvoke) {
                prepared = prepared.copy(policyConvokePaymentOptions = convokeOptions(action, state, simulator))
            }
            if (action.additionalCostInfo?.costType == "Blight") {
                prepared = prepared.copy(policyBlightTargetOptions = blightOptions(action, state, simulator))
            }
            if (action.additionalCostInfo?.costType == "TapPermanents") {
                prepared = prepared.copy(policyTapPaymentOptions = tapOptions(action, state, simulator))
            }
            if (action.additionalCostInfo?.costType == "Behold") {
                prepared = prepared.copy(policyBeholdPaymentOptions = beholdOptions(action, state, simulator))
            }
            preflightParameterFree(prepared, state, simulator)
        })
    }

    /**
     * A cast or non-mana activation the policy submits exactly as enumerated, run through the final
     * preflight here, and masked if the engine would refuse it at the step.
     *
     * With no targets, X or payment to choose, there is exactly one submission, so the step's own check
     * ([PolicyActionStager]: accepted directly, or after floating Treasure mana) can be run in advance.
     * Enumerated affordability misses costs it does not model: the first live PPO iterations
     * (mtg-draft-ai `docs/51` §3.1) found Group Project's flashback, whose "tap three untapped creatures"
     * the flashback enumerator prices as {0}, and a {R}{R/W}{W} creature the mana solver passes and the
     * cast refuses. Each rejected the whole step batch. Checking every parameter-free action is what closes
     * the class rather than one card.
     *
     * A simple single-target cast is checked the same way with its first advertised target: whether the
     * cost can be paid does not depend on which legal target is named, unless the target prices the spell,
     * and [targetPriced] has already checked those target by target. Dismember ({1}{B/P}{B/P}) was the
     * case: advertised as affordable, refused at every target.
     */
    private fun preflightParameterFree(action: LegalAction, state: GameState, simulator: GameSimulator): LegalAction {
        if (!action.affordable || !callable(action)) return action
        if (action.isManaAbility && !costsMoreThanTap(action, state, simulator.cardRegistry)) return action
        val fixedShape = !action.hasXCost && action.additionalCostInfo == null && !action.hasConvoke &&
            action.modalEnumeration == null && !action.requiresDamageDistribution
        if (!fixedShape) return action
        if (action.action !is CastSpell && action.action !is ActivateAbility) return action
        val params = when {
            !action.requiresTargets -> ActionParams()
            action.action is CastSpell && (action.action as CastSpell).targets.isEmpty() &&
                action.minTargets == 1 && action.targetCount == 1 &&
                action.targetRequirements.orEmpty().isEmpty() && !action.validTargets.isNullOrEmpty() ->
                ActionParams(targets = listOf(action.validTargets!!.first()))
            else -> return action
        }
        val completed = ActionParameterizer.apply(action.action, params, state)
        return if (PolicyActionStager(simulator).begin(state, completed) != null) action else action.copy(affordable = false)
    }

    /**
     * A mana ability with a cost beyond {T}, which the enumerator may not price.
     *
     * Rubble Rouser's "{T}, Exile a card from your graveyard: Add {R}" was advertised as affordable with an
     * empty graveyard, and refused at the step (mtg-draft-ai `docs/43` §5.1, the smoke's negated arm). A plain
     * {T} ability, nearly every mana ability in a game, skips the preflight: it costs one simulated step per
     * ability per observation. An ability the source's script does not list (a granted one) is not checked.
     */
    private fun costsMoreThanTap(action: LegalAction, state: GameState, registry: CardRegistry): Boolean {
        val activation = action.action as? ActivateAbility ?: return false
        val card = state.getEntity(activation.sourceId)?.get<CardComponent>() ?: return false
        val ability = registry.getCard(card.cardDefinitionId)?.script?.activatedAbilities
            ?.firstOrNull { it.id == activation.abilityId } ?: return false
        return ability.cost != AbilityCost.Tap
    }

    /**
     * A cast whose cost the chosen target changes, narrowed to the targets the engine accepts.
     *
     * Affordability is enumerated before targets are chosen. For a target-conditional reduction the
     * enumerator assumes the cheapest target (`CostCalculator.calculateMinPossibleCost`): Ajani's Response
     * shows as castable for {1}{W} whenever any creature is tapped, but it still offers every creature, and an
     * untapped one costs {4}{W} and fails the final preflight. Found by the first live PPO iteration
     * (mtg-draft-ai `docs/51` §3.1). A simple single-target cast is preflighted target by target and keeps the
     * accepted ones; a multi-target, X or modal cast priced by its targets is masked, since its advertised
     * targets cannot be checked one at a time. Casts whose cost no target changes are untouched, so an
     * ordinary observation pays nothing.
     *
     * A cast that may target one of the caster's own mana sources is checked the same way, because paying for
     * it may need that source: Suspend Aggression ({1}{R}{W}) aimed at the caster's only Treasure, with two
     * other lands, was advertised as affordable and refused (mtg-draft-ai `docs/43` §5.3).
     */
    private fun targetPriced(
        action: LegalAction,
        state: GameState,
        simulator: GameSimulator,
        battlefieldPricesTargets: () -> Boolean,
        manaSources: () -> Set<EntityId>,
    ): LegalAction {
        val cast = action.action as? CastSpell ?: return action
        if (!action.affordable || !action.requiresTargets || cast.targets.isNotEmpty()) return action
        if (!selfPricedByTarget(cast, state, simulator.cardRegistry) && !battlefieldPricesTargets() &&
            action.validTargets.orEmpty().none { it in manaSources() }
        ) return action
        val simple = !action.hasXCost && action.minTargets == 1 && action.targetCount == 1 &&
            action.targetRequirements.orEmpty().isEmpty() && action.modalEnumeration == null &&
            !action.requiresDamageDistribution && action.additionalCostInfo == null && !action.hasConvoke &&
            action.validTargets.orEmpty().size in 1..MAX_PRICED_TARGETS
        if (!simple) return action.copy(affordable = false)
        val accepted = action.validTargets.orEmpty().distinct().filter { target ->
            val completed = ActionParameterizer.apply(cast, ActionParams(targets = listOf(target)), state)
            PolicyActionStager(simulator).begin(state, completed) != null
        }
        return if (accepted.isEmpty()) action.copy(affordable = false) else action.copy(validTargets = accepted)
    }

    private fun staticAbilitiesOf(state: GameState, entityId: EntityId, registry: CardRegistry) =
        state.getEntity(entityId)?.let { container ->
            val card = container.get<CardComponent>() ?: return@let null
            registry.getCard(card.cardDefinitionId)?.script
                ?.effectiveStaticAbilities(container.get<ClassLevelComponent>()?.currentLevel)
        }.orEmpty()

    private fun pricesByTarget(ability: ModifySpellCost): Boolean =
        ability.target is SpellCostTarget.OpponentsCastTargeting || when (val modification = ability.modification) {
            is CostModification.ReduceColoredIfAnyTargetMatches,
            is CostModification.IncreaseGenericIfAnyTargetMatches -> true
            is CostModification.ReduceGenericBy -> modification.source is CostReductionSource.FixedIfAnyTargetMatches
            else -> false
        }

    /** The spell's own "costs less (or more) if it targets ..." ability. */
    private fun selfPricedByTarget(cast: CastSpell, state: GameState, registry: CardRegistry): Boolean =
        staticAbilitiesOf(state, cast.cardId, registry).any {
            it is ModifySpellCost && it.target == SpellCostTarget.SelfCast && pricesByTarget(it)
        }

    /** A permanent that prices other spells by what they target ("spells that target ... cost more"). */
    private fun battlefieldPricesTargets(state: GameState, registry: CardRegistry): Boolean =
        state.turnOrder.any { player ->
            state.getBattlefield(player).any { permanent ->
                staticAbilitiesOf(state, permanent, registry).any {
                    it is ModifySpellCost && it.target != SpellCostTarget.SelfCast && pricesByTarget(it)
                }
            }
        }

    /** Bound the cross product and preflight each complete target/payment pair. */
    private fun beholdOptions(
        action: LegalAction, state: GameState, simulator: GameSimulator,
    ): Map<EntityId, List<List<EntityId>>> {
        val cast = action.action as? CastSpell ?: return emptyMap()
        val info = action.additionalCostInfo ?: return emptyMap()
        if (!action.affordable || action.hasXCost || !action.requiresTargets ||
            action.minTargets != 1 || action.targetCount != 1 ||
            action.targetRequirements.orEmpty().isNotEmpty() || cast.targets.isNotEmpty() ||
            action.modalEnumeration != null || action.requiresDamageDistribution ||
            action.hasConvoke || action.hasDelve || action.hasTapForGeneric ||
            action.hasHarmonize || action.requiresManaColorChoice ||
            action.manaCostPerExtraTarget != null || info.costType != "Behold" ||
            info.beholdCount !in 1..MAX_BEHOLD_COUNT ||
            info.validBeholdTargets.size > MAX_BEHOLD_CANDIDATES ||
            action.validTargets.orEmpty().size !in 1..MAX_BEHOLD_TARGETS
        ) return emptyMap()
        val candidates = info.validBeholdTargets.distinct().sortedBy { it.value }
        val targets = action.validTargets.orEmpty().distinct().sortedBy { it.value }
        val payments = mutableListOf<List<EntityId>>()
        fun visit(start: Int, chosen: List<EntityId>) {
            if (chosen.size == info.beholdCount) {
                payments.add(chosen)
                return
            }
            for (index in start until candidates.size) visit(index + 1, chosen + candidates[index])
        }
        visit(0, emptyList())
        if (payments.isEmpty()) return emptyMap()
        val accepted = linkedMapOf<EntityId, List<List<EntityId>>>()
        for (target in targets) {
            val options = payments.filter { payment ->
                val completed = ActionParameterizer.apply(
                    cast, ActionParams(targets = listOf(target), beheldCards = payment), state,
                )
                simulator.accepts(state, completed)
            }
            // The model can choose any advertised target. Keep this action masked unless every
            // target has a complete, accepted Behold payment.
            if (options.isEmpty()) return emptyMap()
            accepted[target] = options
        }
        return accepted
    }

    /** At most 56 selections: eight candidates choose up to three permanents. */
    private fun tapOptions(
        action: LegalAction, state: GameState, simulator: GameSimulator,
    ): List<List<EntityId>> {
        val activation = action.action as? ActivateAbility ?: return emptyList()
        val info = action.additionalCostInfo ?: return emptyList()
        if (!action.affordable || action.hasXCost || action.requiresTargets ||
            action.targetRequirements.orEmpty().isNotEmpty() || action.hasConvoke || action.hasDelve ||
            action.hasTapForGeneric || action.hasHarmonize || action.requiresManaColorChoice ||
            info.costType != "TapPermanents" || info.tapCount !in 1..MAX_TAP_COUNT ||
            info.tapBatchMaxActivations != 1 || info.validTapTargets.size > MAX_TAP_CANDIDATES
        ) return emptyList()
        val candidates = info.validTapTargets.distinct().sortedBy { it.value }
        val accepted = mutableListOf<List<EntityId>>()
        fun visit(start: Int, chosen: List<EntityId>) {
            if (chosen.size == info.tapCount) {
                val payment = (activation.costPayment ?: AdditionalCostPayment())
                    .copy(tappedPermanents = chosen)
                if (simulator.accepts(state, activation.copy(costPayment = payment))) {
                    accepted.add(chosen)
                }
                return
            }
            for (index in start until candidates.size) visit(index + 1, chosen + candidates[index])
        }
        visit(0, emptyList())
        return accepted
    }

    /** Only a simple, untargeted activation can be preflighted as a complete move here. */
    private fun blightOptions(
        action: LegalAction, state: GameState, simulator: GameSimulator,
    ): List<EntityId> {
        val activation = action.action as? ActivateAbility ?: return emptyList()
        val info = action.additionalCostInfo ?: return emptyList()
        if (!action.affordable || action.hasXCost || action.requiresTargets ||
            action.targetRequirements.orEmpty().isNotEmpty() ||
            action.hasConvoke || action.hasDelve || action.hasTapForGeneric || action.hasHarmonize ||
            action.requiresManaColorChoice || info.costType != "Blight" || info.blightAmount <= 0 ||
            info.validBlightTargets.size > MAX_BLIGHT_CANDIDATES
        ) return emptyList()
        return info.validBlightTargets.sortedBy { it.value }.filter { target ->
            simulator.accepts(state, activation.copy(costPayment =
                (activation.costPayment ?: AdditionalCostPayment()).copy(blightTargets = listOf(target))))
        }
    }

    private fun convokeCallable(action: LegalAction): Boolean = !action.hasConvoke ||
        (action.actionType == "CastSpell" && !action.hasXCost &&
            (action.canPayWithoutConvoke || action.policyConvokePaymentOptions.isNotEmpty()))

    /**
     * Search only ordinary, fixed-cost, untargeted casts. Each candidate is checked by the real
     * action processor, so a mana creature cannot both convoke and tap for mana, and unusual
     * payment restrictions cannot leak an unpayable action into the policy mask. The limits are
     * intentional: beyond them the cast stays masked until it has a richer payment head.
     */
    private fun convokeOptions(
        action: LegalAction, state: GameState, simulator: GameSimulator,
    ): List<Map<EntityId, ConvokePayment>> {
        val cast = action.action as? CastSpell ?: return emptyList()
        if (!action.affordable || action.actionType != "CastSpell" || action.hasXCost ||
            action.additionalCostInfo != null || action.hasDelve || action.hasTapForGeneric ||
            action.hasHarmonize || action.manaCostPerExtraTarget != null ||
            (action.requiresTargets && action.minTargets > 0) ||
            action.targetRequirements.orEmpty().any { it.minTargets > 0 }
        ) return emptyList()
        val cost = action.manaCostString?.let { runCatching { ManaCost.parse(it) }.getOrNull() }
            ?: return emptyList()
        val creatures = action.convokeCreatures.orEmpty()
            .sortedWith(compareByDescending<com.wingedsheep.engine.legalactions.ConvokeCreatureData> {
                it.colors.count { color -> cost.symbols.any { symbol -> paysColored(symbol, color) } }
            }.thenBy { it.entityId.value })
            .take(MAX_CREATURES)
        if (creatures.isEmpty()) return emptyList()

        data class Candidate(
            val next: Int,
            val remaining: ManaCost,
            val payments: Map<EntityId, ConvokePayment>,
        )
        val queue = ArrayDeque<Candidate>()
        queue.add(Candidate(0, cost, emptyMap()))
        val options = mutableListOf<Map<EntityId, ConvokePayment>>()
        var checked = 0
        while (queue.isNotEmpty() && checked < MAX_PREFLIGHTS && options.size < MAX_OPTIONS) {
            val current = queue.removeFirst()
            for (index in current.next until creatures.size) {
                val creature = creatures[index]
                val choices = buildList<Color?> {
                    creature.colors.sortedBy { it.ordinal }.forEach { color ->
                        if (current.remaining.symbols.any { paysColored(it, color) }) add(color)
                    }
                    if (current.remaining.genericAmount > 0) add(null)
                }
                for (color in choices) {
                    val reduced = if (color == null) current.remaining.reduceGeneric(1)
                        else reduceColored(current.remaining, color)
                    if (reduced == current.remaining) continue
                    val payment = current.payments + (creature.entityId to ConvokePayment(color))
                    checked++
                    val completed = cast.copy(alternativePayment =
                        (cast.alternativePayment ?: AlternativePaymentChoice.NONE).copy(
                            convokedCreatures = payment))
                    if (simulator.accepts(state, completed)) options.add(payment)
                    if (payment.size < MAX_CREATURES && checked < MAX_PREFLIGHTS) {
                        queue.add(Candidate(index + 1, reduced, payment))
                    }
                    if (checked >= MAX_PREFLIGHTS || options.size >= MAX_OPTIONS) break
                }
                if (checked >= MAX_PREFLIGHTS || options.size >= MAX_OPTIONS) break
            }
        }
        return options
    }

    private fun paysColored(symbol: ManaSymbol, color: Color): Boolean = when (symbol) {
        is ManaSymbol.Colored -> symbol.color == color
        is ManaSymbol.Hybrid -> symbol.color1 == color || symbol.color2 == color
        is ManaSymbol.MonocolorHybrid -> symbol.color == color
        else -> false
    }

    private fun reduceColored(cost: ManaCost, color: Color): ManaCost {
        val symbols = cost.symbols.toMutableList()
        val index = symbols.indexOfFirst { it == ManaSymbol.Colored(color) }
            .takeIf { it >= 0 } ?: symbols.indexOfFirst { paysColored(it, color) }
        if (index >= 0) symbols.removeAt(index)
        return ManaCost(symbols)
    }

    private const val MAX_CREATURES = 7
    private const val MAX_PREFLIGHTS = 128
    private const val MAX_OPTIONS = 4
    private const val MAX_BLIGHT_CANDIDATES = 8
    private const val MAX_TAP_CANDIDATES = 8
    private const val MAX_TAP_COUNT = 3
    private const val MAX_BEHOLD_CANDIDATES = 6
    private const val MAX_BEHOLD_TARGETS = 4
    private const val MAX_BEHOLD_COUNT = 3
    private const val MAX_PRICED_TARGETS = 16

    private fun additionalCostCallable(action: LegalAction): Boolean {
        val info = action.additionalCostInfo ?: return true
        if (action.action is CastSpell) {
            return info.costType == "Behold" && action.policyBeholdPaymentOptions.isNotEmpty()
        }
        // SacrificeSelf needs no parameter only when the source is its sole payment; Blight
        // needs an engine-checked recipient before the policy may submit it.
        val activation = action.action as? ActivateAbility ?: return false
        return when (info.costType) {
            "Blight" -> action.policyBlightTargetOptions.isNotEmpty()
            "TapPermanents" -> action.policyTapPaymentOptions.isNotEmpty()
            "SacrificeSelf" -> info.sacrificeCount == 1 &&
                info.validSacrificeTargets == listOf(activation.sourceId) &&
                info.counterRemovalCreatures.isEmpty()
            else -> false
        }
    }
}
