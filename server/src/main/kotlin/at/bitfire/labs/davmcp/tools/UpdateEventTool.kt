package at.bitfire.labs.davmcp.tools

import at.bitfire.dav4jvm.ktor.DavResource
import at.bitfire.labs.davmcp.HttpClientBuilder
import at.bitfire.labs.davmcp.db.Database
import at.bitfire.labs.davmcp.db.User
import at.bitfire.labs.davmcp.icalendar.SimpleEvent
import at.bitfire.labs.davmcp.icalendar.SimpleEventConverter
import at.bitfire.labs.davmcp.icalendar.iCalendarContentType
import at.bitfire.labs.davmcp.icalendar.simpleEventSchema
import collectionIdSchema
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.modelcontextprotocol.kotlin.sdk.server.ClientConnection
import io.modelcontextprotocol.kotlin.sdk.types.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import javax.inject.Inject

class UpdateEventTool @Inject constructor(
    private val database: Database,
    private val httpClientBuilder: HttpClientBuilder,
    private val simpleConverter: SimpleEventConverter
) : BaseMcpTool() {

    override fun tool() = Tool(
        name = "events.update",
        description = "Updates an existing event in the user's calendar. " +
                "If you already know the file name of the vent, you don't need to query/fetch the event first. " +
                "Only the file name and field names to update/remove are known.",
        inputSchema = ToolSchema(
            properties = buildJsonObject {
                collectionIdSchema()
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
        outputSchema = ToolSchema(
            properties = buildJsonObject {
                put("fileName", buildJsonObject {
                    put("type", "string")
                    put("description", "File name of the updated event")
                })
                put("iCalendar", buildJsonObject {
                    put("type", "string")
                    put("description", "Updated iCalendar data")
                })
                put("success", buildJsonObject {
                    put("type", "boolean")
                    put("description", "Whether the operation was successful")
                })
                put("message", buildJsonObject {
                    put("type", "string")
                    put("description", "Additional information about the result")
                })
            },
            required = listOf("fileName", "iCalendar", "success")
        ),
        annotations = ToolAnnotations(
            readOnlyHint = false,
            destructiveHint = true,
            idempotentHint = false
        )
    )

    override suspend fun handle(connection: ClientConnection, user: User, request: CallToolRequest): CallToolResult {
        val input = McpJson.decodeFromJsonElement<InputData>(
            request.arguments ?: throw IllegalArgumentException("Request arguments are required")
        )
        logToolCall("UpdateEventTool", user, input)

        val service = getCalDavService(database, user)
        val collection = resolveCollection(database, service, input.collectionId)
        val collectionUrl = Url(collection.url)
        val eventUrl = URLBuilder(collectionUrl).appendPathSegments(input.fileName).build()

        httpClientBuilder.buildFromService(service).use { client ->
            val davResource = DavResource(client, eventUrl)

            // fetch existing event
            val originalICalendar = client.get(eventUrl) {
                expectSuccess = true
            }.bodyAsText()

            // generate ICalendar with the updated fields, using the existing ICalendar as base
            val updatedEvent = simpleConverter.toICalendar(
                eventData = input.eventDataToUpdate,
                originalICalendar = originalICalendar,
                removeFieldsFromOriginal = input.eventFieldsToRemove
            )

            // upload updated event
            davResource.put(io.ktor.http.content.TextContent(updatedEvent, iCalendarContentType)) { _ ->
                // success
            }

            val outputData = OutputData(
                fileName = input.fileName,
                iCalendar = updatedEvent,
                success = true,
                message = "Event successfully updated"
            )

            return CallToolResult(
                content = listOf(
                    TextContent(McpJson.encodeToString(outputData))
                ),
                structuredContent = McpJson.encodeToJsonElement(outputData).jsonObject
            )
        }
    }


    @Serializable
    private data class InputData(
        val collectionId: Long? = null,
        val fileName: String,
        val eventDataToUpdate: SimpleEvent,
        val eventFieldsToRemove: List<String>
    )

    @Serializable
    private data class OutputData(
        val fileName: String,
        val iCalendar: String,
        val success: Boolean = true,
        val message: String? = null
    )

}