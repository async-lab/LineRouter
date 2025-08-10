package club.asynclab.linerouter;

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.inject.Inject
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.connection.PreLoginEvent
import com.velocitypowered.api.event.player.GameProfileRequestEvent
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent
import com.velocitypowered.api.event.proxy.ProxyReloadEvent
import com.velocitypowered.api.plugin.Plugin
import com.velocitypowered.api.plugin.annotation.DataDirectory
import com.velocitypowered.api.proxy.ProxyServer
import com.velocitypowered.api.util.UuidUtils
import org.slf4j.Logger
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.Path
import java.util.concurrent.TimeUnit
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlin.uuid.toKotlinUuid


@OptIn(ExperimentalUuidApi::class)
@Plugin(
    id = "linerouter", name = "LineRouter", version = BuildConstants.VERSION
)
class LineRouter @Inject constructor(
    val logger: Logger,
    val server: ProxyServer,
    @DataDirectory val dataDirectory: Path,
) {
    private val client: HttpClient = HttpClient.newHttpClient()
    private val cache: Cache<String, Uuid> = Caffeine.newBuilder()
        .maximumSize(1000)
        .expireAfterWrite(30, TimeUnit.MINUTES)
        .build()

    fun init() {
        this.cache.invalidateAll()
    }

    @Subscribe
    fun onProxyInitialization(event: ProxyInitializeEvent) {
        this.init()
        this.logger.info("Loaded!")
    }

    @Subscribe
    fun onReload(event: ProxyReloadEvent) {
        this.init()
        this.logger.info("Reloaded!")
    }

    @Subscribe(priority = Short.MAX_VALUE)
    fun onPreLogin(event: PreLoginEvent) {
        event.result = when (val uuid = fallbackRequest(event.username)) {
            null -> PreLoginEvent.PreLoginComponentResult.allowed()
            else -> {
                if (uuid == event.uniqueId?.toKotlinUuid()) PreLoginEvent.PreLoginComponentResult.forceOnlineMode()
                else PreLoginEvent.PreLoginComponentResult.forceOfflineMode()
            }
        }
    }

    @Subscribe(priority = Short.MIN_VALUE)
    fun onGameProfileRequest(event: GameProfileRequestEvent) {
        if (this.server.configuration.isOnlineMode) return
        val offlineUuid = UuidUtils.generateOfflinePlayerUuid(event.username)
        if (event.originalProfile.id == offlineUuid) return
        event.gameProfile = event.originalProfile.withId(offlineUuid)
    }

    fun fallbackRequest(username: String): Uuid? {
        return try {
            this.cache.get(username) {
                Utils.endpoints.asSequence()
                    .mapNotNull { endpoint -> fetchUuidFromApi(endpoint, username) }
                    .firstOrNull() ?: throw Exception("Failed to fetch UUID for $username")
            }
        } catch (e: Exception) {
            logger.error(e.message)
            null
        }
    }

    private fun fetchUuidFromApi(endpoint: Utils.Endpoint, username: String): Uuid? {
        val response = try {
            this.client.send(
                HttpRequest.newBuilder().uri(URI.create(endpoint.combine(username))).build(),
                HttpResponse.BodyHandlers.ofString()
            )
        } catch (e: Exception) {
            this.logger.error("Error while sending request to ${endpoint.baseUrl}: ${e.message}")
            return null
        }
        return when (response.statusCode()) {
            StatusCode.OK.code -> ((JsonParser.parseString(response.body()) as? JsonObject)
                ?.get(endpoint.uuidField)?.asString)?.let { Utils.parseUuidString(it) }

            StatusCode.NOT_FOUND.code -> Uuid.NIL

            else -> null
        }
    }
}
