package at.bitfire.labs.davmcp.tools

import at.bitfire.dav4jvm.ktor.DavResource
import at.bitfire.dav4jvm.ktor.Response
import at.bitfire.dav4jvm.property.caldav.CalDAV
import at.bitfire.dav4jvm.property.caldav.CalendarData
import at.bitfire.dav4jvm.property.webdav.WebDAV
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
import javax.inject.Inject

class ListAllEventsTool @Inject constructor(
    private val database: Database,
    private val httpClientBuilder: HttpClientBuilder,
    private val simpleEventConverter: SimpleEventConverter
) : BaseMcpTool() {

    override fun tool() = Tool(
        name = "events.listAll",
        description = "List all events in a calendar collection",
        inputSchema = ToolSchema(
            properties = buildJsonObject {
                collectionIdSchema()
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
        logToolCall("ListAllEventsTool", user, input)

        val service = getCalDavService(database, user)
        val collection = resolveCollection(database, service, input.collectionId)
        val collectionUrl = Url(collection.url)

        httpClientBuilder.buildFromService(service).use { client ->
            val davResource = DavResource(client, collectionUrl)

            val events = mutableListOf<EventWithName>()
            davResource.propfind(1, WebDAV.GetETag, CalDAV.CalendarData) { response, relation ->
                if (relation != Response.HrefRelation.MEMBER)
                    return@propfind

                val calendarData = response[CalendarData::class.java]?.iCalendar ?: return@propfind
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
        val collectionId: Long? = null
    )

    @Serializable
    data class EventWithName(
        val fileName: String,
        val eventData: SimpleEvent
    )

    @Serializable
    data class OutputData(
        val events: List<EventWithName>
    )

}