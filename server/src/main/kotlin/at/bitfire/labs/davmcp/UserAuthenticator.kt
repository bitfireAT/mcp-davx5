package at.bitfire.labs.davmcp

import at.bitfire.labs.davmcp.db.Database
import at.bitfire.labs.davmcp.db.User
import javax.inject.Inject

class UserAuthenticator @Inject constructor(
    private val database: Database
) {

    fun authorizeUser(token: String): User {
        val userId = database.accessTokenQueries.getUserIdByToken(token).executeAsOneOrNull()
            ?: throw UnauthorizedUserException(token)
        return database.userQueries.getById(userId).executeAsOne()
    }


    class UnauthorizedUserException(token: String) : Exception("No user found for token: $token")

}