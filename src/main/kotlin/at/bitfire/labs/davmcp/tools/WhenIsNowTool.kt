@file:UseSerializers(InstantSerializer::class)

package at.bitfire.labs.davmcp.tools

import at.bitfire.labs.davmcp.json.InstantSerializer
import at.bitfire.labs.davmcp.json.McpJson
import io.modelcontextprotocol.kotlin.sdk.server.ClientConnection
import io.modelcontextprotocol.kotlin.sdk.types.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import java.time.Instant
import java.util.logging.Logger
import javax.inject.Inject

class WhenIsNowTool @Inject constructor() : McpTool {

    private val logger
        get() = Logger.getLogger(javaClass.name)

    override fun tool() = Tool(
        name = "timetools.now",
        description = "Use this tool to query what date or time it is now. Also useful for calculating relative times (\"in two weeks\") etc.",
        inputSchema = ToolSchema(),
        outputSchema = ToolSchema(
            properties = buildJsonObject {
                put("now", buildJsonObject {
                    put("type", "string")
                    put("format", "date-time")
                    put("description", "The current date and time (\"now\").")
                })
            },
            required = listOf("now")
        ),
        annotations = ToolAnnotations(
            readOnlyHint = true,
            destructiveHint = false
        )
    )

    override suspend fun handler(
        connection: ClientConnection,
        request: CallToolRequest
    ): CallToolResult {
        val result = Result(
            now = Instant.now()
        )
        return CallToolResult(
            content = listOf(TextContent(McpJson.encodeToString(result))),
            isError = false,
            structuredContent = McpJson.encodeToJsonElement(result).jsonObject
        ).also { logger.info("Result: $it") }
    }


    @Serializable
    data class Result(
        val now: Instant
    )

}