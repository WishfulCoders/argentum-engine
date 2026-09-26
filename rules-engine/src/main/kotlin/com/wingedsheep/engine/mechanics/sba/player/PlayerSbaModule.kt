package com.wingedsheep.engine.mechanics.sba.player

import com.wingedsheep.engine.handlers.effects.ZoneTransitionService
import com.wingedsheep.engine.mechanics.sba.StateBasedActionCheck
import com.wingedsheep.engine.mechanics.sba.StateBasedActionModule

class PlayerSbaModule(private val zones: ZoneTransitionService) : StateBasedActionModule {
    override fun checks(): List<StateBasedActionCheck> = listOf(
        StartYourEnginesCheck(),
        AscendCitysBlessingCheck(),
        StoriedEnduringStoryCheck(),
        PlayerLifeLossCheck(predicateEvaluator = zones.predicateEvaluator),
        EmptyLibraryDrawLossCheck(predicateEvaluator = zones.predicateEvaluator),
        CommanderDamageLossCheck(predicateEvaluator = zones.predicateEvaluator),
        PoisonLossCheck(predicateEvaluator = zones.predicateEvaluator),
        TeamLossPropagationCheck(),
        PlayerLeavesGameCheck(zones)
    )
}
