package com.wingedsheep.gameserver.controller

import com.wingedsheep.gameserver.matchmaking.MatchmakingService
import com.wingedsheep.gameserver.protocol.ServerMessage
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * How many players are searching in each matchmaking queue — the home screen's first paint. Changes
 * after that arrive over the socket as [ServerMessage.MatchmakingQueues].
 */
@RestController
@RequestMapping("/api/matchmaking")
class MatchmakingController(private val matchmakingService: MatchmakingService) {

    @GetMapping("/queues")
    fun queues(): List<ServerMessage.MatchmakingQueueCount> = matchmakingService.queueCounts()
}
