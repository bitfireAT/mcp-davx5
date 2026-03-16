package at.bitfire.labs.davmcp.tools

import io.modelcontextprotocol.kotlin.sdk.server.ClientConnection
import io.modelcontextprotocol.kotlin.sdk.types.*
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

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
            },
            required = listOf("title")
        )
    )
    
    fun handler(connection: ClientConnection, request: CallToolRequest): CallToolResult {
        try {
            val eventData = EventData(
                summary = request.arguments?.get("title")?.toString() ?: throw IllegalArgumentException("Title is required")
            )
            return CallToolResult(content = listOf(TextContent("Success")))
        } catch (e: Exception) {
            return CallToolResult(
                content = listOf(TextContent(e.message ?: e.javaClass.name)),
                isError = true
            )
        }
    }

    private class EventData(
        val summary: String
    )

}