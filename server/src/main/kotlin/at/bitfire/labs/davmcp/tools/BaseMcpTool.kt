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
     * Resolution order:
     * 1. If [specificId] is provided, that specific collection is used.
     * 2. If the service has a [Service.defaultCollectionId] configured, that collection is used.
     * 3. Otherwise, silently falls back to the first collection found for the service,
     *    assuming it is the only one.
     *
     * @param database database instance used to query collections and services
     * @param service the service whose collections are searched
     * @param specificId optional explicit collection ID requested by the caller
     * @return the resolved [Collection]
     * @throws IllegalArgumentException if [specificId] is given but no matching collection exists
     * @throws IllegalStateException if no collection exists for the service at all
     */
    protected fun resolveCollection(database: Database, service: Service, specificId: Long?): Collection {
        // specific collection requested
        if (specificId != null)
            return database.collectionQueries.getById(specificId).executeAsOneOrNull()
                ?: throw IllegalArgumentException("Collection with id=$specificId not found")

        // find explicitly defined default collection
        database.serviceQueries.getDefaultCollection(service.id).executeAsOneOrNull()?.let { return it }

        // fall back to first collection
        return database.collectionQueries.getByService(service.id).executeAsOneOrNull()
            ?: throw IllegalStateException("No calendar collection found for service id=${service.id}.")
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