package at.bitfire.labs.davmcp

import at.bitfire.labs.davmcp.tools.AddEventTool
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.server.plugins.contentnegotiation.*
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.server.ServerOptions
import io.modelcontextprotocol.kotlin.sdk.server.mcpStreamableHttp
import io.modelcontextprotocol.kotlin.sdk.types.Implementation
import io.modelcontextprotocol.kotlin.sdk.types.McpJson
import io.modelcontextprotocol.kotlin.sdk.types.ServerCapabilities

fun main(args: Array<String>) {
    val port = args.firstOrNull()?.toIntOrNull() ?: 3000
    val config = DavConfig.fromEnvironment()
    val mcpServer = Server(
        serverInfo = Implementation(
            name = "3dav-mcp-server",
            version = "0.0.1"
        ),
        options = ServerOptions(
            capabilities = ServerCapabilities(
                tools = ServerCapabilities.Tools(listChanged = true),
            ),
        )
    )

    val addEventTool = AddEventTool(config)
    mcpServer.addTool(
        addEventTool.tool(),
        addEventTool::handler
    )

    println("Running MCP server")
    embeddedServer(CIO, host = "127.0.0.1", port = port) {
        install(ContentNegotiation) {
            json(McpJson)
        }
        mcpStreamableHttp {
            mcpServer
        }
    }.start(wait = true)
}