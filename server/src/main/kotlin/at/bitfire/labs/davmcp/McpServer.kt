package at.bitfire.labs.davmcp

import at.bitfire.labs.davmcp.db.User
import at.bitfire.labs.davmcp.tools.McpTool
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sse.*
import io.ktor.util.collections.*
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.server.ServerOptions
import io.modelcontextprotocol.kotlin.sdk.server.StreamableHttpServerTransport
import io.modelcontextprotocol.kotlin.sdk.types.Implementation
import io.modelcontextprotocol.kotlin.sdk.types.McpJson
import io.modelcontextprotocol.kotlin.sdk.types.ServerCapabilities
import java.util.logging.Logger
import javax.inject.Inject

class McpServer @Inject constructor(
    private val tools: Set<@JvmSuppressWildcards McpTool>,
    private val userAuthenticator: UserAuthenticator
) {

    private val logger
        get() = Logger.getLogger(javaClass.name)

    fun start(port: Int) {
        println("Running MCP server")
        embeddedServer(CIO, host = "127.0.0.1", port = port) {
            install(SSE)
            install(ContentNegotiation) {
                json(McpJson)
            }
            install(Authentication) {
                bearer("mcp-token") {
                    authenticate { credential ->
                        userAuthenticator.authorizeUser(credential.token)
                    }
                }
            }

            val transports = ConcurrentMap<String, StreamableHttpServerTransport>()

            routing {
                authenticate("mcp-token") {
                    route("/mcp") {
                        sse {
                            val transport = findTransport(call, transports) ?: return@sse
                            transport.handleRequest(this, call)
                        }
                        post {
                            val transport = getOrCreateTransport(call, transports) ?: return@post
                            transport.handleRequest(null, call)
                        }
                        delete {
                            val transport = findTransport(call, transports) ?: return@delete
                            transport.handleRequest(null, call)
                        }
                    }
                }
            }
        }.start(wait = true)
    }

    private fun createServerForUser(user: User): Server {
        val server = Server(
            serverInfo = Implementation(
                name = "3dav-mcp-server",
                version = "0.0.1"
            ),
            options = ServerOptions(
                capabilities = ServerCapabilities(
                    resources = ServerCapabilities.Resources(),
                    tools = ServerCapabilities.Tools(),
                ),
            )
        )
        tools.forEach { tool ->
            server.addTool(tool.tool()) { request ->
                tool.handler(this, user, request)
            }
        }
        return server
    }

    private suspend fun findTransport(
        call: ApplicationCall,
        transports: ConcurrentMap<String, StreamableHttpServerTransport>,
    ): StreamableHttpServerTransport? {
        val sessionId = call.request.header(MCP_SESSION_ID_HEADER)
        if (sessionId.isNullOrEmpty()) {
            call.respond(HttpStatusCode.BadRequest, "No valid session ID provided")
            return null
        }
        val transport = transports[sessionId]
        if (transport == null) {
            call.respond(HttpStatusCode.NotFound, "Session not found")
        }
        return transport
    }

    private suspend fun getOrCreateTransport(
        call: ApplicationCall,
        transports: ConcurrentMap<String, StreamableHttpServerTransport>,
    ): StreamableHttpServerTransport? {
        val sessionId = call.request.header(MCP_SESSION_ID_HEADER)
        if (sessionId != null) {
            return transports[sessionId] ?: run {
                call.respond(HttpStatusCode.NotFound, "Session not found")
                null
            }
        }

        val user = call.principal<User>()!!
        logger.fine("Authenticated user: ${user.email}")

        val transport = StreamableHttpServerTransport(
            StreamableHttpServerTransport.Configuration(enableJsonResponse = true)
        )
        transport.setOnSessionInitialized { initializedSessionId ->
            transports[initializedSessionId] = transport
        }
        transport.setOnSessionClosed { closedSessionId ->
            transports.remove(closedSessionId)
        }

        val server = createServerForUser(user)
        server.onClose {
            transport.sessionId?.let { transports.remove(it) }
        }
        server.createSession(transport)

        return transport
    }


    companion object {

        private const val MCP_SESSION_ID_HEADER = "mcp-session-id"

    }

}
