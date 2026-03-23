package at.bitfire.labs.davmcp

import at.bitfire.labs.davmcp.di.DaggerAppComponent

fun main(args: Array<String>) {
    val port = args.firstOrNull()?.toIntOrNull() ?: 3000
    val mcpServer = DaggerAppComponent.create().mcpServer()
    mcpServer.start(port)
}