package com.wingedsheep.sdk.model

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Supertype
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.PartnerKind

/**
 * Determines whether a card — or a pair of cards — is a legal commander designation under CR 903.3
 * (Commander) and the analogous Brawl/Standard Brawl rules.
 *
 * Default rule: a commander must be a legendary creature. The override clauses allow specific
 * non-creature cards (planeswalkers, the occasional artifact) to be commanders when their
 * oracle text says so explicitly — e.g. Daretti, Scrap Savant: "Daretti, Scrap Savant can be
 * your commander." (CR 903.3a).
 *
 * **Two commanders.** The partner abilities (CR 702.124 — partner, partner—[text], partner with
 * [name], choose a Background, Doctor's companion) let a deck designate two commanders. They are
 * modelled as [KeywordAbility.Partner] on the card; [pairingError] says whether two cards may be
 * designated together, and [identityOf] gives the pair's combined color identity (CR 702.124c).
 *
 * It lives in the SDK rather than next to the deck validator because several modules ask the same
 * question of the same pure card data: `game-server` validates a submitted commander (or pair),
 * the web client's deck builder mirrors it, and the `ai` module's commander deck builder has to
 * *pick* one. A second copy of the rule is exactly how a deck the builder produces becomes a deck the
 * validator rejects.
 */
object CommanderEligibility {

    private val CAN_BE_COMMANDER = Regex(
        """can be your commander""",
        RegexOption.IGNORE_CASE,
    )

    /** True if [card] satisfies the eligibility rule for a single (non-partnered) commander. */
    fun isLegalCommander(card: CardDefinition): Boolean {
        if (Supertype.LEGENDARY in card.typeLine.supertypes && card.typeLine.isCreature) return true
        // Explicit override clause — covers planeswalker commanders (Daretti, Freyalise, etc.)
        // and oddities like Faceless One. The clause is matched case-insensitively against the
        // card's own oracle text; we don't require it to name the card by string match because
        // some printings phrase it as "This card can be your commander."
        return CAN_BE_COMMANDER.containsMatchIn(card.oracleText)
    }

    /** The partner abilities printed on [card] (CR 702.124a); empty when it has none. */
    fun partnerKinds(card: CardDefinition): List<PartnerKind> =
        card.keywordAbilities.filterIsInstance<KeywordAbility.Partner>().map { it.kind }

    /** True if [card] has any partner ability — i.e. it *may* be one half of a commander pair. */
    fun hasPartnerAbility(card: CardDefinition): Boolean = partnerKinds(card).isNotEmpty()

    /**
     * A legendary Background enchantment card (CR 702.124k) — the second half of a
     * "choose a Background" pair, and never a commander on its own.
     */
    fun isBackground(card: CardDefinition): Boolean =
        Supertype.LEGENDARY in card.typeLine.supertypes &&
            card.typeLine.isEnchantment &&
            card.typeLine.hasSubtype(Subtype.BACKGROUND)

    /**
     * Why [first] and [second] can't be designated together as a deck's two commanders, or null
     * when they can (CR 702.124).
     *
     * A pair is legal when some partner ability on one card and some partner ability on the other
     * satisfy the *same* partner rule — different partner abilities never combine (702.124f), and
     * a card with several may use any one of them (702.124g):
     *  - partner + partner (702.124h);
     *  - partner—[text] + the same partner—[text] (702.124i) — e.g. "Friends forever";
     *  - partner with [name] naming each other (702.124j);
     *  - choose a Background + a legendary Background enchantment card (702.124k);
     *  - Doctor's companion + a legendary Time Lord Doctor creature card with no other creature
     *    types (702.124m).
     *
     * Each card must also be a legal commander in its own right (CR 903.3), except the Background
     * half of a choose-a-Background pair, which is what that ability exists to admit.
     */
    fun pairingError(first: CardDefinition, second: CardDefinition): String? {
        if (first.name == second.name) {
            return "A deck can't have two copies of ${first.name} as its commanders"
        }
        val backgroundPair = isBackgroundPair(first, second) || isBackgroundPair(second, first)
        if (backgroundPair) return null
        // A lone Background, or a Background beside anything that isn't choose-a-Background.
        listOf(first, second).firstOrNull { isBackground(it) }?.let { background ->
            val other = if (background === first) second else first
            return "${background.name} is a Background — it can only be a commander alongside a " +
                "commander with \"Choose a Background\", which ${other.name} doesn't have"
        }
        listOf(first, second).firstOrNull { !isLegalCommander(it) }?.let { card ->
            return "${card.name} is not a legal commander (must be a legendary creature or say it can be your commander)"
        }
        // The Doctor half of a Doctor's companion pair needs no partner ability of its own.
        if (isDoctorPair(first, second) || isDoctorPair(second, first)) return null
        if (!hasPartnerAbility(first) || !hasPartnerAbility(second)) {
            val missing = if (!hasPartnerAbility(first)) first else second
            return "${missing.name} has no partner ability, so it can't share command with ${
                if (missing === first) second.name else first.name
            }"
        }
        val firstKinds = partnerKinds(first)
        val secondKinds = partnerKinds(second)
        val paired = firstKinds.any { a -> secondKinds.any { b -> kindsPair(a, first, b, second) } }
        if (paired) return null
        return "${first.name} (${firstKinds.joinToString { it.description }}) and " +
            "${second.name} (${secondKinds.joinToString { it.description }}) can't be partnered — " +
            "their partner abilities don't match"
    }

    /** True when [first] and [second] form a legal two-commander pair. */
    fun canPair(first: CardDefinition, second: CardDefinition): Boolean = pairingError(first, second) == null

    /**
     * The combined color identity of a deck's commanders (CR 903.4, 702.124c) — the union of each
     * commander's identity.
     */
    fun identityOf(commanders: Collection<CardDefinition>): Set<Color> =
        commanders.flatMapTo(mutableSetOf()) { it.colorIdentity }

    private fun kindsPair(a: PartnerKind, aCard: CardDefinition, b: PartnerKind, bCard: CardDefinition): Boolean =
        when (a) {
            PartnerKind.Plain -> b == PartnerKind.Plain
            is PartnerKind.Named -> b is PartnerKind.Named && a.text.equals(b.text, ignoreCase = true)
            is PartnerKind.With -> b is PartnerKind.With && a.name == bCard.name && b.name == aCard.name
            // Handled structurally (the other half needn't have a partner ability of its own).
            PartnerKind.ChooseABackground, PartnerKind.DoctorsCompanion -> false
        }

    private fun isBackgroundPair(chooser: CardDefinition, background: CardDefinition): Boolean =
        PartnerKind.ChooseABackground in partnerKinds(chooser) &&
            isLegalCommander(chooser) &&
            isBackground(background)

    /**
     * [companion] has Doctor's companion and [doctor] is a legendary Time Lord Doctor creature
     * card with no other creature types (CR 702.124m). Both must be legendary creature cards.
     */
    private fun isDoctorPair(companion: CardDefinition, doctor: CardDefinition): Boolean {
        if (PartnerKind.DoctorsCompanion !in partnerKinds(companion)) return false
        if (!companion.isLegendaryCreature() || !doctor.isLegendaryCreature()) return false
        return isTimeLordDoctor(doctor)
    }

    private fun CardDefinition.isLegendaryCreature(): Boolean =
        Supertype.LEGENDARY in typeLine.supertypes && typeLine.isCreature

    /**
     * Exactly the creature types Time Lord and Doctor. The type-line parser splits subtypes on
     * spaces, so "Time Lord Doctor" may arrive as {Time, Lord, Doctor} as well as
     * {Time Lord, Doctor}; both spellings are accepted.
     */
    private fun isTimeLordDoctor(card: CardDefinition): Boolean {
        val types = card.typeLine.subtypes.map { it.value }.toSet()
        return types == setOf("Time Lord", "Doctor") || types == setOf("Time", "Lord", "Doctor")
    }
}
