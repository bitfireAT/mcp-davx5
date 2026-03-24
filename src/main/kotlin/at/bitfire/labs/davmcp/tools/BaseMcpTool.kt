package at.bitfire.labs.davmcp.tools

import io.modelcontextprotocol.kotlin.sdk.server.ClientConnection
import io.modelcontextprotocol.kotlin.sdk.types.CallToolRequest
import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import java.io.PrintWriter
import java.io.StringWriter

open abstract class BaseMcpTool : McpTool {

    abstract suspend fun handle(connection: ClientConnection, request: CallToolRequest): CallToolResult

    override suspend fun handler(connection: ClientConnection, request: CallToolRequest): CallToolResult =
        try {
            handle(connection, request)
        } catch (e: Exception) {
            val stringWriter = StringWriter()
            val printWriter = PrintWriter(stringWriter)

            printWriter.println(e.message ?: e.javaClass.name)
            printWriter.println()
            printWriter.println("-----")
            printWriter.println()
            e.printStackTrace(printWriter)

            CallToolResult(
                content = listOf(TextContent(stringWriter.toString())),
                isError = true
            )
        }

}