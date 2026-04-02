package at.bitfire.labs.davmcp.tools

import at.bitfire.dav4jvm.ktor.DavCalendar
import at.bitfire.dav4jvm.ktor.Response
import at.bitfire.dav4jvm.property.caldav.CalDAV
import at.bitfire.dav4jvm.property.caldav.CalendarData
import at.bitfire.labs.davmcp.HttpClientBuilder
import at.bitfire.labs.davmcp.db.Database
import at.bitfire.labs.davmcp.db.User
import at.bitfire.labs.davmcp.icalendar.SimpleEvent
import at.bitfire.labs.davmcp.icalendar.SimpleEventConverter
import at.bitfire.labs.davmcp.icalendar.simpleEventSchema
import at.bitfire.labs.davmcp.json.McpJson
import collectionIdSchema
import io.ktor.http.*
import io.modelcontextprotocol.kotlin.sdk.server.ClientConnection
import io.modelcontextprotocol.kotlin.sdk.types.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import net.fortuna.ical4j.model.Component
import java.time.Instant
import java.util.logging.Logger
import javax.inject.Inject

class QueryEventsByTimeTool @Inject constructor(
    private val database: Database,
    private val httpClientBuilder: HttpClientBuilder,
    private val simpleEventConverter: SimpleEventConverter
) : BaseMcpTool() {

    private val logger
        get() = Logger.getLogger(javaClass.name)

    override fun tool() = Tool(
        name = "events.queryByTime",
        description = "Query events by time range",
        inputSchema = ToolSchema(
            properties = buildJsonObject {
                collectionIdSchema()
                put("start", buildJsonObject {
                    put("type", "string")
                    put("format", "date-time")
                    put(
                        "description",
                        "Optional start date-time of the query. Only events with recurrences on or after this timestamp will be returned."
                    )
                })
                put("end", buildJsonObject {
                    put("type", "string")
                    put("format", "date-time")
                    put(
                        "description",
                        "Optional end date-time of the query. Only events with recurrences before this timestamp will be returned."
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
                        put("type", "object")
                        put("properties", buildJsonObject {
                            put("fileName", buildJsonObject {
                                put("type", "string")
                                put("description", "File name of the event (iCalendar)")
                            })
                            put("eventData", buildJsonObject {
                                simpleEventSchema()
                            })
                        })
                    })
                })
            },
            required = listOf("events")
        ),
        annotations = ToolAnnotations(
            readOnlyHint = true,
            destructiveHint = false
        )
    )

    override suspend fun handle(connection: ClientConnection, user: User, request: CallToolRequest): CallToolResult {
        val input = McpJson.decodeFromJsonElement<InputData>(
            request.arguments ?: throw IllegalArgumentException("Request arguments are required")
        )
        logger.info("QueryByTimeTool: $input")

        val service = database.serviceQueries.getByUserId(user.id).executeAsOne()
        val collection = resolveCollection(database, service, input.collectionId)
        val collectionUrl = Url(collection.url)

        httpClientBuilder.buildFromService(service).use { client ->
            val calendar = DavCalendar(client, collectionUrl)

            val start: Instant? = input.start?.let { Instant.parse(it) }
            val end: Instant? = input.end?.let { Instant.parse(it) }

            val events = mutableListOf<EventWithName>()
            calendar.calendarQuery(Component.VEVENT, start, end, setOf(CalDAV.CalendarData)) { response, relation ->
                if (relation != Response.HrefRelation.MEMBER)
                    return@calendarQuery

                val calendarData = response[CalendarData::class.java]?.iCalendar ?: return@calendarQuery
                val event = simpleEventConverter.fromICalendar(calendarData)
                if (event != null)
                    events += EventWithName(
                        fileName = response.hrefName(),
                        eventData = event
                    )
            }
            return CallToolResult(
                content = listOf(TextContent(McpJson.encodeToString(events))),
                isError = false,
                structuredContent = McpJson.encodeToJsonElement(OutputData(events)).jsonObject
            ).also { logger.info("Result: $it") }
        }
    }


    @Serializable
    data class InputData(
        val collectionId: Long? = null,
        val start: String?,
        val end: String?
    )

    @Serializable
    data class EventWithName(
        val fileName: String,
        val eventData: SimpleEvent
    )

    @Serializable
    data class OutputData(
        val events: List<EventWithName>
    ) {

    }

}
