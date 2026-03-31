package at.bitfire.labs.davmcp

import at.bitfire.labs.davmcp.db.Service
import io.ktor.client.*
import io.ktor.client.plugins.auth.*
import io.ktor.client.plugins.auth.providers.*
import io.ktor.client.plugins.logging.*
import javax.inject.Inject

class HttpClientBuilder @Inject constructor() {

    fun buildFromService(service: Service): HttpClient {
        return HttpClient {
            install(Auth) {
                val username = service.username
                val password = service.password
                if (username != null && password != null)
                    basic {
                        sendWithoutRequest { true }
                        credentials {
                            BasicAuthCredentials(username, password)
                        }
                    }
            }
            install(Logging) {
                level = LogLevel.ALL
                logger = Logger.SIMPLE
            }
        }
    }

}