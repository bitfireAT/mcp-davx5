package at.bitfire.labs.davmcp.tools

import at.bitfire.dav4jvm.ktor.DavResource
import at.bitfire.labs.davmcp.DavConfig
import at.bitfire.labs.davmcp.HttpClientBuilder
import at.bitfire.labs.davmcp.icalendar.SimpleEventConverter
import io.ktor.http.*
import io.modelcontextprotocol.kotlin.sdk.server.ClientConnection
import io.modelcontextprotocol.kotlin.sdk.types.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.put
import java.util.logging.Logger
import javax.inject.Inject

class DeleteEventTool @Inject constructor(
    private val config: DavConfig,
    private val httpClientBuilder: HttpClientBuilder,
    private val simpleConverter: SimpleEventConverter
) : BaseMcpTool() {

    private val logger
        get() = Logger.getLogger(javaClass.name)

    override fun tool() = Tool(
        name = "events.delete",
        description = "Deletes an event from the user's calendar.",
        inputSchema = ToolSchema(
            properties = buildJsonObject {
                put("fileName", buildJsonObject {
                    put("type", "string")
                    put(
                        "description",
                        "File name of the event to be deleted, as returned by the `events.queryByTime` tool."
                    )
                })
            },
            required = listOf("fileName")
        ),
        annotations = ToolAnnotations(
            readOnlyHint = false,
            destructiveHint = true,
            idempotentHint = false
        )
    )

    override suspend fun handle(connection: ClientConnection, request: CallToolRequest): CallToolResult {
        val input = McpJson.decodeFromJsonElement<InputData>(
            request.arguments ?: throw IllegalArgumentException("Request arguments are required")
        )
        logger.info("QueryByTimeTool: $input")

        httpClientBuilder.buildFromConfig().use { client ->
            val collectionUrl = Url(config.calendarUrl)
            val url = URLBuilder(collectionUrl).appendPathSegments(input.fileName).build()
            logger.info("Deleting event $url")

            val dav = DavResource(client, url)
            dav.delete {
                // success
            }
        }

        return CallToolResult(content = listOf(TextContent("Success")))
    }


    @Serializable
    private data class InputData(
        val fileName: String
    )

}