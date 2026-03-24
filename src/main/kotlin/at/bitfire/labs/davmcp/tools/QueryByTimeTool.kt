package at.bitfire.labs.davmcp.tools

import at.bitfire.dav4jvm.ktor.DavCalendar
import at.bitfire.dav4jvm.ktor.Response
import at.bitfire.dav4jvm.property.caldav.CalendarData
import at.bitfire.labs.davmcp.DavConfig
import at.bitfire.labs.davmcp.HttpClientBuilder
import io.ktor.http.*
import io.modelcontextprotocol.kotlin.sdk.server.ClientConnection
import io.modelcontextprotocol.kotlin.sdk.types.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import net.fortuna.ical4j.model.Component
import java.time.Instant
import javax.inject.Inject

class QueryByTimeTool @Inject constructor(
    private val config: DavConfig,
    private val httpClientBuilder: HttpClientBuilder
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
                        "Optional UNIX timestamp in seconds. Only events with recurrences on or after this timestamp will be returned. Example: 1711154400 for Sat Mar 23 2024 00:40:00 GMT+0000"
                    )
                })
                put("end", buildJsonObject {
                    put("type", "integer")
                    put(
                        "description",
                        "Optional UNIX timestamp in seconds. Only events with recurrences before this timestamp will be returned."
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
        System.err.println("QueryByTime: $queryRequest")

        httpClientBuilder.buildFromConfig().use { client ->
            val url = Url(config.calendarUrl)
            val calendar = DavCalendar(client, url)

            val start: Instant? = queryRequest.start?.let { Instant.ofEpochSecond(it) }
            val end: Instant? = queryRequest.end?.let { Instant.ofEpochSecond(it) }

            val b = StringBuilder()

            val result = mutableListOf<EventResult>()
            calendar.calendarQuery(Component.VEVENT, start, end) { response, relation ->
                if (relation != Response.HrefRelation.MEMBER)
                    return@calendarQuery

                // TODO: actually query calendarData

                val calendarData = response[CalendarData::class.java]?.iCalendar
                if (calendarData != null) {
                    b.append(calendarData)
                    b.append("---")
                }

                result += EventResult(
                    fileName = response.hrefName(),
                    iCal = calendarData
                )
            }
            val json = buildJsonObject {
                put("events", json.encodeToJsonElement(result))
            }
            return CallToolResult.success(b.toString(), json)
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
        val fileName: String?,
        val iCal: String?
    )

}
