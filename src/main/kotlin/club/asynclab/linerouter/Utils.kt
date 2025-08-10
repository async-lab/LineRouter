package club.asynclab.linerouter

import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
object Utils {
    data class Endpoint(val baseUrl: String, val uuidField: String) {
        fun combine(username: String) = "${baseUrl}${URLEncoder.encode(username, StandardCharsets.UTF_8)}"
    }

    val endpoints = listOf(
        Endpoint("https://api.mojang.com/users/profiles/minecraft/", "id"),
        Endpoint("https://api.ashcon.app/mojang/v2/user/", "uuid")
    )

    fun parseUuidString(uuidString: String) = try {
        if (uuidString.contains("-")) Uuid.parse(uuidString)
        else uuidString.chunked(16).map { it.toULong(16) }.let { Uuid.fromULongs(it[0], it[1]) }
    } catch (e: Exception) {
        Uuid.NIL
    }
}