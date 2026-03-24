package at.bitfire.labs.davmcp

import io.ktor.client.*
import io.ktor.client.plugins.auth.*
import io.ktor.client.plugins.auth.providers.*
import io.ktor.client.plugins.logging.*
import javax.inject.Inject

class HttpClientBuilder @Inject constructor(
    private val config: DavConfig
) {

    fun buildFromConfig(): HttpClient {
        return HttpClient {
            install(Auth) {
                basic {
                    sendWithoutRequest { true }
                    credentials {
                        BasicAuthCredentials(username = config.username, password = config.password)
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