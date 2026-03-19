package at.bitfire.labs.davmcp.tools

import at.bitfire.labs.davmcp.DavConfig
import io.ktor.client.*
import io.ktor.client.plugins.auth.*
import io.ktor.client.plugins.auth.providers.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.modelcontextprotocol.kotlin.sdk.server.ClientConnection
import io.modelcontextprotocol.kotlin.sdk.types.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.put
import net.fortuna.ical4j.data.CalendarOutputter
import net.fortuna.ical4j.model.Calendar
import net.fortuna.ical4j.model.ComponentContainer
import net.fortuna.ical4j.model.Property
import net.fortuna.ical4j.model.PropertyContainer
import net.fortuna.ical4j.model.component.CalendarComponent
import net.fortuna.ical4j.model.component.VEvent
import net.fortuna.ical4j.model.property.*
import net.fortuna.ical4j.model.property.immutable.ImmutableCalScale
import net.fortuna.ical4j.model.property.immutable.ImmutableVersion
import okio.IOException
import java.io.ByteArrayOutputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

class AddEventTool(private val config: DavConfig) {

    @Serializable
    private data class EventRequest(
        val title: String,
        val startDateTime: String,
        val endDateTime: String,
        val description: String? = null,
        val location: String? = null
    )

    private data class EventData(
        val summary: String,
        val startDate: LocalDateTime,
        val endDate: LocalDateTime,
        val description: String?,
        val location: String?
    )

    fun tool() = Tool(
        name = "events.add",
        description = "Adds an event to the user's calendar",
        inputSchema = ToolSchema(
            properties = buildJsonObject {
                put("title", buildJsonObject {
                    put("type", "string")
                    put("description", "Title of the event")
                })
                put("startDateTime", buildJsonObject {
                    put("type", "string")
                    put("format", "date-time")
                    put("description", "Start date date/time (inclusive) of the event (\"YYYY-MM-DDThh:mm:ss\")")
                })
                put("endDateTime", buildJsonObject {
                    put("type", "string")
                    put("format", "date-time")
                    put("description", "End date or date/time (exclusive) of the event (\"YYYY-MM-DDThh:mm:ss\")")
                })
                put("description", buildJsonObject {
                    put("type", "string")
                    put("description", "Optional description of the event (plain text)")
                })
                put("location", buildJsonObject {
                    put("type", "string")
                    put("description", "Optional location of the event")
                })
            },
            required = listOf("title", "startDateTime", "endDateTime")
        )
    )
    
    suspend fun handler(connection: ClientConnection, request: CallToolRequest): CallToolResult {
        try {
            val json = Json {
                ignoreUnknownKeys = true
                isLenient = true
                explicitNulls = false
            }
            
            val eventRequest = json.decodeFromJsonElement<EventRequest>(
                request.arguments ?: throw IllegalArgumentException("Request arguments are required")
            )

            val dateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
            val eventData = EventData(
                summary = eventRequest.title,
                startDate = LocalDateTime.parse(eventRequest.startDateTime, dateTimeFormatter),
                endDate = LocalDateTime.parse(eventRequest.endDateTime, dateTimeFormatter),
                description = eventRequest.description,
                location = eventRequest.location
            )

            val uid = UUID.randomUUID().toString()
            val iCal = generateICal(uid, eventData)
            uploadToCollection("$uid.ics", iCal)

            return CallToolResult(content = listOf(TextContent("Success: $iCal")))
        } catch (e: Exception) {
            e.printStackTrace(System.err)
            return CallToolResult(
                content = listOf(TextContent(e.message ?: e.javaClass.name)),
                isError = true
            )
        }
    }


    private fun generateICal(uid: String, eventData: EventData): ByteArray {
        val calendar = Calendar()
        calendar += ImmutableVersion.VERSION_2_0
        calendar += ImmutableCalScale.GREGORIAN
        calendar += ProdId("-//Bitfire Labs//DAV-MCP//EN")

        val event = VEvent()
        event += Uid(uid)
        event += DtStart(eventData.startDate)
        event += DtEnd(eventData.endDate)
        event += Summary(eventData.summary)
        event += Location(eventData.location)

        if (eventData.description != null) {
            event += Description(eventData.description)
        }

        calendar.add<ComponentContainer<CalendarComponent>>(event)

        // write to String and return it
        val baos = ByteArrayOutputStream()
        CalendarOutputter(false).output(calendar, baos)
        return baos.toByteArray()
    }

    private operator fun PropertyContainer.plusAssign(property: Property) {
        add<PropertyContainer>(property)
    }


    private suspend fun uploadToCollection(memberName: String, iCalendar: ByteArray) {
        val authUsername = config.username
        val authPassword = config.password
        HttpClient {
            install(Auth) {
                basic {
                    sendWithoutRequest { true }
                    credentials {
                        BasicAuthCredentials(username = authUsername, password = authPassword)
                    }
                }
            }
            install(Logging) {
                logger = Logger.SIMPLE
            }
        }.use { client ->
            val collectionUrl = Url(config.calendarUrl)
            val url = URLBuilder(collectionUrl).appendPathSegments(memberName).build()
            System.err.println("Uploading $iCalendar to $url")

            val response = client.put {
                url(url)
                setBody(iCalendar)
            }
            if (!response.status.isSuccess())
                throw IOException("HTTP ${response.status.value} ${response.status.description}")

            /*val calendar = DavCalendar(client, url)
            calendar.put(
                provideBody = { ByteReadChannel(iCalendar) },
                mimeType = ContentType.parse("text/calendar; charset=utf-8")
            ) { response ->
                if (!response.status.isSuccess())
                    throw IOException("HTTP ${response.status.value} ${response.status.description}")
            }*/
        }
    }

}