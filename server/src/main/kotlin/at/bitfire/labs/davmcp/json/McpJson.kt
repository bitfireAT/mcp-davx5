package at.bitfire.labs.davmcp.json

import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual

val McpJson = Json {
    ignoreUnknownKeys = true
    isLenient = true
    explicitNulls = false

    serializersModule = SerializersModule {
        contextual(InstantSerializer)
    }
}