package at.bitfire.labs.davmcp.tools

import at.bitfire.dav4jvm.ktor.DavResource
import at.bitfire.labs.davmcp.DavConfig
import at.bitfire.labs.davmcp.HttpClientBuilder
import at.bitfire.labs.davmcp.icalendar.SimpleEvent
import at.bitfire.labs.davmcp.icalendar.SimpleEventConverter
import at.bitfire.labs.davmcp.icalendar.iCalendarContentType
import at.bitfire.labs.davmcp.icalendar.simpleEventSchema
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.modelcontextprotocol.kotlin.sdk.server.ClientConnection
import io.modelcontextprotocol.kotlin.sdk.types.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.put
import java.util.logging.Logger
import javax.inject.Inject

class UpdateEventTool @Inject constructor(
    private val config: DavConfig,
    private val httpClientBuilder: HttpClientBuilder,
    private val simpleConverter: SimpleEventConverter
) : BaseMcpTool() {

    private val logger
        get() = Logger.getLogger(javaClass.name)

    override fun tool() = Tool(
        name = "events.update",
        description = "Updates an existing event in the user's calendar. " +
                "If you already know the file name of the vent, you don't need to query/fetch the event first. " +
                "Only the file name and field names to update/remove are known.",
        inputSchema = ToolSchema(
            properties = buildJsonObject {
                put("fileName", buildJsonObject {
                    put("type", "string")
                    put(
                        "description",
                        "File name of the event to be updated, as returned by the `events.queryByTime` tool."
                    )
                })
                put("eventDataToUpdate", buildJsonObject {
                    put("description", "Event fields to update (only set fields that shall be updated).")
                    simpleEventSchema(includeRequired = false, includeICalendar = false)
                })
                put("eventFieldsToRemove", buildJsonObject {
                    put("type", "array")
                    put("description", "Event fields to delete (only set fields that shall be deleted).")
                    put("items", buildJsonObject {
                        put("type", "string")
                        put(
                            "description",
                            "Name of event field to delete. See eventDataToUpdate schema for valid field names."
                        )
                    })
                })
            },
            required = listOf("fileName", "eventDataToUpdate", "eventFieldsToRemove")
        ),
        annotations = ToolAnnotations(
            readOnlyHint = false,
            destructiveHint = true,
            idempotentHint = false
        )
    )

    override suspend fun handle(connection: ClientConnection, request: CallToolRequest): CallToolResult {
        val input = McpJson.decodeFromJsonElement<InputData>(
            request.arguments ?: throw IllegalArgumentException("Request arguments are required")
        )
        logger.info("UpdateEventTool: $input")

        val collectionUrl = Url(config.calendarUrl)
        val eventUrl = URLBuilder(collectionUrl).appendPathSegments(input.fileName).build()

        var newFileName: String? = null
        httpClientBuilder.buildFromConfig().use { client ->
            val davResource = DavResource(client, eventUrl)

            // fetch existing event
            val originalICalendar = client.get(eventUrl).bodyAsText()

            // generate ICalendar with the updated fields, using the existing ICalendar as base
            val updatedEvent = simpleConverter.toICalendar(
                eventData = input.eventDataToUpdate,
                originalICalendar = originalICalendar,
                removeFieldsFromOriginal = input.eventFieldsToRemove
            )

            // upload updated event
            davResource.put(io.ktor.http.content.TextContent(updatedEvent, iCalendarContentType)) { response ->
                // success
                val newLocation = response.headers[HttpHeaders.ContentLocation]
                if (newLocation != null)
                    newFileName = Url(newLocation).segments.lastOrNull()
            }
        }

        val successMessage = if (newFileName != null)
            "Success, file name of created event: $newFileName"
        else
            "Success, file name of created event unknown."
        return CallToolResult(content = listOf(TextContent(successMessage)))
    }


    @Serializable
    private data class InputData(
        val fileName: String,
        val eventDataToUpdate: SimpleEvent,
        val eventFieldsToRemove: List<String>
    )

}