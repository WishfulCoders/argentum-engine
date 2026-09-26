package com.wingedsheep.sdk.dsl

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.scripting.effects.CreateTokenEffect
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Scryfall art for the 1/1 red Warrior token created by Mobilize (Tarkir: Dragonstorm).
 * Every Mobilize card produces this identical token, so the image is shared here rather
 * than duplicated per card.
 */
private const val MOBILIZE_WARRIOR_TOKEN_IMAGE =
    "https://cards.scryfall.io/normal/front/7/e/7edc0515-a130-45a7-aa09-0e23bba41587.jpg?1742506712"

/**
 * Add Mobilize N (Tarkir: Dragonstorm) — keyword ability + triggered ability.
 *
 * "Whenever this creature attacks, create N tapped and attacking 1/1 red Warrior
 * creature tokens. Sacrifice those tokens at the beginning of the next end step."
 *
 * The keyword ability is display-only (no separate Mobilize handler exists); the
 * behavior lives entirely in the attack-triggered ability wired here. The tokens
 * enter tapped and attacking via [CreateTokenEffect.tapped]/[CreateTokenEffect.attacking],
 * and their end-of-turn sacrifice is scheduled via [CreateTokenEffect.sacrificeAtStep]
 * (the sacrifice sibling of `exileAtStep`), which the executor turns into one delayed
 * [com.wingedsheep.sdk.scripting.effects.SacrificeTargetEffect] per created token.
 */
fun CardBuilder.mobilize(n: Int) {
    keywordAbilityList.add(KeywordAbility.mobilize(n))
    val tokenWord = if (n == 1) "token" else "tokens"
    val pronoun = if (n == 1) "it" else "those tokens"
    val article = if (n == 1) "a" else "$n"
    triggeredAbilities.add(
        TriggeredAbility.create(
            trigger = Triggers.self.attacks(),
            effect = CreateTokenEffect(
                count = DynamicAmount.Fixed(n),
                power = 1,
                toughness = 1,
                colors = setOf(Color.RED),
                creatureTypes = setOf("Warrior"),
                imageUri = MOBILIZE_WARRIOR_TOKEN_IMAGE,
                tapped = true,
                attacking = true,
                sacrificeAtStep = Step.END
            ),
            descriptionOverride = "Whenever this creature attacks, create $article tapped " +
                "and attacking 1/1 red Warrior creature $tokenWord. Sacrifice $pronoun at the " +
                "beginning of the next end step."
        )
    )
}

/**
 * Add Mobilize X (Tarkir: Dragonstorm) — keyword ability + triggered ability where
 * the token count is a [DynamicAmount] resolved on attack rather than a fixed integer.
 *
 * "Whenever this creature attacks, create X tapped and attacking 1/1 red Warrior
 * creature tokens. Sacrifice those tokens at the beginning of the next end step."
 *
 * Used by Avenger of the Fallen ("Mobilize X, where X is the number of creature cards
 * in your graveyard"). [label] is the placeholder rendered after "Mobilize" in the
 * keyword list (defaults to "X"); [amountDescription] is the natural-language phrase
 * describing the count, woven into the triggered-ability reminder text. The tokens
 * enter tapped and attacking and are sacrificed at the next end step, identical to the
 * fixed-count [mobilize] helper.
 */
fun CardBuilder.mobilize(amount: DynamicAmount, amountDescription: String, label: String = "X") {
    keywordAbilityList.add(KeywordAbility.mobilizeVariable(label))
    triggeredAbilities.add(
        TriggeredAbility.create(
            trigger = Triggers.self.attacks(),
            effect = CreateTokenEffect(
                count = amount,
                power = 1,
                toughness = 1,
                colors = setOf(Color.RED),
                creatureTypes = setOf("Warrior"),
                imageUri = MOBILIZE_WARRIOR_TOKEN_IMAGE,
                tapped = true,
                attacking = true,
                sacrificeAtStep = Step.END
            ),
            descriptionOverride = "Whenever this creature attacks, create $amountDescription " +
                "tapped and attacking 1/1 red Warrior creature tokens. Sacrifice those tokens " +
                "at the beginning of the next end step."
        )
    )
}
