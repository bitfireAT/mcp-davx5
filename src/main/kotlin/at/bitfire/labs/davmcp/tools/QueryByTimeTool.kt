package at.bitfire.labs.davmcp.tools

import at.bitfire.dav4jvm.ktor.DavCalendar
import at.bitfire.dav4jvm.ktor.Response
import at.bitfire.dav4jvm.property.caldav.CalDAV
import at.bitfire.dav4jvm.property.caldav.CalendarData
import at.bitfire.labs.davmcp.DavConfig
import at.bitfire.labs.davmcp.HttpClientBuilder
import at.bitfire.labs.davmcp.icalendar.SimpleEvent
import at.bitfire.labs.davmcp.icalendar.SimpleEventConverter
import at.bitfire.labs.davmcp.icalendar.simpleEventSchema
import at.bitfire.labs.davmcp.json.McpJson
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
    private val httpClientBuilder: HttpClientBuilder,
    private val simpleEventConverter: SimpleEventConverter
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
                        "Optional start date-time (RFC 3339 format). Only events with recurrences on or after this timestamp will be returned."
                    )
                })
                put("end", buildJsonObject {
                    put("type", "string")
                    put("format", "date-time")
                    put(
                        "description",
                        "Optional end date-time (RFC 3339 format). Only events with recurrences before this timestamp will be returned."
                    )
                })
            },
            required = listOf()
        ),
        outputSchema = ToolSchema(
            properties = buildJsonObject {
                put("events", buildJsonObject {
                    put("type", "array")
                    put("items", buildJsonObject {
                        simpleEventSchema()
                    })
                })
            },
            required = listOf("events")
        )
    )

    override suspend fun handler(connection: ClientConnection, request: CallToolRequest): CallToolResult {
        val queryRequest = McpJson.decodeFromJsonElement<QueryByTimeRequest>(
            request.arguments ?: throw IllegalArgumentException("Request arguments are required")
        )
        logger.info("QueryByTime: $queryRequest")

        httpClientBuilder.buildFromConfig().use { client ->
            val url = Url(config.calendarUrl)
            val calendar = DavCalendar(client, url)

            val start: Instant? = queryRequest.start?.let { Instant.parse(it) }
            val end: Instant? = queryRequest.end?.let { Instant.parse(it) }

            val events = mutableListOf<SimpleEvent>()
            calendar.calendarQuery(Component.VEVENT, start, end, setOf(CalDAV.CalendarData)) { response, relation ->
                if (relation != Response.HrefRelation.MEMBER)
                    return@calendarQuery

                val calendarData = response[CalendarData::class.java]?.iCalendar ?: return@calendarQuery
                val event = simpleEventConverter.convert(response.hrefName(), calendarData)
                if (event != null)
                    events += event
            }
            return CallToolResult(
                content = listOf(TextContent(McpJson.encodeToString(events))),
                isError = false,
                structuredContent = McpJson.encodeToJsonElement(Result(events)).jsonObject
            ).also { logger.info("Result: $it") }
        }

        //return CallToolResult.error("Unknown error")
    }


    @Serializable
    data class QueryByTimeRequest(
        val start: String?,
        val end: String?
    )

    @Serializable
    data class Result(
        val events: List<SimpleEvent>
    ) {

    }

}
