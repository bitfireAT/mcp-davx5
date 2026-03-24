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
import java.util.logging.Logger
import javax.inject.Inject

class QueryByTimeTool @Inject constructor(
    private val config: DavConfig,
    private val httpClientBuilder: HttpClientBuilder
) : McpTool {

    private val logger
        get() = Logger.getLogger(javaClass.name)

    override fun tool() = Tool(
        name = "events.queryByTime",
        description = "Query events by time range",
        inputSchema = ToolSchema(
            properties = buildJsonObject {
                put("start", buildJsonObject {
                    put("type", "string")
                    put("format", "date-time")
                    put(
                        "description",
                        "Optional start date-time. Only events with recurrences on or after this timestamp will be returned."
                    )
                })
                put("end", buildJsonObject {
                    put("type", "string")
                    put("format", "date-time")
                    put(
                        "description",
                        "Optional end date-time. Only events with recurrences before this timestamp will be returned."
                    )
                })
            },
            required = listOf()
        ),
        //outputSchema = ToolSchema()
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
        logger.info("QueryByTime: $queryRequest")

        httpClientBuilder.buildFromConfig().use { client ->
            val url = Url(config.calendarUrl)
            val calendar = DavCalendar(client, url)

            val start: Instant? = queryRequest.start?.let { Instant.parse(it) }
            val end: Instant? = queryRequest.end?.let { Instant.parse(it) }

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
        val start: String?,
        val end: String?
    )

    @Serializable
    data class EventResult(
        val fileName: String?,
        val iCal: String?
    )

}
