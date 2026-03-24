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
        description = "Updates an existing event in the user's calendar. Only set the eventData fields" +
                "that need to be updated (+ title). In order remove an eventData field, set it to an empty string. " +
                "WARNING: Potentially destructive action, only use with user's explicit consent.",
        inputSchema = ToolSchema(
            properties = buildJsonObject {
                put("fileName", buildJsonObject {
                    put("type", "string")
                    put(
                        "description",
                        "File name of the event to be deleted, as returned by the `events.queryByTime` tool."
                    )
                })
                put("eventDataToUpdate", buildJsonObject {
                    simpleEventSchema()
                })
            },
            required = listOf("fileName", "eventDataToUpdate")
        )
    )

    override suspend fun handle(connection: ClientConnection, request: CallToolRequest): CallToolResult {
        val input = McpJson.decodeFromJsonElement<InputData>(
            request.arguments ?: throw IllegalArgumentException("Request arguments are required")
        )
        logger.info("QueryByTimeTool: $input")

        val collectionUrl = Url(config.calendarUrl)
        val eventUrl = URLBuilder(collectionUrl).appendPathSegments(input.fileName).build()

        httpClientBuilder.buildFromConfig().use { client ->
            val davResource = DavResource(client, eventUrl)

            // fetch existing event
            val originalICalendar = client.get(eventUrl).bodyAsText()

            // generate ICalendar with the updated fields, using the existing ICalendar as base
            val updatedEvent = simpleConverter.toICalendar(input.eventDataToUpdate, originalICalendar)

            // upload updated event
            davResource.put(io.ktor.http.content.TextContent(updatedEvent, iCalendarContentType)) {
                // success
            }
        }

        return CallToolResult(content = listOf(TextContent("Success")))
    }


    @Serializable
    private data class InputData(
        val fileName: String,
        val eventDataToUpdate: SimpleEvent
    )

}