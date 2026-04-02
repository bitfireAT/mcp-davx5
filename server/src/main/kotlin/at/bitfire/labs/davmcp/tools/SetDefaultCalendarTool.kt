package at.bitfire.labs.davmcp.tools

import at.bitfire.labs.davmcp.db.Database
import at.bitfire.labs.davmcp.db.User
import at.bitfire.labs.davmcp.json.McpJson
import io.modelcontextprotocol.kotlin.sdk.server.ClientConnection
import io.modelcontextprotocol.kotlin.sdk.types.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.put
import javax.inject.Inject

class SetDefaultCalendarTool @Inject constructor(
    private val database: Database
) : BaseMcpTool() {

    override fun tool() = Tool(
        name = "events.setDefaultCalendar",
        description = "Sets the default calendar collection that will be used when no specific collection is specified in event operations.",
        inputSchema = ToolSchema(
            properties = buildJsonObject {
                put("collectionId", buildJsonObject {
                    put("type", "number")
                    put("description", "The ID of the collection to set as default.")
                })
            },
            required = listOf("collectionId")
        ),
        annotations = ToolAnnotations(
            readOnlyHint = false,
            destructiveHint = false,
            idempotentHint = true
        )
    )

    override suspend fun handle(connection: ClientConnection, user: User, request: CallToolRequest): CallToolResult {
        val input = McpJson.decodeFromJsonElement<InputData>(
            request.arguments ?: throw IllegalArgumentException("Request arguments are required")
        )
        logToolCall("SetDefaultCalendarTool", user, input)

        // Verify the collection exists and belongs to the service that belongs to the authenticated user
        val service = getCalDavService(database, user)
        val collection =
            database.collectionQueries.getByServiceAndId(service.id, input.collectionId).executeAsOneOrNull()
            ?: throw IllegalArgumentException("Collection with id=${input.collectionId} not found")

        // Update the default collection
        database.serviceQueries.setDefaultCollection(collection.id, service.id)

        return CallToolResult(
            content = listOf(
                TextContent(
                    "Success. Default calendar set to collection ID ${collection.id} (\"${collection.displayName}\"). " +
                            "This collection will now be used when no specific collection is specified in event operations."
                )
            )
        )
    }

    @Serializable
    private data class InputData(
        val collectionId: Long
    )

}