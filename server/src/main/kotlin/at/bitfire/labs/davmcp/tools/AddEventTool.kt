package at.bitfire.labs.davmcp.tools

import at.bitfire.dav4jvm.ktor.DavCalendar
import at.bitfire.labs.davmcp.HttpClientBuilder
import at.bitfire.labs.davmcp.db.Database
import at.bitfire.labs.davmcp.db.User
import at.bitfire.labs.davmcp.icalendar.SimpleEvent
import at.bitfire.labs.davmcp.icalendar.SimpleEventConverter
import at.bitfire.labs.davmcp.icalendar.iCalendarContentType
import at.bitfire.labs.davmcp.icalendar.simpleEventSchema
import collectionIdSchema
import io.ktor.http.*
import io.modelcontextprotocol.kotlin.sdk.server.ClientConnection
import io.modelcontextprotocol.kotlin.sdk.types.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import java.util.*
import java.util.logging.Level
import javax.inject.Inject
import io.ktor.http.content.TextContent as KtorTextContent

class AddEventTool @Inject constructor(
    private val database: Database,
    private val httpClientBuilder: HttpClientBuilder,
    private val simpleConverter: SimpleEventConverter
) : BaseMcpTool() {

    override fun tool() = Tool(
        name = "events.add",
        description = "Adds a new event to the user's calendar.",
        inputSchema = ToolSchema(
            properties = buildJsonObject {
                collectionIdSchema()
                put("eventData", buildJsonObject {
                    simpleEventSchema()
                })
            },
            required = listOf("eventData")
        ),
        outputSchema = ToolSchema(
            properties = buildJsonObject {
                put("fileName", buildJsonObject {
                    put("type", "string")
                    put("description", "File name of the created event")
                })
                put("iCalendar", buildJsonObject {
                    put("type", "string")
                    put("description", "Generated iCalendar data")
                })
                put("success", buildJsonObject {
                    put("type", "boolean")
                    put("description", "Whether the operation was successful")
                })
                put("message", buildJsonObject {
                    put("type", "string")
                    put("description", "Additional information about the result")
                })
            },
            required = listOf("fileName", "iCalendar", "success")
        ),
        annotations = ToolAnnotations(
            readOnlyHint = false,
            destructiveHint = true,
            idempotentHint = false
        )
    )

    override suspend fun handle(connection: ClientConnection, user: User, request: CallToolRequest): CallToolResult {
        val input = McpJson.decodeFromJsonElement<InputData>(
            request.arguments ?: throw IllegalArgumentException("Request arguments are required")
        )
        logToolCall("AddEventTool", user, input)

        val event = input.eventData
        val uid = UUID.randomUUID()
        val fileName = "$uid.ics"
        val iCalendar = simpleConverter.toICalendar(event)

        val service = getCalDavService(database, user)
        val collection = resolveCollection(database, service, input.collectionId)
        val collectionUrl = Url(collection.url)

        httpClientBuilder.buildFromService(service).use { client ->
            val url = URLBuilder(collectionUrl).appendPathSegments(fileName).build()
            logger.log(Level.INFO, "Uploading iCalendar to $url", iCalendar)

            val calendar = DavCalendar(client, url)
            val content = KtorTextContent(
                text = iCalendar,
                contentType = iCalendarContentType
            )

            var newFileName: String = fileName
            calendar.put(content) { response ->
                // success
                val newLocation = response.headers[HttpHeaders.ContentLocation]
                if (newLocation != null)
                    newFileName = Url(newLocation).segments.last()
            }

            val outputData = OutputData(
                fileName = newFileName,
                iCalendar = iCalendar,
                success = true,
                message = "File name of created event: $newFileName. " +
                        "This file name can directly be used to edit or delete the event again, without need to query/fetch it first."
            )

            return CallToolResult(
                content = listOf(
                    TextContent(McpJson.encodeToString(outputData))
                ),
                structuredContent = McpJson.encodeToJsonElement(outputData).jsonObject
            )
        }
    }


    @Serializable
    private data class InputData(
        val collectionId: Long? = null,
        val eventData: SimpleEvent
    )

    @Serializable
    private data class OutputData(
        val fileName: String,
        val iCalendar: String,
        val success: Boolean = true,
        val message: String? = null
    )

}