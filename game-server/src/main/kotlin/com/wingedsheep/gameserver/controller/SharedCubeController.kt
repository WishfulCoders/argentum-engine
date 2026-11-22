package com.wingedsheep.gameserver.controller

import com.wingedsheep.gameserver.persistence.CubeRepository
import com.wingedsheep.gameserver.persistence.UserRepository
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * Public (unauthenticated) read of a shared cube: `GET /api/cubes/shared/{code}`.
 *
 * The share code is the capability — the owner mints it through [AccountCubeController.share] and
 * revoking it makes this 404. The body is the owner's *current* saved cube, not a snapshot, so a link
 * handed to friends follows the cube as it is tuned. Only the owner's display name is disclosed.
 */
@RestController
@RequestMapping("/api/cubes/shared")
@ConditionalOnProperty(name = ["accounts.enabled"], havingValue = "true")
class SharedCubeController(
    private val cubes: CubeRepository,
    private val users: UserRepository,
) {
    private val json = Json { ignoreUnknownKeys = true }

    @GetMapping("/{code}", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun get(@PathVariable code: String): ResponseEntity<Any> {
        if (!CubeShareCodes.isWellFormed(code)) return ResponseEntity.notFound().build()
        val cube = cubes.findByShareCode(code) ?: return ResponseEntity.notFound().build()
        val ownerName = users.findById(cube.userId).orElse(null)?.displayName
        val body = buildJsonObject {
            put("shareCode", code)
            put("name", cube.name)
            put("cardCount", cube.cardCount)
            put("updatedAt", cube.updatedAt.toString())
            put("ownerName", ownerName)
            put("cube", json.parseToJsonElement(cube.data))
        }
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON)
            .body(json.encodeToString(JsonObject.serializer(), body))
    }
}
