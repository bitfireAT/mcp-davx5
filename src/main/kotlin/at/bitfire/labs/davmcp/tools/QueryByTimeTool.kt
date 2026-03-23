package at.bitfire.labs.davmcp.tools

import at.bitfire.dav4jvm.ktor.DavCalendar
import at.bitfire.dav4jvm.ktor.Response
import at.bitfire.labs.davmcp.DavConfig
import io.ktor.client.*
import io.ktor.client.plugins.auth.*
import io.ktor.client.plugins.auth.providers.*
import io.ktor.client.plugins.logging.*
import io.ktor.http.*
import io.modelcontextprotocol.kotlin.sdk.server.ClientConnection
import io.modelcontextprotocol.kotlin.sdk.types.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import net.fortuna.ical4j.model.Component
import java.time.Instant
import javax.inject.Inject

class QueryByTimeTool @Inject constructor(
    private val config: DavConfig
) : McpTool {

    override fun tool() = Tool(
        name = "events.queryByTime",
        description = "Query events by time range",
        inputSchema = ToolSchema(
            properties = buildJsonObject {
                put("start", buildJsonObject {
                    put("type", "integer")
                    put(
                        "description",
                        "Only events with recurrences on or after this UNIX timestamp will be returned. Optional."
                    )
                })
                put("end", buildJsonObject {
                    put("type", "integer")
                    put(
                        "description",
                        "Only events with recurrences before this UNIX timestamp will be returned. Optional."
                    )
                })
            },
            required = listOf()
        )
    )

    override suspend fun handler(connection: ClientConnection, request: CallToolRequest): CallToolResult {
        val json = Json {
            ignoreUnknownKeys = true
            isLenient = true
            explicitNulls = false
        }
        val queryRequest = json.decodeFromJsonElement<QueryByTimeRequest>(
            request.arguments ?: throw IllegalArgumentException("Request arguments are required")
        )

        val authUsername = config.username
        val authPassword = config.password
        HttpClient {
            install(Auth) {
                basic {
                    sendWithoutRequest { true }
                    credentials {
                        BasicAuthCredentials(username = authUsername, password = authPassword)
                    }
                }
            }
            install(Logging) {
                logger = Logger.SIMPLE
            }
        }.use { client ->
            val url = Url(config.calendarUrl)
            val calendar = DavCalendar(client, url)

            val start: Instant? = queryRequest.start?.let { Instant.ofEpochSecond(it) }
            val end: Instant? = queryRequest.start?.let { Instant.ofEpochSecond(it) }

            val result = mutableListOf<EventResult>()
            calendar.calendarQuery(Component.VEVENT, start, end) { response, relation ->
                if (relation != Response.HrefRelation.MEMBER)
                    return@calendarQuery

                result += EventResult(
                    fileName = response.hrefName()
                )
            }
            val json = buildJsonObject {
                put("events", json.encodeToJsonElement(result))
            }
            return CallToolResult.success("Success", json)
        }

        //return CallToolResult.error("Unknown error")
    }


    @Serializable
    data class QueryByTimeRequest(
        val start: Long?,
        val end: Long?
    )

    @Serializable
    data class EventResult(
        val fileName: String?
    )

}
