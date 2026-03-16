package at.bitfire.labs.davmcp.tools

import io.modelcontextprotocol.kotlin.sdk.server.ClientConnection
import io.modelcontextprotocol.kotlin.sdk.types.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.put
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class AddEventTool {

    fun tool() = Tool(
        name = "events.add",
        description = "Adds an event to the user's calendar",
        inputSchema = ToolSchema(
            properties = buildJsonObject {
                put("title", buildJsonObject {
                    put("type", "string")
                    put("description", "Title of the event")
                })
                put("startDateTime", buildJsonObject {
                    put("type", "string")
                    put("format", "date-time")
                    put("description", "Start date date/time (inclusive) of the event (\"YYYY-MM-DDThh:mm:ss\")")
                })
                put("endDateTime", buildJsonObject {
                    put("type", "string")
                    put("format", "date-time")
                    put("description", "End date or date/time (exclusive) of the event (\"YYYY-MM-DDThh:mm:ss\")")
                })
                put("description", buildJsonObject {
                    put("type", "string")
                    put("description", "Description of the event (plain text)")
                })
            },
            required = listOf("title", "startDateTime", "endDateTime")
        )
    )
    
    fun handler(connection: ClientConnection, request: CallToolRequest): CallToolResult {
        try {
            val eventRequest = Json.decodeFromJsonElement<EventRequest>(
                request.arguments ?: throw IllegalArgumentException("Request arguments are required")
            )

            val dateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
            val eventData = EventData(
                summary = eventRequest.title,
                startDate = LocalDate.parse(eventRequest.startDateTime, dateTimeFormatter),
                endDate = LocalDate.parse(eventRequest.endDateTime, dateTimeFormatter),
                description = eventRequest.description
            )

            return CallToolResult(content = listOf(TextContent("Success: $eventData")))
        } catch (e: Exception) {
            return CallToolResult(
                content = listOf(TextContent(e.message ?: e.javaClass.name)),
                isError = true
            )
        }
    }

    @Serializable
    private data class EventRequest(
        val title: String,
        val startDateTime: String,
        val endDateTime: String,
        val description: String?
    )

    private data class EventData(
        val summary: String,
        val startDate: LocalDate,
        val endDate: LocalDate,
        val description: String?
    )

}