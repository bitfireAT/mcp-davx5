package at.bitfire.labs.davmcp.tools

import at.bitfire.labs.davmcp.db.Database
import at.bitfire.labs.davmcp.db.User
import at.bitfire.labs.davmcp.json.McpJson
import io.modelcontextprotocol.kotlin.sdk.server.ClientConnection
import io.modelcontextprotocol.kotlin.sdk.types.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import javax.inject.Inject

class ListCollectionsTool @Inject constructor(
    private val database: Database
) : BaseMcpTool() {

    override fun tool() = Tool(
        name = "collections.list",
        description = "Lists all calendar collections (including calendar ID field). " +
                "Use the collection ID that is returned by this tool to target a specific calendar in other event tools.",
        inputSchema = ToolSchema(
            properties = buildJsonObject {},
            required = listOf()
        ),
        outputSchema = ToolSchema(
            properties = buildJsonObject {
                put("collections", buildJsonObject {
                    put("type", "array")
                    put("items", buildJsonObject {
                        put("type", "object")
                        put("properties", buildJsonObject {
                            put("id", buildJsonObject {
                                put("type", "number")
                                put("description", "Collection ID to use in event tools.")
                            })
                            put("displayName", buildJsonObject {
                                put("type", "string")
                                put("description", "Human-readable name of the calendar.")
                            })
                            put("url", buildJsonObject {
                                put("type", "string")
                                put("description", "CalDAV URL of the collection.")
                            })
                            put("isDefault", buildJsonObject {
                                put("type", "boolean")
                                put("description", "Whether this is the user's default calendar.")
                            })
                        })
                        put("required", buildJsonArray {
                            add("id"); add("url"); add("isDefault")
                        })
                    })
                })
            },
            required = listOf("collections")
        ),
        annotations = ToolAnnotations(
            readOnlyHint = true,
            destructiveHint = false
        )
    )

    override suspend fun handle(connection: ClientConnection, user: User, request: CallToolRequest): CallToolResult {
        val service = database.serviceQueries.getByUserId(user.id).executeAsOne()
        val collections = database.collectionQueries.getByService(service.id).executeAsList()

        val result = collections.map { col ->
            CollectionInfo(
                id = col.id,
                displayName = col.displayName,
                url = col.url,
                isDefault = col.id == service.defaultCollectionId
            )
        }

        return CallToolResult(
            content = listOf(TextContent(McpJson.encodeToString(result))),
            isError = false,
            structuredContent = McpJson.encodeToJsonElement(OutputData(result)).jsonObject
        )
    }


    @Serializable
    data class CollectionInfo(
        val id: Long,
        val displayName: String?,
        val url: String,
        val isDefault: Boolean
    )

    @Serializable
    data class OutputData(
        val collections: List<CollectionInfo>
    )

}
