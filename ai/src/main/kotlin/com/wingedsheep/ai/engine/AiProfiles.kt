package com.wingedsheep.ai.engine

object AiProfiles {

    /**
     * Parse a `+`-joined profile name into an [AiProfile]. One parser serves every caller, so a name
     * means the same player in the arena (`-Darena.profile` / `-Darena.targetProfile`), in a gym env's
     * pilot seat, in the replay harness and in the game-server. The tokens are documented on
     * [profileFromTokens].
     */
    fun parse(name: String): AiProfile = profileFromTokens(name)
}
