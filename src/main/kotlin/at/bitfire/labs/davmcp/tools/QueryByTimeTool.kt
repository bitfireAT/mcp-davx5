package at.bitfire.labs.davmcp.tools

import at.bitfire.labs.davmcp.DavConfig
import io.modelcontextprotocol.kotlin.sdk.server.ClientConnection
import io.modelcontextprotocol.kotlin.sdk.types.*
import kotlinx.serialization.json.buildJsonObject
import javax.inject.Inject

class QueryByTimeTool @Inject constructor(
    private val config: DavConfig
) : McpTool {
    override fun tool() = Tool(
        name = "queryByTime",
        description = "Query events by time range",
        inputSchema = ToolSchema(
            properties = buildJsonObject {},
            required = listOf()
        )
    )

    override suspend fun handler(connection: ClientConnection, request: CallToolRequest): CallToolResult {
        // Stub implementation
        return CallToolResult(content = listOf(TextContent("Query by time stub implementation")))
    }
}
