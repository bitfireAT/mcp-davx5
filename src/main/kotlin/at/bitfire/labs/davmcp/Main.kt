package at.bitfire.labs.davmcp

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import at.bitfire.labs.davmcp.db.Database
import at.bitfire.labs.davmcp.di.DaggerAppComponent
import java.util.Properties

fun main(args: Array<String>) {
    val driver: SqlDriver = JdbcSqliteDriver("jdbc:sqlite:data/users.db", Properties().apply {
        put("foreign_keys", "true")
    }, Database.Schema)

    val port = args.firstOrNull()?.toIntOrNull() ?: 3000
    val mcpServer = DaggerAppComponent.create().mcpServer()
    mcpServer.start(port)
}