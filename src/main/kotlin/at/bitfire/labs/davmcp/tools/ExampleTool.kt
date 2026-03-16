package at.bitfire.labs.davmcp.tools

import io.modelcontextprotocol.kotlin.sdk.server.ClientConnection
import io.modelcontextprotocol.kotlin.sdk.types.*
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class ExampleTool {

    fun tool() = Tool(
        name = "example-tool",
        description = "An example tool",
        inputSchema = ToolSchema(
            properties = buildJsonObject {
                put("input", buildJsonObject { put("type", "string") })
            }
        )
    )
    
    fun handler(connection: ClientConnection, request: CallToolRequest): CallToolResult {
        return CallToolResult(content = listOf(TextContent("Hello, world!")))
    }

}