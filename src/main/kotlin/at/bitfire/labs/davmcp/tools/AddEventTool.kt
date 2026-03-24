package at.bitfire.labs.davmcp.tools

import at.bitfire.dav4jvm.ktor.DavCalendar
import at.bitfire.labs.davmcp.DavConfig
import at.bitfire.labs.davmcp.HttpClientBuilder
import at.bitfire.labs.davmcp.icalendar.SimpleEvent
import at.bitfire.labs.davmcp.icalendar.SimpleEventConverter
import at.bitfire.labs.davmcp.icalendar.iCalendarContentType
import at.bitfire.labs.davmcp.icalendar.simpleEventSchema
import io.ktor.http.*
import io.modelcontextprotocol.kotlin.sdk.server.ClientConnection
import io.modelcontextprotocol.kotlin.sdk.types.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import okio.IOException
import java.util.*
import java.util.logging.Level
import java.util.logging.Logger
import javax.inject.Inject
import io.ktor.http.content.TextContent as KtorTextContent

class AddEventTool @Inject constructor(
    private val config: DavConfig,
    private val httpClientBuilder: HttpClientBuilder,
    private val simpleConverter: SimpleEventConverter
) : BaseMcpTool() {

    private val logger
        get() = Logger.getLogger(javaClass.name)

    override fun tool() = Tool(
        name = "events.add",
        description = "Adds an event to the user's calendar.",
        inputSchema = ToolSchema(
            properties = buildJsonObject {
                put("eventData", buildJsonObject {
                    simpleEventSchema()
                })
            },
            required = listOf("event")
        )
    )

    override suspend fun handle(connection: ClientConnection, request: CallToolRequest): CallToolResult {
        val input = McpJson.decodeFromJsonElement<InputData>(
            request.arguments ?: throw IllegalArgumentException("Request arguments are required")
        )
        logger.info("QueryByTimeTool: $input")

        val event = input.eventData
        val uid = UUID.randomUUID()

        val iCalendar = simpleConverter.toICalendar(event, uid = UUID.randomUUID().toString())
        uploadToCollection("$uid.ics", iCalendar)

        return CallToolResult(content = listOf(TextContent("Success")))
    }


    private suspend fun uploadToCollection(memberName: String, iCalendar: String) {
        httpClientBuilder.buildFromConfig().use { client ->
            val collectionUrl = Url(config.calendarUrl)
            val url = URLBuilder(collectionUrl).appendPathSegments(memberName).build()
            logger.log(Level.INFO, "Uploading iCalendar to $url", iCalendar)

            val calendar = DavCalendar(client, url)
            val content = KtorTextContent(
                text = iCalendar,
                contentType = iCalendarContentType
            )
            calendar.put(content) { response ->
                if (!response.status.isSuccess())
                    throw IOException("HTTP ${response.status.value} ${response.status.description}")
            }
        }
    }


    @Serializable
    private data class InputData(
        val eventData: SimpleEvent
    )

}