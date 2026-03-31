package at.bitfire.labs.davmcp

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import at.bitfire.labs.davmcp.db.Database
import java.util.*

class ServerConfig {

    fun databaseDriver(): SqlDriver =
        JdbcSqliteDriver("jdbc:sqlite:data/users.db", Properties().apply {
            put("foreign_keys", "true")
        }, Database.Schema)

}
