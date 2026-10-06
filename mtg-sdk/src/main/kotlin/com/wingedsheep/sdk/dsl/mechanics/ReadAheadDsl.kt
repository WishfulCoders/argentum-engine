package com.wingedsheep.sdk.dsl

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.scripting.ChoiceType
import com.wingedsheep.sdk.scripting.EntersWithChoice

/**
 * Add Read ahead (CR 702.155) to a Saga — "Choose a chapter and start with that many lore
 * counters. Add one after your draw step. Skipped chapters don't trigger."
 *
 * Composed from the existing [EntersWithChoice] `NUMBER` replacement (CR 714.3b: "as this Saga
 * enters, choose a number between one and this Saga's final chapter number"), which records the
 * choice in the permanent's `CHOSEN_NUMBER` slot before it enters, plus the [Keyword.READ_AHEAD]
 * tag the engine reads in two places:
 *  - Saga entry puts the chosen number of lore counters on it instead of the single intrinsic one
 *    (CR 714.3b replaces 714.3a);
 *  - chapter-trigger detection lets a chapter trigger the turn the Saga entered only when the lore
 *    count is exactly that chapter's number (CR 702.155a), so skipped chapters don't trigger.
 *
 * [finalChapter] is the Saga's last chapter number — III for every printed read-ahead Saga so far.
 */
fun CardBuilder.readAhead(finalChapter: Int = 3) {
    require(finalChapter >= 1) { "Read ahead needs a final chapter of at least 1" }
    keywordSet.add(Keyword.READ_AHEAD)
    replacementEffect(
        EntersWithChoice(
            choiceType = ChoiceType.NUMBER,
            minValue = 1,
            maxValue = finalChapter,
        )
    )
}
