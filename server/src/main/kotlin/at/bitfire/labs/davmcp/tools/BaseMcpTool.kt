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
import java.util.logging.Logger

abstract class BaseMcpTool : McpTool {

    protected val logger: Logger
        get() = Logger.getLogger(javaClass.name)

    /**
     * Gets the CalDAV service for the given user.
     *
     * @param database database instance used to query the service
     * @param user the user whose service should be retrieved
     * @return the [Service] associated with the user
     */
    protected fun getCalDavService(database: Database, user: User): Service =
        database.serviceQueries.getByUserId(user.id).executeAsOne()

    /**
     * Logs a tool call with the given input data.
     *
     * @param toolName the name of the tool being called
     * @param user the authenticated user calling the tool
     * @param input the input data to log (can be null for tools with no input)
     */
    protected fun logToolCall(toolName: String, user: User, input: Any?) {
        logger.info("MCP tool $toolName called by ${user.email}: ${input ?: "(no input)"}")
    }

    /**
     * Resolves the calendar collection to use for an operation.
     *
     * Resolution order:
     * 1. If [specificId] is provided, that specific collection is used.
     * 2. If the user has a [User.defaultCalendarId] configured, that collection is used.
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
        // Determine which collection ID to use
        val collectionId: Long =
            // specific ID
            specificId
            // or default ID, if defined
                ?: database.userQueries.getDefaultCalendarId(service.userId).executeAsOneOrNull()?.defaultCalendarId
                ?: throw IllegalStateException("No default calendar defined. Ask the user to set a default calendar and then try again.")

        // Always verify the collection belongs to the service (which belongs to the authenticated user)
        return database.collectionQueries.getByServiceAndId(service.id, collectionId).executeAsOneOrNull()
            ?: throw IllegalArgumentException("Collection with id=$collectionId not found")
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