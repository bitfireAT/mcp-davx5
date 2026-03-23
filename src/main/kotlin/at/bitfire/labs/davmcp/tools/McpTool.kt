package at.bitfire.labs.davmcp.tools

import io.modelcontextprotocol.kotlin.sdk.server.ClientConnection
import io.modelcontextprotocol.kotlin.sdk.types.CallToolRequest
import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.types.Tool

interface McpTool {
    fun tool(): Tool
    suspend fun handler(connection: ClientConnection, request: CallToolRequest): CallToolResult
}
