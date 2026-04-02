package at.bitfire.labs.davmcp.tools

import at.bitfire.labs.davmcp.db.Collection
import at.bitfire.labs.davmcp.db.Database
import at.bitfire.labs.davmcp.db.Service
import at.bitfire.labs.davmcp.db.User
import io.modelcontextprotocol.kotlin.sdk.server.ClientConnection
import io.modelcontextprotocol.kotlin.sdk.types.CallToolRequest
import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import java.io.PrintWriter
import java.io.StringWriter

abstract class BaseMcpTool : McpTool {

    /**
     * Resolves the calendar collection to use for an operation.
     *
     * If [collectionId] is provided, that specific collection is used.
     * Otherwise, falls back to the service's configured default collection.
     * Throws [IllegalStateException] if no collection can be resolved.
     */
    protected fun resolveCollection(database: Database, service: Service, collectionId: Long?): Collection {
        if (collectionId != null)
            return database.collectionQueries.getById(collectionId).executeAsOneOrNull()
                ?: throw IllegalArgumentException("Collection with id=$collectionId not found")

        return database.serviceQueries.getDefaultCollection(service.id).executeAsOneOrNull()
            ?: throw IllegalStateException(
                "No default calendar configured for service id=${service.id}. " +
                        "Use collections.list to find available calendars and set a default, " +
                        "or pass a collectionId explicitly."
            )
    }



    abstract suspend fun handle(connection: ClientConnection, user: User, request: CallToolRequest): CallToolResult

    override suspend fun handler(connection: ClientConnection, user: User, request: CallToolRequest): CallToolResult =
        try {
            handle(connection, user, request)
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